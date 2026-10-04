package com.duong.travelweb.util;

public class StringUtil {
    public static boolean checkString(String data){
        if(data != null && !data.trim().equals("")){
            return true;
        }
        else return false;
    }

    /** Chữ thường, bỏ dấu tiếng Việt (kể cả đ → d) để so khớp tên; null → "". */
    public static String removeAccents(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = java.text.Normalizer.normalize(value.toLowerCase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").replace('đ', 'd');
    }
}
