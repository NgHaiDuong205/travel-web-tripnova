package com.duong.travelweb.customexception;

public class FieldRequireException extends RuntimeException {
    public FieldRequireException(String s){
        super(s);
    }
}
