package com.erp.dao;

import com.erp.util.DBConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GradeScaleDAO {
    public List<Map<String,Object>> list()throws SQLException{List<Map<String,Object>> rows=new ArrayList<>();try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT id,min_percent,grade_label,status FROM grade_scales ORDER BY min_percent DESC");ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getLong("id"));m.put("min_percent",r.getBigDecimal("min_percent"));m.put("grade_label",r.getString("grade_label"));m.put("status",r.getInt("status"));rows.add(m);}}return rows;}
    public void save(Long id,BigDecimal minPercent,String label)throws SQLException{String sql=id==null?"INSERT INTO grade_scales(min_percent,grade_label) VALUES(?,?)":"UPDATE grade_scales SET min_percent=?,grade_label=?,status=1 WHERE id=?";try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setBigDecimal(1,minPercent);p.setString(2,label);if(id!=null)p.setLong(3,id);p.executeUpdate();}}
    public void deactivate(long id)throws SQLException{try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("UPDATE grade_scales SET status=0 WHERE id=? AND min_percent<>0")){p.setLong(1,id);if(p.executeUpdate()!=1)throw new SQLException("The baseline grade cannot be deactivated.");}}
}
