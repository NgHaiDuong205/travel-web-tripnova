package com.duong.travelweb.repository.impl;

import com.duong.travelweb.repository.CityRepository;
import com.duong.travelweb.repository.entity.CityEntity;
import org.springframework.stereotype.Repository;

import java.sql.*;
@Repository
public class CityRepositoryImpl implements CityRepository {
    static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=tripnova;encrypt=true;trustServerCertificate=true";
    static final String USER = "sa";
    static final String PASS = "Duong06112005!";
    @Override
    public CityEntity findNameById(Long id) {
        StringBuilder sql = new StringBuilder("SELECT name, country_id FROM cities ");
        StringBuilder where = new StringBuilder(" WHERE id = " + id + " ; ");
        sql.append(where);
        CityEntity cityEntity = new CityEntity();
        try(Connection conn = DriverManager.getConnection(DB_URL,USER,PASS);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql.toString());){
            while (rs.next()){
                cityEntity.setName(rs.getString("name"));
                cityEntity.setCountryID(rs.getString("country_id"));
            }
        }catch (SQLException e){
            e.printStackTrace();;
        }
        return cityEntity;
    }
}
