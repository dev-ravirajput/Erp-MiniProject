package com.erp.dao;

import com.erp.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class AppSettingsDAO {
    public Map<String,String> getSettings() throws SQLException {
        Map<String,String> settings=new LinkedHashMap<>();settings.put("site_name","EduERP");settings.put("logo_url","");settings.put("favicon_url","");
        try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement("SELECT setting_key,setting_value FROM app_settings WHERE setting_key IN ('site_name','logo_url','favicon_url')");ResultSet rs=ps.executeQuery()){
            while(rs.next())settings.put(rs.getString("setting_key"),rs.getString("setting_value"));
        }return settings;
    }

    public void saveSettings(Map<String,String> values)throws SQLException{
        try(Connection con=DBConnection.getConnection()){
            con.setAutoCommit(false);
            try(PreparedStatement ps=con.prepareStatement("INSERT INTO app_settings(setting_key,setting_value) VALUES(?,?) ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value)")){
                for(String key:new String[]{"site_name","logo_url","favicon_url"}){ps.setString(1,key);ps.setString(2,values.get(key));ps.addBatch();}ps.executeBatch();con.commit();
            }catch(SQLException e){con.rollback();throw e;}
        }
    }
}
