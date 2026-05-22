package com.duong.travelweb.util;

import java.util.Map;
import java.util.UUID;

public class MapUtil {
    public static <T> T getObject(Map<String, Object> params, String key, Class<T> tClass) {
        Object obj = params.getOrDefault(key, null);
        if (obj != null) {
            String strValue = obj.toString().trim();
            if (strValue.isEmpty()) {
                return null;
            }
            Object parsedObj;
            if (tClass.equals(Long.class)) {
                parsedObj = Long.valueOf(strValue);
            } else if (tClass.equals(Integer.class)) {
                parsedObj = Integer.valueOf(strValue);
            } else if (tClass.equals(Double.class)) {
                parsedObj = Double.valueOf(strValue);
            } else if (tClass.equals(String.class)) {
                parsedObj = obj.toString();
            } else if (tClass.equals(UUID.class)) {
                parsedObj = UUID.fromString(strValue);
            } else {
                parsedObj = obj;
            }
            return tClass.cast(parsedObj);
        }
        return null;
    }
}
