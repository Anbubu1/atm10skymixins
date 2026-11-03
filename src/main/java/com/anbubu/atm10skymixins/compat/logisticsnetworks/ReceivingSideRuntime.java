package com.anbubu.atm10skymixins.compat.logisticsnetworks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reflection boundary for LogisticsNetworks. This keeps the mixin project compile-time independent
 * of LogisticsNetworks while still constructing side-specific capability delegates at runtime.
 */
public final class ReceivingSideRuntime {
    private static final String LN = "me.almana.logisticsnetworks.";
    private static final ThreadLocal<String> CURRENT_SIDE = new ThreadLocal<>();
    private static final ThreadLocal<List<?>> CURRENT_CARDS = new ThreadLocal<>();
    private static final Map<Object, Optional<String>> WANT_SIDES =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<Object, Optional<String>> PENDING_SIDES =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<Object, List<?>> LANE_INGREDIENTS =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private record FluidRequirement(FluidStack sample, long amount) {}
    private record ChemicalRequirement(Object sample, long amount) {}

    private ReceivingSideRuntime() {}

    public static Object createSideAwareEndpoint(Object target, Object source, Object caps, Object cache,
            Object originalEndpoint) {
        try {
            Object channel = call(target, "channel");
            Object defaultSide = call(channel, "getIoSide");
            String defaultKey = sideKey(defaultSide);
            Object sourceItems = call(source, "items");
            Object sourceFluids = call(source, "fluids");
            Object sourceChemicals = call(source, "chemicals");

            Object node = call(target, "node");
            int acceptedTypes = (int) callStatic(LN + "upgrade.NodeUpgradeData", "effectiveTypes", node, channel);
            boolean acceptsItems = channelTypeEnabled("ITEM", acceptedTypes);
            boolean acceptsFluids = channelTypeEnabled("FLUID", acceptedTypes);
            boolean acceptsChemicals = channelTypeEnabled("CHEMICAL", acceptedTypes);

            Set<String> itemSides = acceptsItems ? requiredSides("ITEM", defaultKey) : Set.of();
            Set<String> fluidSides = acceptsFluids ? requiredSides("FLUID", defaultKey) : Set.of();
            Set<String> chemicalSides = acceptsChemicals ? requiredSides("CHEMICAL", defaultKey) : Set.of();
            Map<String, Object> itemTargets = new LinkedHashMap<>();
            Map<String, Object> fluidTargets = new LinkedHashMap<>();
            Map<String, Object> chemicalTargets = new LinkedHashMap<>();

            try {
                for (Object side : enumValues(LN + "data.IoSide")) {
                    String key = sideKey(side);
                    if ((sourceItems == null || !itemSides.contains(key))
                            && (sourceFluids == null || !fluidSides.contains(key))
                            && (sourceChemicals == null || !chemicalSides.contains(key))) continue;
                    call(channel, "setIoSide", side);
                    if (sourceItems != null && itemSides.contains(key))
                        itemTargets.put(key, callStatic(LN + "logic.RecipeChannel", "items",
                                target, sourceItems, caps, cache));
                    if (sourceFluids != null && fluidSides.contains(key))
                        fluidTargets.put(key, callStatic(LN + "logic.RecipeChannel", "fluids",
                                target, sourceFluids, caps));
                    if (sourceChemicals != null && chemicalSides.contains(key))
                        chemicalTargets.put(key, callStatic(LN + "logic.RecipeChannel", "chemicals",
                                target, sourceChemicals, caps));
                }
            } finally {
                call(channel, "setIoSide", defaultSide);
            }

            Object items = itemTargetProxy(itemTargets, defaultKey);
            Object fluids = fluidTargetProxy(fluidTargets, defaultKey);
            Object chemicals = chemicalTargetProxy(chemicalTargets, defaultKey);
            Map<String, Object> replacements = new LinkedHashMap<>();
            replacements.put("items", items);
            replacements.put("fluids", fluids);
            replacements.put("chemicals", chemicals);
            Object endpoint = rebuildRecord(originalEndpoint, replacements);

            List<?> cards = CURRENT_CARDS.get();
            if (cards != null) {
                Object candidate = endpoint;

                if (cards.stream().noneMatch(
                        card -> !usableForCard(card, List.of(candidate)).isEmpty())) {
                    replacements.replaceAll((name, ignored) -> null);
                    endpoint = rebuildRecord(originalEndpoint, replacements);
                }
            }

            return endpoint;
        } catch (Throwable error) {
            throw new IllegalStateException("Could not build side-aware LogisticsNetworks recipe endpoint", unwrap(error));
        }
    }

    private static Object itemTargetProxy(Map<String, Object> values, String defaultSide) throws Exception {
        Object prototype = firstNonNull(values);
        if (prototype == null) return null;

        Map<String, Object> handlers = new LinkedHashMap<>();
        Map<String, Object> bulkHandlers = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object value = entry.getValue();
            if (value == null) continue;
            Object handler = call(value, "handler");
            Object bulk = call(value, "bulkHandler");
            if (handler != null) handlers.put(entry.getKey(), handler);
            if (bulk != null) bulkHandlers.put(entry.getKey(), bulk);
        }
        Object handlerProxy = proxy("net.neoforged.neoforge.items.IItemHandler", handlers, defaultSide);
        Object bulkProxy = bulkHandlers.isEmpty() ? null
                : proxy("net.neoforged.neoforge.items.IItemHandler", bulkHandlers, defaultSide);
        Map<String, Object> replacements = new LinkedHashMap<>();
        replacements.put("handler", handlerProxy);
        replacements.put("bulkHandler", bulkProxy);
        return rebuildRecord(prototype, replacements);
    }

    private static Object fluidTargetProxy(Map<String, Object> values, String defaultSide) throws Exception {
        Object prototype = firstNonNull(values);
        if (prototype == null) return null;
        Map<String, Object> handlers = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getValue() != null) {
                Object handler = call(entry.getValue(), "handler");
                if (handler != null) handlers.put(entry.getKey(), handler);
            }
        }
        Map<String, Object> replacements = new LinkedHashMap<>();
        replacements.put("handler", proxy("net.neoforged.neoforge.fluids.capability.IFluidHandler", handlers,
                defaultSide));
        return rebuildRecord(prototype, replacements);
    }

    private static Object chemicalTargetProxy(Map<String, Object> values, String defaultSide) throws Exception {
        Object prototype = firstNonNull(values);
        if (prototype == null) return null;
        Map<String, Object> handlers = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getValue() != null) {
                Object handler = call(entry.getValue(), "handler");
                if (handler != null) handlers.put(entry.getKey(), handler);
            }
        }
        Map<String, Object> replacements = new LinkedHashMap<>();
        replacements.put("handler", proxy("mekanism.api.chemical.IChemicalHandler", handlers, defaultSide));
        return rebuildRecord(prototype, replacements);
    }

    private static Object proxy(String interfaceName, Map<String, Object> handlers, String defaultSide)
            throws ClassNotFoundException {
        Class<?> type = Class.forName(interfaceName);
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                new SideHandler(new LinkedHashMap<>(handlers), defaultSide));
    }

    private static Object rebuildRecord(Object prototype, Map<String, Object> replacements) throws Exception {
        Class<?> type = prototype.getClass();
        RecordComponent[] components = type.getRecordComponents();
        if (components == null) throw new IllegalStateException(type.getName() + " is not a record");
        Class<?>[] parameterTypes = new Class<?>[components.length];
        Object[] arguments = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            parameterTypes[i] = component.getType();
            if (replacements.containsKey(component.getName())) {
                arguments[i] = replacements.get(component.getName());
            } else {
                Method accessor = component.getAccessor();
                accessor.setAccessible(true);
                arguments[i] = accessor.invoke(prototype);
            }
        }
        Constructor<?> constructor = type.getDeclaredConstructor(parameterTypes);
        constructor.setAccessible(true);
        return constructor.newInstance(arguments);
    }

    private static Object firstNonNull(Map<String, Object> values) {
        for (Object value : values.values()) if (value != null) return value;
        return null;
    }

    public static void setForIngredient(Object ingredient) {
        setCurrent(sideOf(ingredient));
    }

    public static void registerWanted(Object lane, List<?> ingredients) {
        try {
            List<?> wanted = (List<?>) field(lane, "wanted");
            int length = Math.min(wanted.size(), ingredients.size());
            for (int i = 0; i < length; i++) WANT_SIDES.put(wanted.get(i), Optional.ofNullable(sideOf(ingredients.get(i))));
            LANE_INGREDIENTS.put(lane, List.copyOf(ingredients));
        } catch (Throwable error) {
            throw new IllegalStateException("Could not associate LogisticsNetworks recipe resources with sides", error);
        }
    }

    public static void setForWanted(Object wanted) {
        Optional<String> side = WANT_SIDES.get(wanted);
        setCurrent(side == null ? null : side.orElse(null));
    }

    public static void rememberPending(Object movedResource) {
        PENDING_SIDES.put(movedResource, Optional.ofNullable(CURRENT_SIDE.get()));
    }

    public static void setForPending(Object movedResource) {
        Optional<String> side = PENDING_SIDES.get(movedResource);
        setCurrent(side == null ? null : side.orElse(null));
    }

    public static void clearLane(Object lane) {
        try {
            Object wanted = field(lane, "wanted");
            if (wanted instanceof Iterable<?> values) {
                for (Object value : values) WANT_SIDES.remove(value);
            }
            Object pending = field(lane, "pending");
            if (pending instanceof Iterable<?> values) {
                for (Object value : values) {
                    Object resource = value;
                    try { resource = call(value, "stack"); } catch (Throwable ignored) {}
                    PENDING_SIDES.remove(resource);
                }
            }
            LANE_INGREDIENTS.remove(lane);
            CURRENT_SIDE.remove();
        } catch (Throwable error) {
            throw new IllegalStateException("Could not clear LogisticsNetworks receiving-side state", error);
        }
    }

    public static void clearPending(Object lane) {
        try {
            Object pending = field(lane, "pending");
            if (pending instanceof Iterable<?> values) {
                for (Object value : values) {
                    Object resource = value;
                    try { resource = call(value, "stack"); } catch (Throwable ignored) {}
                    PENDING_SIDES.remove(resource);
                }
            }
            CURRENT_SIDE.remove();
        } catch (Throwable error) {
            throw new IllegalStateException("Could not clear LogisticsNetworks pending receiving-side state", error);
        }
    }

    public static boolean hasHandler(Object handlerProxy, String side) {
        if (handlerProxy == null || side == null || !Proxy.isProxyClass(handlerProxy.getClass())) return false;
        InvocationHandler handler = Proxy.getInvocationHandler(handlerProxy);
        return handler instanceof SideHandler sideHandler && sideHandler.has(side);
    }

    public static String defaultSide(Object handlerProxy) {
        if (handlerProxy == null || !Proxy.isProxyClass(handlerProxy.getClass())) return null;
        InvocationHandler handler = Proxy.getInvocationHandler(handlerProxy);
        return handler instanceof SideHandler sideHandler ? sideHandler.defaultSide : null;
    }

    public static List<Object> delegates(Object handlerProxy) {
        if (handlerProxy == null || !Proxy.isProxyClass(handlerProxy.getClass())) return List.of();
        InvocationHandler handler = Proxy.getInvocationHandler(handlerProxy);
        return handler instanceof SideHandler sideHandler ? sideHandler.delegates() : List.of();
    }

    public static List<Integer> usableForCard(Object card, List<?> endpoints) {
        try {
            List<Integer> result = new ArrayList<>();
            List<?> ingredients = (List<?>) call(card, "ingredients");

            for (int endpointIndex = 0; endpointIndex < endpoints.size(); endpointIndex++) {
                Object endpoint = endpoints.get(endpointIndex);
                boolean usable = true;

                for (Object ingredient : ingredients) {
                    String kind = String.valueOf(call(ingredient, "kind"));
                    String accessor = switch (kind) {
                        case "ITEM" -> "items";
                        case "FLUID" -> "fluids";
                        case "CHEMICAL" -> "chemicals";
                        default -> throw new IllegalStateException(
                                "Unknown LogisticsNetworks recipe kind: " + kind);
                    };

                    Object target = call(endpoint, accessor);
                    if (target == null) {
                        usable = false;
                        break;
                    }

                    Object handler = call(target, "handler");
                    String side = sideOf(ingredient);
                    if (side == null) side = defaultSide(handler);

                    if (!hasHandler(handler, side)) {
                        usable = false;
                        break;
                    }
                }

                if (usable) result.add(endpointIndex);
            }

            return result;
        } catch (Throwable error) {
            throw new IllegalStateException(
                    "Could not determine usable LogisticsNetworks recipe endpoints",
                    unwrap(error));
        }
    }

    public static boolean holdsOnAnySide(Object endpoint, List<?> blockers) {
        try {
            for (String type : List.of("items", "fluids", "chemicals")) {
                Object target = call(endpoint, type);
                if (target == null) continue;
                Object handler = call(target, "handler");
                String lane = switch (type) {
                    case "items" -> "logic.ItemLane";
                    case "fluids" -> "logic.FluidLane";
                    default -> "integration.mekanism.ChemicalLane";
                };
                for (Object delegate : delegates(handler)) {
                    if (Boolean.TRUE.equals(callStatic(LN + lane, "holds", delegate, blockers))) return true;
                }
            }
            return false;
        } catch (Throwable error) {
            throw new IllegalStateException("Could not evaluate LogisticsNetworks recipe blockers by receiving side", unwrap(error));
        }
    }

    public static int fluidAvailable(Object lane, int cap) {
        try {
            IFluidHandler source = (IFluidHandler) field(lane, "source");
            List<?> wanted = (List<?>) field(lane, "wanted");
            TagKey<Fluid> blacklist = (TagKey<Fluid>) staticField(
                    LN + "registration.ModTags", "RESOURCE_BLACKLIST_FLUIDS");
            Map<String, FluidRequirement> requirements = new LinkedHashMap<>();
            for (Object value : wanted) {
                FluidStack stack = (FluidStack) value;
                if (stack.isEmpty() || stack.is(blacklist)) return 0;
                String id = BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString();
                FluidRequirement previous = requirements.get(id);
                requirements.put(id, new FluidRequirement(stack,
                        stack.getAmount() + (previous == null ? 0 : previous.amount())));
            }
            int sets = cap;
            for (FluidRequirement requirement : requirements.values()) {
                int amount = scaledFluidAmount(requirement.amount(), sets);
                FluidStack request = requirement.sample().copyWithAmount(amount);
                int available = source.drain(request, IFluidHandler.FluidAction.SIMULATE).getAmount();
                sets = Math.min(sets, (int) (available / requirement.amount()));
                if (sets <= 0) return 0;
            }
            return sets;
        } catch (Throwable error) {
            throw new IllegalStateException("Could not aggregate LogisticsNetworks fluid requirements", unwrap(error));
        }
    }

    public static int fluidFit(Object lane, int targetIndex, int sets) {
        try {
            int fit = fluidAvailable(lane, sets);
            if (fit <= 0) return 0;
            List<?> ingredients = LANE_INGREDIENTS.get(lane);
            List<?> wanted = (List<?>) field(lane, "wanted");
            List<?> endpoints = (List<?>) field(lane, "endpoints");
            Object endpoint = endpoints.get(targetIndex);
            Object provider = field(lane, "provider");
            Object cache = field(lane, "cache");
            IdentityHashMap<Object, Map<String, FluidRequirement>> byHandler = new IdentityHashMap<>();
            for (int i = 0; i < wanted.size(); i++) {
                FluidStack want = (FluidStack) wanted.get(i);
                Object target = call(endpoint, "fluids");
                if (target == null) return 0;
                Object handlerProxy = call(target, "handler");
                String side = sideOf(ingredients.get(i));
                if (side == null) side = defaultSide(handlerProxy);
                Object receiver = delegateFor(handlerProxy, side);
                if (receiver == null || !Boolean.TRUE.equals(callStatic(LN + "logic.FilterLogic", "matchesFluid",
                        call(target, "filters"), call(target, "mode"), want, provider, cache))) return 0;
                String id = BuiltInRegistries.FLUID.getKey(want.getFluid()).toString();
                Map<String, FluidRequirement> byFluid = byHandler.computeIfAbsent(receiver,
                        ignored -> new LinkedHashMap<>());
                FluidRequirement previous = byFluid.get(id);
                byFluid.put(id, new FluidRequirement(want,
                        want.getAmount() + (previous == null ? 0 : previous.amount())));
            }
            for (Map.Entry<Object, Map<String, FluidRequirement>> handler : byHandler.entrySet()) {
                for (FluidRequirement requirement : handler.getValue().values()) {
                    int amount = scaledFluidAmount(requirement.amount(), fit);
                    FluidStack request = requirement.sample().copyWithAmount(amount);
                    int accepted = (int) call(handler.getKey(), "fill", request, IFluidHandler.FluidAction.SIMULATE);
                    fit = Math.min(fit, (int) (accepted / requirement.amount()));
                    if (fit <= 0) return 0;
                }
            }
            return fit;
        } catch (Throwable error) {
            throw new IllegalStateException("Could not calculate LogisticsNetworks fluid fit by receiving side", unwrap(error));
        }
    }

    public static int chemicalAvailable(Object lane, int cap) {
        try {
            Object source = field(lane, "source");
            List<?> wanted = (List<?>) field(lane, "wanted");
            Object blacklist = staticField(LN + "integration.mekanism.ChemicalTransferHelper",
                    "RESOURCE_BLACKLIST_CHEMICALS");
            Map<String, ChemicalRequirement> requirements = new LinkedHashMap<>();
            for (Object stack : wanted) {
                if (Boolean.TRUE.equals(call(stack, "isEmpty")) || Boolean.TRUE.equals(call(stack, "is", blacklist)))
                    return 0;
                String id = (String) callStatic(LN + "integration.mekanism.ChemicalTransferHelper", "getChemicalId", stack);
                if (id == null) return 0;
                ChemicalRequirement previous = requirements.get(id);
                long amount = (long) call(stack, "getAmount");
                requirements.put(id, new ChemicalRequirement(stack, amount + (previous == null ? 0 : previous.amount())));
            }
            Object simulate = Class.forName("mekanism.api.Action").getField("SIMULATE").get(null);
            long sets = cap;
            for (ChemicalRequirement requirement : requirements.values()) {
                Object request = call(requirement.sample(), "copyWithAmount", requirement.amount() * sets);
                Object extracted = call(source, "extractChemical", request, simulate);
                long available = (long) call(extracted, "getAmount");
                sets = Math.min(sets, available / requirement.amount());
                if (sets <= 0) return 0;
            }
            return (int) sets;
        } catch (Throwable error) {
            throw new IllegalStateException("Could not aggregate LogisticsNetworks chemical requirements", unwrap(error));
        }
    }

    public static int chemicalFit(Object lane, int targetIndex, int sets) {
        try {
            int fit = chemicalAvailable(lane, sets);
            if (fit <= 0) return 0;
            List<?> ingredients = LANE_INGREDIENTS.get(lane);
            List<?> wanted = (List<?>) field(lane, "wanted");
            List<?> endpoints = (List<?>) field(lane, "endpoints");
            Object endpoint = endpoints.get(targetIndex);
            Object cache = field(lane, "cache");
            IdentityHashMap<Object, Map<String, ChemicalRequirement>> byHandler = new IdentityHashMap<>();
            for (int i = 0; i < wanted.size(); i++) {
                Object want = wanted.get(i);
                Object target = call(endpoint, "chemicals");
                if (target == null) return 0;
                Object handlerProxy = call(target, "handler");
                String side = sideOf(ingredients.get(i));
                if (side == null) side = defaultSide(handlerProxy);
                Object receiver = delegateFor(handlerProxy, side);
                String id = (String) callStatic(LN + "integration.mekanism.ChemicalTransferHelper", "getChemicalId", want);
                if (receiver == null || id == null || !Boolean.TRUE.equals(callStatic(LN + "logic.FilterLogic",
                        "matchesChemical", call(target, "filters"), call(target, "mode"), id, cache))) return 0;
                Map<String, ChemicalRequirement> byChemical = byHandler.computeIfAbsent(receiver,
                        ignored -> new LinkedHashMap<>());
                ChemicalRequirement previous = byChemical.get(id);
                long amount = (long) call(want, "getAmount");
                byChemical.put(id, new ChemicalRequirement(want, amount + (previous == null ? 0 : previous.amount())));
            }
            Object simulate = Class.forName("mekanism.api.Action").getField("SIMULATE").get(null);
            for (Map.Entry<Object, Map<String, ChemicalRequirement>> handler : byHandler.entrySet()) {
                for (ChemicalRequirement requirement : handler.getValue().values()) {
                    Object request = call(requirement.sample(), "copyWithAmount", requirement.amount() * fit);
                    Object rest = call(handler.getKey(), "insertChemical", request, simulate);
                    long accepted = (long) call(request, "getAmount") - (long) call(rest, "getAmount");
                    fit = Math.min(fit, (int) (accepted / requirement.amount()));
                    if (fit <= 0) return 0;
                }
            }
            return fit;
        } catch (Throwable error) {
            throw new IllegalStateException("Could not calculate LogisticsNetworks chemical fit by receiving side", unwrap(error));
        }
    }

    private static int scaledFluidAmount(long amount, int sets) {
        return (int) Math.min(Integer.MAX_VALUE, amount * (long) sets);
    }

    private static Object delegateFor(Object handlerProxy, String side) {
        if (handlerProxy == null || side == null || !Proxy.isProxyClass(handlerProxy.getClass())) return null;
        InvocationHandler handler = Proxy.getInvocationHandler(handlerProxy);
        return handler instanceof SideHandler sideHandler ? sideHandler.delegate(side) : null;
    }

    public static Object delegateForCurrent(Object handlerProxy) {
        if (handlerProxy == null) return null;
        if (!Proxy.isProxyClass(handlerProxy.getClass())) return handlerProxy;
        InvocationHandler handler = Proxy.getInvocationHandler(handlerProxy);
        if (!(handler instanceof SideHandler sideHandler)) return handlerProxy;
        String side = CURRENT_SIDE.get();
        if (side == null) side = sideHandler.defaultSide;
        return sideHandler.delegate(side);
    }

    private static Object staticField(String className, String name) throws Exception {
        Class<?> type = Class.forName(className);
        var field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    public static void setCards(List<?> cards) {
        CURRENT_CARDS.set(cards);
    }

    public static void clearCards() {
        CURRENT_CARDS.remove();
    }

    private static Set<String> requiredSides(String kind, String defaultSide) {
        List<?> cards = CURRENT_CARDS.get();
        if (cards == null) {
            Set<String> all = new LinkedHashSet<>();
            try {
                for (Object side : enumValues(LN + "data.IoSide")) all.add(sideKey(side));
            } catch (ClassNotFoundException error) {
                throw new IllegalStateException("Could not enumerate LogisticsNetworks IO sides", error);
            }
            return all;
        }
        Set<String> result = new LinkedHashSet<>();
        for (Object card : cards) {
            try {
                for (Object ingredient : (List<?>) call(card, "ingredients")) {
                    if (!kind.equals(String.valueOf(call(ingredient, "kind")))) continue;
                    String side = sideOf(ingredient);
                    result.add(side == null ? defaultSide : side);
                }
            } catch (Throwable error) {
                throw new IllegalStateException("Could not read recipe-card receiving sides", unwrap(error));
            }
        }
        return result;
    }

    public static void setCurrent(String side) {
        if (side == null || side.isBlank()) CURRENT_SIDE.remove();
        else CURRENT_SIDE.set(side.toLowerCase(Locale.ROOT));
    }

    public static String currentSide() {
        return CURRENT_SIDE.get();
    }

    public static String sideOf(Object ingredient) {
        if (ingredient instanceof IngredientSideAccess access) return access.atm10skymixins$getReceivingSide();
        return null;
    }

    public static Object call(Object owner, String name, Object... args) throws Exception {
        if (owner == null) return null;
        Method method = findMethod(owner.getClass(), name, args, false);
        method.setAccessible(true);
        try {
            return method.invoke(owner, args);
        } catch (InvocationTargetException error) {
            throw rethrow(error.getCause());
        }
    }

    public static Object callStatic(String className, String name, Object... args) throws Exception {
        Class<?> type = Class.forName(className);
        Method method = findMethod(type, name, args, true);
        method.setAccessible(true);
        try {
            return method.invoke(null, args);
        } catch (InvocationTargetException error) {
            throw rethrow(error.getCause());
        }
    }

    public static Object field(Object owner, String name) throws Exception {
        if (owner == null) return null;
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            try {
                var field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(owner.getClass().getName() + "." + name);
    }

    private static Method findMethod(Class<?> owner, String name, Object[] args, boolean requireStatic)
            throws NoSuchMethodException {
        for (Class<?> type = owner; type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != args.length
                        || requireStatic != java.lang.reflect.Modifier.isStatic(method.getModifiers())) continue;
                Class<?>[] parameters = method.getParameterTypes();
                boolean matches = true;
                for (int i = 0; i < parameters.length; i++) {
                    if (!compatible(parameters[i], args[i])) {
                        matches = false;
                        break;
                    }
                }
                if (matches) return method;
            }
        }
        for (Method method : owner.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != args.length
                    || requireStatic != java.lang.reflect.Modifier.isStatic(method.getModifiers())) continue;
            Class<?>[] parameters = method.getParameterTypes();
            boolean matches = true;
            for (int i = 0; i < parameters.length; i++) {
                if (!compatible(parameters[i], args[i])) {
                    matches = false;
                    break;
                }
            }
            if (matches) return method;
        }
        throw new NoSuchMethodException(owner.getName() + "." + name + "/" + args.length);
    }

    private static boolean compatible(Class<?> parameter, Object argument) {
        if (argument == null) return !parameter.isPrimitive();
        if (!parameter.isPrimitive()) return parameter.isAssignableFrom(argument.getClass());
        return (parameter == int.class && argument instanceof Integer)
                || (parameter == long.class && argument instanceof Long)
                || (parameter == boolean.class && argument instanceof Boolean)
                || (parameter == byte.class && argument instanceof Byte)
                || (parameter == short.class && argument instanceof Short)
                || (parameter == float.class && argument instanceof Float)
                || (parameter == double.class && argument instanceof Double)
                || (parameter == char.class && argument instanceof Character);
    }

    private static Object[] enumValues(String className) throws ClassNotFoundException {
        Object[] values = Class.forName(className).getEnumConstants();
        if (values == null) throw new IllegalStateException(className + " is not an enum");
        return values;
    }

    private static boolean channelTypeEnabled(String name, int mask) throws Exception {
        Class<?> type = Class.forName(LN + "data.ChannelType");
        Object channelType = null;
        for (Object value : type.getEnumConstants()) {
            if (value instanceof Enum<?> enumValue && enumValue.name().equals(name)) {
                channelType = value;
                break;
            }
        }
        return channelType != null && Boolean.TRUE.equals(call(channelType, "in", mask));
    }

    private static String sideKey(Object side) {
        return String.valueOf(side).toLowerCase(Locale.ROOT);
    }

    private static Throwable unwrap(Throwable error) {
        return error instanceof InvocationTargetException invocation && invocation.getCause() != null
                ? invocation.getCause() : error;
    }

    private static Exception rethrow(Throwable error) throws Exception {
        if (error instanceof Exception exception) return exception;
        if (error instanceof Error fatal) throw fatal;
        return new RuntimeException(error);
    }

    private static final class SideHandler implements InvocationHandler {
        private final Map<String, Object> handlers;
        private final String defaultSide;

        private SideHandler(Map<String, Object> handlers, String defaultSide) {
            this.handlers = handlers;
            this.defaultSide = defaultSide;
        }

        boolean has(String side) {
            return side != null && handlers.get(side) != null;
        }

        Object delegate(String side) {
            return side == null ? null : handlers.get(side);
        }

        List<Object> delegates() {
            List<Object> result = new ArrayList<>();
            for (Object value : handlers.values()) {
                if (value != null && result.stream().noneMatch(existing -> existing == value)) result.add(value);
            }
            return result;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> "ReceivingSideProxy[" + handlers.keySet() + "]";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == (args == null ? null : args[0]);
                    default -> null;
                };
            }
            String selected = CURRENT_SIDE.get();
            if (selected == null) selected = defaultSide;
            Object delegate = selected == null ? null : handlers.get(selected);
            if (delegate == null) return emptyResult(method, args);
            try {
                method.setAccessible(true);
                return method.invoke(delegate, args);
            } catch (InvocationTargetException error) {
                throw error.getCause();
            }
        }

        private static Object emptyResult(Method method, Object[] args) throws Exception {
            Class<?> result = method.getReturnType();
            if (result == void.class) return null;
            if (result == boolean.class || result == Boolean.class) return false;
            if (result == int.class || result == Integer.class) return 0;
            if (result == long.class || result == Long.class) return 0L;
            if (result == float.class || result == Float.class) return 0F;
            if (result == double.class || result == Double.class) return 0D;
            if (method.getName().equals("insertItem") && args != null && args.length > 1) return args[1];
            if (method.getName().equals("insertChemical") && args != null && args.length > 0) return args[0];
            if (result == ItemStack.class) return ItemStack.EMPTY;
            if (result == FluidStack.class) return FluidStack.EMPTY;
            if (result.getName().equals("mekanism.api.chemical.ChemicalStack")) {
                return result.getField("EMPTY").get(null);
            }
            return null;
        }
    }
}
