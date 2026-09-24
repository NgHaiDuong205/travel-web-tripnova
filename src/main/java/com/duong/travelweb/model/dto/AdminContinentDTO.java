package com.duong.travelweb.model.dto;

import java.util.UUID;

/** Châu lục cho màn admin. */
public class AdminContinentDTO {
    private UUID id;
    private String code;
    private String name;
    private long countryCount;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCountryCount() {
        return countryCount;
    }

    public void setCountryCount(long countryCount) {
        this.countryCount = countryCount;
    }
}
