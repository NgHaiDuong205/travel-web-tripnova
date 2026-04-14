package com.duong.travelweb.repository.impl;

import com.duong.travelweb.StringUtil.ConnectionJDBCUtil;
import com.duong.travelweb.StringUtil.NumberUtil;
import com.duong.travelweb.StringUtil.StringUtil;
import com.duong.travelweb.builder.HotelSearchBuider;
import com.duong.travelweb.repository.HotelRepository;
import com.duong.travelweb.repository.entity.HotelEntity;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Repository
public class HotelRepositoryImpl implements HotelRepository {
    public static void joinTable(HotelSearchBuider hotelSearchBuider, StringBuilder sql){
        String cityId = hotelSearchBuider.getCityId() != null ? hotelSearchBuider.getCityId().toString() : null;
        String countryId = hotelSearchBuider.getCountryId();
        
        if(StringUtil.checkString(countryId) || StringUtil.checkString(cityId)){
            boolean joinCity = false;
            
            if(StringUtil.checkString(cityId)){
                if(NumberUtil.isNumber(cityId)){
                    sql.append(" INNER JOIN cities CI ON CI.id = H.city_id ");
                    joinCity = true;
                }
            }
            if(StringUtil.checkString(countryId)){
                if(!joinCity){
                    sql.append(" INNER JOIN cities CI ON CI.id = H.city_id ");
                }
                sql.append(" INNER JOIN countries C ON C.id = CI.country_id ");
            }
        }
    }

    public static void queryNormal(HotelSearchBuider hotelSearchBuider, StringBuilder where){
        try{
            Field[] fields = HotelSearchBuider.class.getDeclaredFields();
            for(Field item : fields){
                item.setAccessible(true);
                String fieldName = item.getName();
                if(!fieldName.equals("cityId") && !fieldName.equals("countryId") && !fieldName.equals("typeCode") && !fieldName.startsWith("this")){
                    Object objValue =item.get(hotelSearchBuider);
                    if(objValue != null){
                        String value = objValue.toString();
                        if(StringUtil.checkString(value)){
                            if(NumberUtil.isNumber(value)){
                                where.append(" AND H." + fieldName + " = " + value +" ");
                            }
                            else {
                                where.append(" AND H." + fieldName + " LIKE N'%" + value +"%' " );
                            }
                        }
                    }
                }
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public static void querySpecial(HotelSearchBuider hotelSearchBuider, StringBuilder where){
        String cityId = hotelSearchBuider.getCityId() != null ? hotelSearchBuider.getCityId().toString() : null;
        String countryId = hotelSearchBuider.getCountryId();
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
    public List<HotelEntity> findHotel(HotelSearchBuider hotelSearchBuider) {
        StringBuilder sql = new StringBuilder("SELECT H.* FROM hotels H ");
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        joinTable(hotelSearchBuider,sql);
        queryNormal(hotelSearchBuider,where);
        querySpecial(hotelSearchBuider,where);
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
