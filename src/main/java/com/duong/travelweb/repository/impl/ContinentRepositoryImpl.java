package com.duong.travelweb.repository.impl;

import com.duong.travelweb.StringUtil.ConnectionJDBCUtil;
import com.duong.travelweb.repository.ContinentRepository;
import com.duong.travelweb.repository.entity.ContinentEntity;
import org.springframework.stereotype.Repository;

import java.sql.*;
@Repository
public class ContinentRepositoryImpl implements ContinentRepository {
    @Override
    public ContinentEntity findNameById(Long id) {
        StringBuilder sql = new StringBuilder(" SELECT name FROM continents");
        StringBuilder where = new StringBuilder(" WHERE id = " + id + ";");
        sql.append(where);
        ContinentEntity continentEntity = new ContinentEntity();
        try(Connection conn = ConnectionJDBCUtil.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql.toString());){
            while (rs.next()){
                continentEntity.setName(rs.getString("name"));
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return continentEntity;
    }
}
