package com.duong.travelweb.repository.impl;

import com.duong.travelweb.repository.CountryRepository;
import com.duong.travelweb.repository.entity.CountryEntity;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
@Repository
public class CountryRepositoryImpl implements CountryRepository {
    static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=travel_web;encrypt=true;trustServerCertificate=true";
    static final String USER = "NguyenHaiDuong";
    static final String PASS = "Duong06112005!";
    @Override
    public List<CountryEntity> findCountry(String countryName) {
        String sql = "SELECT * FROM countries WHERE country_name like '%" + countryName + "%'";
        List<CountryEntity> results = new ArrayList<>();
        try(Connection conn = DriverManager.getConnection(DB_URL,USER,PASS);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);) {
            while (rs.next()) {
                CountryEntity country = new CountryEntity();
                country.setId(rs.getString("id"));
                country.setCountryName(rs.getString("country_name"));
                country.setFlag(rs.getString("flag"));
                country.setContinent(rs.getString("continent"));
                results.add(country);
            }
        } catch(SQLException e){
            e.printStackTrace();
        }
        return results;
    }
}
