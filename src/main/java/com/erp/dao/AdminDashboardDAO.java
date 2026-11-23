package com.erp.dao;

import com.erp.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdminDashboardDAO {
    public Map<String,Long> getMetrics()throws SQLException{
        Map<String,Long> m=new LinkedHashMap<>();
        try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement(
                "SELECT (SELECT COUNT(*) FROM users WHERE role='STUDENT' AND status=1 AND deleted_at IS NULL) students,"
                +"(SELECT COUNT(*) FROM users WHERE role='TEACHER' AND status=1 AND deleted_at IS NULL) teachers,"
                +"(SELECT COUNT(*) FROM classes WHERE status=1) classes,"
                +"(SELECT COUNT(*) FROM sections s JOIN classes c ON c.id=s.class_id WHERE c.status=1) sections,"
                +"(SELECT COUNT(*) FROM subjects WHERE status=1) subjects,"
                +"(SELECT COUNT(*) FROM attendance WHERE attendance_date=CURRENT_DATE()) attendance_today,"
                +"(SELECT COUNT(*) FROM exams WHERE status=1 AND exam_date>=CURRENT_DATE()) upcoming_exams" );ResultSet rs=ps.executeQuery()){
            if(rs.next())for(String k:new String[]{"students","teachers","classes","sections","subjects","attendance_today","upcoming_exams"})m.put(k,rs.getLong(k));
        }return m;
    }
}
