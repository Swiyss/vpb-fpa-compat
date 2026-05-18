package com.joao2.vpbfpa.trace;

import com.joao2.vpbfpa.VpbFpaCompatClient;
import com.joao2.vpbfpa.config.CompatConfig;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ModelPartDumper {
    private static final ConcurrentMap<MethodKey, Optional<Method>> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<FieldKey, Optional<Field>> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> REFLECTION_FAILURES = ConcurrentHashMap.newKeySet();
    private static boolean dumped = false;

    private ModelPartDumper() {
    }

    public static void dumpOnce(PlayerEntityModel model, String stage) {
        CompatConfig config = VpbFpaCompatClient.config();
        if (dumped || config == null || !config.dumpModelParts || model == null) {
            return;
        }

        dumped = true;
        TraversalResult result = collectSafeParts(model, config);
        VpbFpaCompatClient.LOGGER.info(
                "[VPB-FPA ModelProbe] stage={} modelClass={} rootClass={} partCount={} discoveredParts={} maxDepth={} maxParts={} rightArmClass={} leftArmClass={} rightSleeveClass={} leftSleeveClass={}",
                stage,
                model.getClass().getName(),
                className(model.getRootPart()),
                model.getParts().size(),
                result.parts().size(),
                config.normalizedDumpModelPartsMaxDepth(),
                config.normalizedDumpModelPartsMaxParts(),
                className(model.rightArm),
                className(model.leftArm),
                className(model.rightSleeve),
                className(model.leftSleeve)
        );

        for (Map.Entry<String, ModelPart> entry : result.parts().entrySet()) {
            String path = entry.getKey();
            ModelPart part = entry.getValue();
            VpbFpaCompatClient.LOGGER.info(
                    "[VPB-FPA ModelProbe] emfPart name={} path={} class={} visible={} hidden={} empty={} rot={} children={}",
                    nameOf(part, path),
                    path,
                    className(part),
                    part.visible,
                    part.hidden,
                    part.isEmpty(),
                    rot(part),
                    childCount(part)
            );

            String reason = candidateReason(path, part);
            if (!reason.isEmpty()) {
                VpbFpaCompatClient.LOGGER.info(
                        "[VPB-FPA ModelProbe] armCandidate name={} path={} class={} reason={}",
                        nameOf(part, path),
                        path,
                        className(part),
                        reason
                );
            }
        }

        for (String reason : result.truncations()) {
            VpbFpaCompatClient.LOGGER.info(
                    "[VPB-FPA ModelProbe] traversal truncated reason={} maxDepth={} maxParts={} discoveredParts={}",
                    reason,
                    config.normalizedDumpModelPartsMaxDepth(),
                    config.normalizedDumpModelPartsMaxParts(),
                    result.parts().size()
            );
        }
    }

    public static List<NamedPart> findArmLikeParts(PlayerEntityModel model, String filter) {
        CompatConfig config = VpbFpaCompatClient.config();
        if (config == null || model == null) {
            return List.of();
        }

        TraversalResult result = collectSafeParts(model, config);
        String normalizedFilter = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        List<NamedPart> candidates = new ArrayList<>();
        for (Map.Entry<String, ModelPart> entry : result.parts().entrySet()) {
            String path = entry.getKey();
            String lower = path.toLowerCase(Locale.ROOT);
            if (!normalizedFilter.isEmpty() && !lower.contains(normalizedFilter)) {
                continue;
            }
            if (!candidateReason(path, entry.getValue()).isEmpty()) {
                candidates.add(new NamedPart(path, entry.getValue()));
            }
        }
        return candidates;
    }

    public static PartMetadata metadata(NamedPart namedPart) {
        if (namedPart == null || namedPart.part() == null) {
            return new PartMetadata("unknown", "unknown", "unknown", "unknown");
        }

        ModelPart part = namedPart.part();
        Object partToBeAttached = fieldValue(part, "partToBeAttached");
        Object attach = fieldValue(part, "attach");
        Object attachments = fieldValue(part, "attachments");
        Object vanillaChildren = fieldValue(part, "vanillaChildren");
        return new PartMetadata(
                stringValue(partToBeAttached),
                stringValue(attach),
                countValue(attachments),
                countValue(vanillaChildren)
        );
    }

    private static TraversalResult collectSafeParts(PlayerEntityModel model, CompatConfig config) {
        Traversal traversal = new Traversal(
                config.normalizedDumpModelPartsMaxDepth(),
                config.normalizedDumpModelPartsMaxParts()
        );

        collectPartTree(model.getRootPart(), "root", 0, traversal);
        collectPartTree(model.rightArm, "rightArm", 0, traversal);
        collectPartTree(model.leftArm, "leftArm", 0, traversal);
        collectPartTree(model.rightSleeve, "rightSleeve", 0, traversal);
        collectPartTree(model.leftSleeve, "leftSleeve", 0, traversal);
        collectRootVanillaParts(model.getRootPart(), traversal);

        return new TraversalResult(
                new LinkedHashMap<>(traversal.parts),
                new ArrayList<>(traversal.truncations)
        );
    }

    private static void collectPartTree(ModelPart part, String path, int depth, Traversal traversal) {
        if (!traversal.add(part, path, depth)) {
            return;
        }
        if (depth >= traversal.maxDepth) {
            if (hasPotentialChildren(part)) {
                traversal.truncate("maxDepth");
            }
            return;
        }

        for (Map.Entry<String, ModelPart> entry : childrenOrEmpty(part).entrySet()) {
            collectPartTree(entry.getValue(), path + "/" + entry.getKey(), depth + 1, traversal);
            if (traversal.isFull()) {
                return;
            }
        }

        int index = 0;
        for (ModelPart child : customChildren(part)) {
            String childName = nameOf(child, "custom" + index);
            collectPartTree(child, path + "/" + childName, depth + 1, traversal);
            if (traversal.isFull()) {
                return;
            }
            index++;
        }
    }

    private static void collectRootVanillaParts(ModelPart root, Traversal traversal) {
        int index = 0;
        for (Object object : allVanillaParts(root)) {
            if (traversal.isFull()) {
                return;
            }
            if (object instanceof ModelPart modelPart) {
                collectPartTree(modelPart, "emf/" + nameOf(modelPart, "vanilla" + index), 1, traversal);
                index++;
            }
        }
    }

    private static String className(Object object) {
        return object == null ? "null" : object.getClass().getName();
    }

    private static String rot(ModelPart part) {
        return String.format(Locale.ROOT, "(%.3f,%.3f,%.3f)", part.pitch, part.yaw, part.roll);
    }

    private static String nameOf(ModelPart part, String fallback) {
        Object value = fieldValue(part, "name");
        if (value instanceof String name && !name.isBlank()) {
            return name;
        }

        Object customId = fieldValue(part, "id");
        if (customId instanceof String id && !id.isBlank()) {
            return id;
        }

        return fallback;
    }

    private static String candidateReason(String path, ModelPart part) {
        if (Boolean.TRUE.equals(fieldValue(part, "isPlayerArm"))) {
            return "arm";
        }

        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.contains("sleeve")) {
            return "sleeve";
        }
        if (lower.contains("hand")) {
            return "hand";
        }
        if (lower.contains("arm")) {
            return "arm";
        }
        if ((lower.contains("right") || lower.contains("left")) && lower.contains("player")) {
            return "contains";
        }
        return "";
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return "unknown";
        }
        String string = value.toString();
        return string.isBlank() ? "unknown" : string;
    }

    private static String countValue(Object value) {
        if (value == null) {
            return "unknown";
        }
        if (value instanceof Collection<?> collection) {
            return Integer.toString(collection.size());
        }
        if (value instanceof Map<?, ?> map) {
            return Integer.toString(map.size());
        }
        return value.toString();
    }

    public static int childCount(ModelPart part) {
        Map<String, ModelPart> children = children(part);
        return children == null ? -1 : children.size();
    }

    public static int cuboidCount(ModelPart part) {
        Object cuboids = fieldValue(part, "cuboids");
        if (cuboids == null) {
            return -1;
        }
        if (cuboids instanceof Collection<?> collection) {
            return collection.size();
        }
        return -1;
    }

    private static boolean hasPotentialChildren(ModelPart part) {
        return !childrenOrEmpty(part).isEmpty() || !customChildren(part).isEmpty();
    }

    private static String childNames(ModelPart part) {
        Map<String, ModelPart> children = children(part);
        if (children == null) {
            return "unavailable";
        }

        StringJoiner joiner = new StringJoiner(",");
        for (String name : children.keySet()) {
            joiner.add(name);
        }
        String result = joiner.toString();
        return result.isEmpty() ? "none" : result;
    }

    private static Map<String, ModelPart> childrenOrEmpty(ModelPart part) {
        Map<String, ModelPart> children = children(part);
        return children == null ? Map.of() : children;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ModelPart> children(ModelPart part) {
        if (part == null) {
            return null;
        }

        Optional<Field> field = field(ModelPart.class, "children");
        if (field.isEmpty()) {
            return null;
        }

        try {
            return (Map<String, ModelPart>) field.get().get(part);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logReflectionFailureOnce("field:ModelPart.children:get", exception);
            return null;
        }
    }

    private static Object fieldValue(Object object, String fieldName) {
        if (object == null) {
            return null;
        }

        Optional<Field> field = field(object.getClass(), fieldName);
        if (field.isEmpty()) {
            return null;
        }

        try {
            return field.get().get(object);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logReflectionFailureOnce("field:" + object.getClass().getName() + "." + fieldName + ":get", exception);
            return null;
        }
    }

    private static List<ModelPart> customChildren(ModelPart part) {
        if (part == null) {
            return List.of();
        }

        Optional<Method> method = method(part.getClass(), "getAllEMFCustomChildren");
        if (method.isEmpty()) {
            return List.of();
        }

        try {
            Object value = method.get().invoke(part);
            if (value instanceof ModelPart[] array) {
                return List.of(array);
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logReflectionFailureOnce("method:" + part.getClass().getName() + ".getAllEMFCustomChildren", exception);
        }
        return List.of();
    }

    private static Collection<?> allVanillaParts(ModelPart root) {
        if (root == null) {
            return List.of();
        }

        Optional<Method> method = method(root.getClass(), "getAllVanillaPartsEMF");
        if (method.isEmpty()) {
            return List.of();
        }

        try {
            Object value = method.get().invoke(root);
            if (value instanceof Collection<?> collection) {
                return collection;
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logReflectionFailureOnce("method:" + root.getClass().getName() + ".getAllVanillaPartsEMF", exception);
        }
        return List.of();
    }

    private static Optional<Method> method(Class<?> type, String name) {
        return METHOD_CACHE.computeIfAbsent(new MethodKey(type, name), key -> {
            try {
                Method found = key.type().getMethod(key.name());
                found.setAccessible(true);
                return Optional.of(found);
            } catch (NoSuchMethodException exception) {
                return Optional.empty();
            } catch (RuntimeException exception) {
                logReflectionFailureOnce("method:" + key.type().getName() + "." + key.name(), exception);
                return Optional.empty();
            }
        });
    }

    private static Optional<Field> field(Class<?> startType, String name) {
        return FIELD_CACHE.computeIfAbsent(new FieldKey(startType, name), key -> {
            Class<?> type = key.type();
            while (type != null) {
                try {
                    Field found = type.getDeclaredField(key.name());
                    found.setAccessible(true);
                    return Optional.of(found);
                } catch (NoSuchFieldException ignored) {
                    type = type.getSuperclass();
                } catch (RuntimeException exception) {
                    logReflectionFailureOnce("field:" + key.type().getName() + "." + key.name(), exception);
                    return Optional.empty();
                }
            }
            return Optional.empty();
        });
    }

    private static void logReflectionFailureOnce(String key, Exception exception) {
        if (REFLECTION_FAILURES.add(key)) {
            VpbFpaCompatClient.LOGGER.debug("[VPB-FPA ModelProbe] reflection unavailable key={} reason={}", key, exception.toString());
        }
    }

    public record NamedPart(String path, ModelPart part) {
    }

    public record PartMetadata(String attachedTo, String attach, String attachmentCount, String vanillaChildrenCount) {
    }

    private record TraversalResult(Map<String, ModelPart> parts, List<String> truncations) {
    }

    private record MethodKey(Class<?> type, String name) {
    }

    private record FieldKey(Class<?> type, String name) {
    }

    private static final class Traversal {
        private final int maxDepth;
        private final int maxParts;
        private final Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Map<String, ModelPart> parts = new LinkedHashMap<>();
        private final Set<String> truncations = new LinkedHashSet<>();

        private Traversal(int maxDepth, int maxParts) {
            this.maxDepth = maxDepth;
            this.maxParts = maxParts;
        }

        private boolean add(ModelPart part, String path, int depth) {
            if (part == null) {
                return false;
            }
            if (depth > maxDepth) {
                truncate("maxDepth");
                return false;
            }
            if (parts.size() >= maxParts) {
                truncate("maxParts");
                return false;
            }
            if (!visited.add(part)) {
                truncate("cycle");
                return false;
            }

            parts.put(uniquePath(path), part);
            return true;
        }

        private boolean isFull() {
            if (parts.size() >= maxParts) {
                truncate("maxParts");
                return true;
            }
            return false;
        }

        private void truncate(String reason) {
            truncations.add(reason);
        }

        private String uniquePath(String path) {
            if (!parts.containsKey(path)) {
                return path;
            }

            int suffix = 2;
            String candidate = path + "#" + suffix;
            while (parts.containsKey(candidate)) {
                suffix++;
                candidate = path + "#" + suffix;
            }
            return candidate;
        }
    }
}
