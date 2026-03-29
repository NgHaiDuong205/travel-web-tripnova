package com.duong.travelweb.repository.impl;

import com.duong.travelweb.repository.CountryRepository;
import com.duong.travelweb.repository.entity.CountryEntity;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
@Repository
public class CountryRepositoryImpl implements CountryRepository {
    static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=springboot_test;encrypt=true;trustServerCertificate=true";
    static final String USER = "NguyenHaiDuong";
    static final String PASS = "Duong06112005!";
    @Override
    public List<CountryEntity> findAll(String countryName) {
        String sql = "SELECT * FROM countries WHERE country_name like '%" + countryName + "%'";
        List<CountryEntity> results = new ArrayList<>();
        try(Connection conn = DriverManager.getConnection(DB_URL,USER,PASS);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);) {
            while (rs.next()) {
                CountryEntity country = new CountryEntity();
                country.setCountry_name(rs.getString("country_name"));
                country.setCode(rs.getString("code"));
                country.setPopulation(rs.getInt("population"));
                country.setCountry_id(rs.getString("country_id"));
                country.setFlag(rs.getString("flag"));
                country.setCapital(rs.getString("capital"));
                country.setRegion(rs.getString("region"));
                country.setCurrencies(rs.getString("currencies"));
                results.add(country);
            }
        } catch(SQLException e){
            e.printStackTrace();
        }
        return results;
    }
}
