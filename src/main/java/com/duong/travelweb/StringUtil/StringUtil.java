package com.duong.travelweb.StringUtil;

public class StringUtil {
    public static boolean checkString(String data){
        if(data != null && !data.trim().equals("")){
            return true;
        }
        else return false;
    }
}
