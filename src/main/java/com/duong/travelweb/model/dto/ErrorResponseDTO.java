package com.duong.travelweb.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

public class ErrorResponseDTO {
    private String error;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String code;
    private List<String> detail = new ArrayList<String>();

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<String> getDetail() {
        return detail;
    }

    public void setDetail(List<String> detail) {
        this.detail = detail;
    }
}
