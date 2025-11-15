package com.cocode.prepnest;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class ListMapUtils {

    /**
     * Sorts a List<HashMap<String, Object>> by key with specified type.
     *
     * @param list      List to sort
     * @param key       The key to sort by
     * @param ascending true for ascending, false for descending
     * @param type      SortType (NUMBER, STRING, TIMESTAMP_STRING, SESSION)
     */
    public static void sortListByKey(List<HashMap<String, Object>> list, String key, boolean ascending, SortType type) {
        Collections.sort(list, new Comparator<HashMap<String, Object>>() {
            @Override
            public int compare(HashMap<String, Object> o1, HashMap<String, Object> o2) {
                int comparison = 0;

                switch (type) {
                    case NUMBER:
                        Number n1 = safeNumber(o1.get(key));
                        Number n2 = safeNumber(o2.get(key));
                        comparison = Double.compare(n1.doubleValue(), n2.doubleValue());
                        break;

                    case STRING:
                        String s1 = safeString(o1.get(key));
                        String s2 = safeString(o2.get(key));
                        comparison = s1.compareToIgnoreCase(s2);
                        break;

                    case TIMESTAMP_STRING:
                        long t1 = safeTimestamp(o1.get(key));
                        long t2 = safeTimestamp(o2.get(key));
                        comparison = Long.compare(t1, t2);
                        break;
                    case SESSION:
                        long session1 = safeSession(o1.get(key));
                        long session2 = safeSession(o2.get(key));
                        comparison = Long.compare(session1, session2);
                        break;
                }

                return ascending ? comparison : -comparison;
            }
        });
    }

    // Helpers
    private static Number safeNumber(Object obj) {
        if (obj instanceof Number) return (Number) obj;
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private static long safeSession(Object obj) {
        if (obj instanceof String) {
            String str = ((String) obj);
            return (Long.parseLong(str.substring(str.lastIndexOf('-'))));
        }
        return 0L;
    }

    private static String safeString(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    private static long safeTimestamp(Object obj) {
        try {
            return Long.parseLong(obj.toString());
        } catch (Exception e) {
            return 0L;
        }
    }

    public enum SortType {
        NUMBER,
        STRING,
        TIMESTAMP_STRING, // timestamp stored as string in ms
        SESSION
    }
}
