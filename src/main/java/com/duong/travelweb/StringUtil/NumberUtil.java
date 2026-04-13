package com.duong.travelweb.utils;

public class NumberUtil {
    public static boolean isNumber(String value){
        try{
            Long number = Long.parseLong(value);
        } catch (NumberFormatException ex){
            return false;
        }
        return true;
    }
}
