package com.duong.travelweb.repository.impl;

import com.duong.travelweb.builder.CountrySearchBuilder;
import com.duong.travelweb.StringUtil.ConnectionJDBCUtil;
import com.duong.travelweb.StringUtil.NumberUtil;
import com.duong.travelweb.StringUtil.StringUtil;
import com.duong.travelweb.repository.CountryRepository;
import com.duong.travelweb.repository.entity.CountryEntity;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class CountryRepositoryImpl implements CountryRepository {
    public static void joinTable(CountrySearchBuilder countrySearchBuilder,StringBuilder sql){
        if(countrySearchBuilder.getContinentId() != null) {
            String continentId = countrySearchBuilder.getContinentId().toString();
            if(StringUtil.checkString(continentId)){
                if(NumberUtil.isNumber(continentId)){
                    sql.append(" INNER JOIN continents C on C.id = CO.continent_id ");
                }
            }
        }
    }

    public static void queryNomal(CountrySearchBuilder countrySearchBuilder,StringBuilder where){
        try{
            Field[] fields = CountrySearchBuilder.class.getDeclaredFields();
            for(Field item : fields){
                item.setAccessible(true);
                String fieldName = item.getName();
                if(!fieldName.equals("continentId") && !fieldName.equals("typeCode") && !fieldName.startsWith("this")){
                    Object objValue = item.get(countrySearchBuilder);
                    if(objValue != null) {
                        String value = objValue.toString();
                        if(StringUtil.checkString(value)){
                            if(NumberUtil.isNumber(value)){
                                where.append(" AND CO." + fieldName + " = " + value);
                            } else {
                                where.append(" AND CO." + fieldName + " LIKE N'%" + value + "%' ");
                            }
                        }
                    }
                }
            }
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }


    public static void querySpecial(CountrySearchBuilder countrySearchBuilder, StringBuilder where){
        if(countrySearchBuilder.getContinentId() != null) {
            String continentId = countrySearchBuilder.getContinentId().toString();
            if(StringUtil.checkString(continentId)){
                if (NumberUtil.isNumber(continentId)){
                    where.append(" AND C.id = " + continentId + " ");
                }
            }
        }
    }

    @Override
    public List<CountryEntity> findCountry(CountrySearchBuilder countrySearchBuilder) {
        StringBuilder sql = new StringBuilder(" SELECT CO.* FROM countries CO ");
        joinTable(countrySearchBuilder,sql);
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        queryNomal(countrySearchBuilder, where);
        querySpecial(countrySearchBuilder, where);
        sql.append(where);
        List<CountryEntity> results = new ArrayList<>();
        try(Connection conn = ConnectionJDBCUtil.getConnection();
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
        try(Connection conn = ConnectionJDBCUtil.getConnection();
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
