package com.anju.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

public final class AuditSnapshotUtil {

    private AuditSnapshotUtil() {
    }

    public static Map<String, Object> toMap(ObjectMapper objectMapper, Object object) {
        if (object == null) {
            return Map.of();
        }
        return objectMapper.convertValue(object, new TypeReference<>() {
        });
    }

    public static Map<String, Map<String, Object>> diff(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Map<String, Object>> changes = new HashMap<>();
        Map<String, Object> safeBefore = before == null ? Map.of() : before;
        Map<String, Object> safeAfter = after == null ? Map.of() : after;

        for (String key : safeBefore.keySet()) {
            Object oldValue = safeBefore.get(key);
            Object newValue = safeAfter.get(key);
            if (!equalsNullable(oldValue, newValue)) {
                changes.put(key, Map.of(
                        "before", oldValue,
                        "after", newValue
                ));
            }
        }
        for (String key : safeAfter.keySet()) {
            if (!safeBefore.containsKey(key)) {
                changes.put(key, Map.of(
                        "before", null,
                        "after", safeAfter.get(key)
                ));
            }
        }
        return changes;
    }

    private static boolean equalsNullable(Object a, Object b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}
