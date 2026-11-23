package com.erp.dao;

import com.erp.util.DBConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminReportDAO {
    public List<Map<String,Object>> getSections()throws SQLException{
        List<Map<String,Object>> rows=new ArrayList<>();
        try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement("SELECT s.id,s.section_name,c.class_name,c.academic_year FROM sections s JOIN classes c ON c.id=s.class_id ORDER BY c.academic_year DESC,c.class_name,s.section_name");ResultSet rs=ps.executeQuery()){
            while(rs.next()){Map<String,Object> r=new LinkedHashMap<>();r.put("id",rs.getLong("id"));r.put("section_name",rs.getString("section_name"));r.put("class_name",rs.getString("class_name"));r.put("academic_year",rs.getString("academic_year"));rows.add(r);}
        }return rows;
    }
    public List<String> getAcademicYears()throws SQLException{List<String> years=new ArrayList<>();try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT DISTINCT academic_year FROM student_class ORDER BY academic_year DESC");ResultSet r=p.executeQuery()){while(r.next())years.add(r.getString(1));}return years;}

    public List<Map<String,Object>> getAttendanceReport(String section,String from,String to,String year)throws SQLException{
        String sql="SELECT u.name,st.admission_no,stc.roll_no,stc.academic_year,COUNT(a.id) total,"
                +"SUM(CASE WHEN a.status='PRESENT' THEN 1 ELSE 0 END) present,"
                +"SUM(CASE WHEN a.status='ABSENT' THEN 1 ELSE 0 END) absent,"
                +"SUM(CASE WHEN a.status='LATE' THEN 1 ELSE 0 END) late,"
                +"SUM(CASE WHEN a.status='LEAVE' THEN 1 ELSE 0 END) leaves "
                +"FROM students st JOIN users u ON u.id=st.user_id JOIN student_class stc ON stc.student_id=st.id JOIN sections sec ON sec.id=stc.section_id JOIN classes c ON c.id=sec.class_id "
                +"LEFT JOIN attendance a ON a.student_id=st.id AND a.section_id=stc.section_id AND COALESCE(a.academic_year,c.academic_year)=stc.academic_year ";
        if(from!=null&&!from.isEmpty())sql+="AND a.attendance_date>=? ";if(to!=null&&!to.isEmpty())sql+="AND a.attendance_date<=? ";
        sql+="WHERE u.role='STUDENT' AND u.status=1 AND u.deleted_at IS NULL ";if(section!=null&&!section.isEmpty())sql+="AND stc.section_id=? ";if(year!=null&&!year.isEmpty())sql+="AND stc.academic_year=? ";
        sql+="GROUP BY stc.id,u.name,st.admission_no,stc.roll_no,stc.academic_year ORDER BY stc.academic_year DESC,u.name";
        List<Map<String,Object>> rows=new ArrayList<>();try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement(sql)){
            int p=1;if(from!=null&&!from.isEmpty())ps.setDate(p++,Date.valueOf(from));if(to!=null&&!to.isEmpty())ps.setDate(p++,Date.valueOf(to));if(section!=null&&!section.isEmpty())ps.setLong(p++,Long.parseLong(section));if(year!=null&&!year.isEmpty())ps.setString(p,year);
            try(ResultSet rs=ps.executeQuery()){while(rs.next()){Map<String,Object> r=new LinkedHashMap<>();for(String k:new String[]{"name","admission_no","roll_no"})r.put(k,rs.getString(k));for(String k:new String[]{"total","present","absent","late","leaves"})r.put(k,rs.getLong(k));r.put("academic_year",rs.getString("academic_year"));rows.add(r);}}
        }return rows;
    }
}
