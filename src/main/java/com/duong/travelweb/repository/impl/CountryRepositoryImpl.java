package com.duong.travelweb.repository.impl;

import com.duong.travelweb.StringUtil.NumberUtil;
import com.duong.travelweb.StringUtil.StringUtil;
import com.duong.travelweb.repository.CountryRepository;
import com.duong.travelweb.repository.entity.CountryEntity;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class CountryRepositoryImpl implements CountryRepository {
    static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=tripnova;encrypt=true;trustServerCertificate=true";
    static final String USER = "sa";
    static final String PASS = "Duong06112005!";

    public static void joinTable(Map<String,Object> params,List<String> typeCode,StringBuilder sql){
        String continentId = (String)params.get("continent_id");
        if(StringUtil.checkString(continentId)){
            if(NumberUtil.isNumber(continentId)){
                sql.append(" INNER JOIN continents C on C.id = CO.continent_id ");
            }
        }
    }

    public static void queryNomal(Map<String,Object> params,StringBuilder where){
        for(Map.Entry<String,Object> it : params.entrySet()){
            if(!it.getKey().equals("continent_id") && !it.getKey().equals("typeCode")){
                String value =(String) it.getValue();
                if(StringUtil.checkString(value)){
                    if(NumberUtil.isNumber(value)){
                        where.append(" AND CO." + it.getKey() + " = " + value);
                    } else {
                        where.append(" AND CO." + it.getKey() + " LIKE N'%" + value + "%' ");
                    }
                }
            }
        }
    }


    public static void querySpecial(Map<String,Object> params, List<String> typeCode, StringBuilder where){
        String continentId = (String) params.get("continent_id");
        if(StringUtil.checkString(continentId)){
            if (NumberUtil.isNumber(continentId)){
                where.append(" AND C.id = " + continentId + " ");
            }
        }
    }

    @Override
    public List<CountryEntity> findCountry(Map<String,Object> params,List<String> typeCode) {
        StringBuilder sql = new StringBuilder(" SELECT CO.* FROM countries CO ");
        joinTable(params,typeCode,sql);
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        queryNomal(params, where);
        querySpecial(params, typeCode, where);
        sql.append(where);
        List<CountryEntity> results = new ArrayList<>();
        try(Connection conn = DriverManager.getConnection(DB_URL,USER,PASS);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql.toString());) {
            while (rs.next()) {
                CountryEntity country = new CountryEntity();
                country.setId(rs.getString("id"));
                country.setCountryName(rs.getString("name"));
                country.setFlag(rs.getString("flag"));
                country.setContinentId(rs.getInt("continent_id"));
                results.add(country);
            }
        } catch(SQLException e){
            e.printStackTrace();
        }
        return results;
    }


    @Override
    public CountryEntity findNameById(String id) {
        StringBuilder sql = new StringBuilder(" SELECT name FROM countries");
        StringBuilder where = new StringBuilder(" WHERE id = '" + id + "';");
        sql.append(where);
        CountryEntity countryEntity= new CountryEntity();
        try(Connection conn = DriverManager.getConnection(DB_URL,USER,PASS);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql.toString());){
            while (rs.next()){
                countryEntity.setCountryName(rs.getString("name"));
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return countryEntity;
    }
}
