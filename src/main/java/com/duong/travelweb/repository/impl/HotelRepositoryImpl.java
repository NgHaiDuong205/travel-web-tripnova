package com.duong.travelweb.repository.impl;

import com.duong.travelweb.StringUtil.ConnectionJDBCUtil;
import com.duong.travelweb.StringUtil.NumberUtil;
import com.duong.travelweb.StringUtil.StringUtil;
import com.duong.travelweb.repository.HotelRepository;
import com.duong.travelweb.repository.entity.HotelEntity;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Repository
public class HotelRepositoryImpl implements HotelRepository {
    public static void joinTable(Map<String,Object> params , List<String> typeCode , StringBuilder sql){
        String cityId = (String) params.get("city_id");
        String countryId = (String) params.get("country_id");
        Boolean joinCity = false;
        if(StringUtil.checkString(cityId)){
            if(NumberUtil.isNumber(cityId)){
                sql.append(" INNER JOIN cities CI ON CI.id = H.city_id ");
                joinCity = true;
            }
        }
        if(StringUtil.checkString(countryId)){
            if(!joinCity){
                sql.append(" INNER JOIN cities CI ON CI.id = H.city_id ");
                sql.append(" INNER JOIN countries C ON C.id = CI.country_id ");
            }
            else {
                sql.append(" INNER JOIN countries C ON C.id = CI.country_id ");
            }
        }
    }

    public static void queryNormal(Map<String,Object> params , StringBuilder where){
        for(Map.Entry<String,Object> it : params.entrySet()){
            if(!it.getKey().equals("city_id") && !it.getKey().equals("country_id") && !it.getKey().equals("typeCode")){
                String value =(String) it.getValue();
                if(StringUtil.checkString(value)){
                    if(NumberUtil.isNumber(value)){
                        where.append(" AND H." + it.getKey() + " = " + value +" ");
                    }
                    else {
                        where.append(" AND H." + it.getKey() + " LIKE N'%" + value +"%' " );
                    }
                }
            }
        }
    }

    public static void querySpecial(Map<String,Object> params , List<String> typeCode , StringBuilder where){
        String cityId = (String) params.get("city_id");
        String countryId = (String) params.get("country_id");
        if(StringUtil.checkString(cityId)){
            if(NumberUtil.isNumber(cityId)){
                where.append(" AND CI.id = " + cityId +" ");
            }
        }
        if(StringUtil.checkString(countryId)){
            where.append(" AND C.id = '" + countryId + "' ");
        }
    }

    @Override
    public List<HotelEntity> findHotel(Map<String, Object> params, List<String> typeCode) {
        StringBuilder sql = new StringBuilder("SELECT H.* FROM hotels H ");
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        joinTable(params,typeCode,sql);
        queryNormal(params,where);
        querySpecial(params,typeCode,where);
        sql.append(where);
        List<HotelEntity> hotelEntities = new ArrayList<HotelEntity>();
        try(Connection conn = ConnectionJDBCUtil.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql.toString());){
            while (rs.next()){
                HotelEntity hotel = new HotelEntity();
                hotel.setId(rs.getInt("id"));
                hotel.setCityId(rs.getInt("city_id"));
                hotel.setName(rs.getString("name"));
                hotel.setAddress(rs.getString("address"));
                hotel.setRating(rs.getDouble("rating"));
                hotel.setAmenities(rs.getString("amenities"));
                hotel.setCreateAt(rs.getObject("created_at", java.time.LocalDateTime.class));
                hotel.setRepairAt(rs.getObject("repair_at", java.time.LocalDateTime.class));
                hotel.setDescription(rs.getString("description"));
                hotelEntities.add(hotel);
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return hotelEntities;
    }
}
