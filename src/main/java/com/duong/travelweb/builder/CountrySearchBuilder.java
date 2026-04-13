package com.duong.travelweb.builder;

import java.util.ArrayList;
import java.util.List;

public class CountrySearchBuilder {
    private String id;
    private String name;
    private String flag;
    private Integer continentId;
    private List<String> typeCode = new ArrayList<>();

    private CountrySearchBuilder(Builder builder){
        this.id = builder.id;
        this.name = builder.name;
        this.flag = builder.flag;
        this.continentId = builder.continentId;
        this.typeCode = builder.typeCode;
    }
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFlag() {
        return flag;
    }

    public Integer getContinentId() {
        return continentId;
    }

    public List<String> getTypeCode() {
        return typeCode;
    }
    public static class Builder{
        private String id;
        private String name;
        private String flag;
        private Integer continentId;
        private List<String> typeCode = new ArrayList<>();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder flag(String flag) {
            this.flag = flag;
            return this;
        }

        public Builder continentId(Integer continentId) {
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
