package com.duong.travelweb.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionJDBCUtil {
    static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=tripnova;encrypt=true;trustServerCertificate=true";
    static final String USER = "sa";
    static final String PASS = "Duong06112005!";
    public static Connection getConnection(){
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL,USER,PASS);
        } catch (SQLException e){
            e.printStackTrace();
        }
        return conn;
    }
}
