package com.duong.travelweb.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CountrySearchBuilder {
    private UUID id;
    private String name;
    private String countryCode;
    private String slug;
    private UUID continentId;
    private List<String> typeCode = new ArrayList<>();

    private CountrySearchBuilder(Builder builder){
        this.id = builder.id;
        this.name = builder.name;
        this.countryCode = builder.countryCode;
        this.slug = builder.slug;
        this.continentId = builder.continentId;
        this.typeCode = builder.typeCode;
    }
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getSlug() {
        return slug;
    }

    public UUID getContinentId() {
        return continentId;
    }

    public List<String> getTypeCode() {
        return typeCode;
    }
    public static class Builder{
        private UUID id;
        private String name;
        private String countryCode;
        private String slug;
        private UUID continentId;
        private List<String> typeCode = new ArrayList<>();

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder countryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public Builder slug(String slug) {
            this.slug = slug;
            return this;
        }

        public Builder continentId(UUID continentId) {
            this.continentId = continentId;
            return this;
        }

        public Builder typeCode(List<String> typeCode) {
            this.typeCode = typeCode;
            return this;
        }

        public CountrySearchBuilder build(){
            return new CountrySearchBuilder(this);
        }
    }
}
