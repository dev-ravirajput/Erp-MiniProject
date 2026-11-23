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

public class AttendanceDAO {
    public List<Map<String, Object>> getRecords(String date, String status) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT a.id,a.attendance_date,a.status,a.remarks,s.id AS student_id,su.name AS student_name,"
                + "t.id AS teacher_id,tu.name AS teacher_name,sub.id AS subject_id,sub.subject_name,"
                + "sec.id AS section_id,sec.section_name,c.class_name FROM attendance a "
                + "JOIN students s ON s.id=a.student_id JOIN users su ON su.id=s.user_id "
                + "JOIN teachers t ON t.id=a.teacher_id JOIN users tu ON tu.id=t.user_id "
                + "JOIN subjects sub ON sub.id=a.subject_id JOIN sections sec ON sec.id=a.section_id "
                + "JOIN classes c ON c.id=sec.class_id WHERE 1=1";
        if (date != null && !date.isEmpty()) sql += " AND a.attendance_date = ?";
        if (status != null && !status.isEmpty()) sql += " AND a.status = ?";
        sql += " ORDER BY a.attendance_date DESC,su.name";
        try (Connection con=DBConnection.getConnection(); PreparedStatement ps=con.prepareStatement(sql)) {
            int i=1; if(date!=null&&!date.isEmpty()) ps.setDate(i++,Date.valueOf(date)); if(status!=null&&!status.isEmpty()) ps.setString(i,status);
            try(ResultSet rs=ps.executeQuery()) { while(rs.next()) { Map<String,Object> r=new LinkedHashMap<>();
                for(String k:new String[]{"id","student_id","teacher_id","subject_id","section_id"}) r.put(k,rs.getLong(k));
                for(String k:new String[]{"attendance_date","status","remarks","student_name","teacher_name","subject_name","section_name","class_name"}) r.put(k,rs.getObject(k)); rows.add(r);
            }}
        }
        return rows;
    }

    public Map<String,Object> getRecord(long id) throws SQLException {
        try(Connection con=DBConnection.getConnection(); PreparedStatement ps=con.prepareStatement("SELECT id,student_id,teacher_id,subject_id,section_id,attendance_date,status,remarks FROM attendance WHERE id=?")) {
            ps.setLong(1,id); try(ResultSet rs=ps.executeQuery()) { if(!rs.next()) return null; Map<String,Object> r=new LinkedHashMap<>();
                for(String k:new String[]{"id","student_id","teacher_id","subject_id","section_id"}) r.put(k,rs.getLong(k));
                r.put("attendance_date",rs.getDate("attendance_date")); r.put("status",rs.getString("status")); r.put("remarks",rs.getString("remarks")); return r;
            }
        }
    }

    public void save(Map<String,String> r, Long id) throws SQLException {
        String sql=id==null?"INSERT INTO attendance(student_id,teacher_id,subject_id,section_id,attendance_date,status,remarks) VALUES(?,?,?,?,?,?,?)":"UPDATE attendance SET session_id=NULL,academic_year=NULL,student_id=?,teacher_id=?,subject_id=?,section_id=?,attendance_date=?,status=?,remarks=? WHERE id=?";
        try(Connection con=DBConnection.getConnection(); PreparedStatement ps=con.prepareStatement(sql)) {
            ps.setLong(1,Long.parseLong(r.get("student_id"))); ps.setLong(2,Long.parseLong(r.get("teacher_id"))); ps.setLong(3,Long.parseLong(r.get("subject_id"))); ps.setLong(4,Long.parseLong(r.get("section_id"))); ps.setDate(5,Date.valueOf(r.get("attendance_date"))); ps.setString(6,r.get("status"));
            if(r.get("remarks")==null||r.get("remarks").isEmpty()) ps.setNull(7,java.sql.Types.VARCHAR); else ps.setString(7,r.get("remarks"));
            if(id!=null) ps.setLong(8,id); ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException { try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement("DELETE FROM attendance WHERE id=?")){ps.setLong(1,id); if(ps.executeUpdate()!=1)throw new SQLException("Attendance record not found.");} }

    public List<Map<String,Object>> getOptions(String kind) throws SQLException {
        String sql;
        switch(kind) {
            case "students": sql="SELECT s.id,u.name,u.email FROM students s JOIN users u ON u.id=s.user_id WHERE u.role='STUDENT' AND u.status=1 AND u.deleted_at IS NULL ORDER BY u.name"; break;
            case "teachers": sql="SELECT t.id,u.name,u.email FROM teachers t JOIN users u ON u.id=t.user_id WHERE u.role='TEACHER' AND u.status=1 AND u.deleted_at IS NULL ORDER BY u.name"; break;
            case "subjects": sql="SELECT id,subject_name,subject_code FROM subjects WHERE status=1 ORDER BY subject_name"; break;
            case "sections": sql="SELECT sec.id,sec.section_name,c.class_name,c.id AS class_id FROM sections sec JOIN classes c ON c.id=sec.class_id WHERE c.status=1 ORDER BY c.class_name,sec.section_name"; break;
            default: throw new IllegalArgumentException("Unknown attendance option");
        }
        List<Map<String,Object>> rows=new ArrayList<>(); try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement(sql);ResultSet rs=ps.executeQuery()) {
            while(rs.next()){Map<String,Object> r=new LinkedHashMap<>(); r.put("id",rs.getLong("id"));
                if("students".equals(kind)||"teachers".equals(kind)){r.put("name",rs.getString("name"));r.put("email",rs.getString("email"));}
                if("subjects".equals(kind)){r.put("subject_name",rs.getString("subject_name"));r.put("subject_code",rs.getString("subject_code"));}
                if("sections".equals(kind)){r.put("section_name",rs.getString("section_name"));r.put("class_name",rs.getString("class_name"));r.put("class_id",rs.getLong("class_id"));}
                rows.add(r);
            }
        } return rows;
    }
}
