package com.erp.dao;

import com.erp.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** User-scoped in-app notifications. */
public class NotificationDAO {
    public int unreadCount(long userId) throws SQLException {
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM notifications WHERE user_id=? AND read_at IS NULL")){
            p.setLong(1,userId);try(ResultSet r=p.executeQuery()){r.next();return r.getInt(1);}
        }
    }
    public List<Map<String,Object>> recent(long userId) throws SQLException {
        List<Map<String,Object>> rows=new ArrayList<>();
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT id,title,message,target_path,created_at,read_at FROM notifications WHERE user_id=? ORDER BY created_at DESC,id DESC LIMIT 50")){
            p.setLong(1,userId);try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> row=new LinkedHashMap<>();for(String k:new String[]{"id","title","message","target_path","created_at","read_at"})row.put(k,r.getObject(k));rows.add(row);}}
        }
        return rows;
    }
    public String markRead(long userId,long notificationId) throws SQLException {
        try(Connection c=DBConnection.getConnection()){
            try(PreparedStatement p=c.prepareStatement("UPDATE notifications SET read_at=COALESCE(read_at,CURRENT_TIMESTAMP) WHERE id=? AND user_id=?")){p.setLong(1,notificationId);p.setLong(2,userId);p.executeUpdate();}
            try(PreparedStatement p=c.prepareStatement("SELECT target_path FROM notifications WHERE id=? AND user_id=?")){p.setLong(1,notificationId);p.setLong(2,userId);try(ResultSet r=p.executeQuery()){return r.next()?r.getString(1):null;}}
        }
    }
    public void notifySectionStudents(String year,long sectionId,String title,String message,String targetPath) throws SQLException {
        String sql="INSERT INTO notifications(user_id,title,message,target_path) SELECT DISTINCT u.id,?,?,? FROM student_class sc JOIN students s ON s.id=sc.student_id JOIN users u ON u.id=s.user_id WHERE sc.section_id=? AND sc.academic_year=? AND u.status=1 AND u.deleted_at IS NULL";
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setString(1,title);p.setString(2,message);p.setString(3,targetPath);p.setLong(4,sectionId);p.setString(5,year);p.executeUpdate();}
    }
    public void notifyAssignmentStudents(long assignmentId,long teacherId,String title,String message,String targetPath) throws SQLException {
        String sql="INSERT INTO notifications(user_id,title,message,target_path) SELECT DISTINCT u.id,?,?,? FROM assignments a JOIN student_class sc ON sc.section_id=a.section_id AND sc.academic_year=a.academic_year JOIN students s ON s.id=sc.student_id JOIN users u ON u.id=s.user_id WHERE a.id=? AND a.teacher_id=? AND u.status=1 AND u.deleted_at IS NULL";
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setString(1,title);p.setString(2,message);p.setString(3,targetPath);p.setLong(4,assignmentId);p.setLong(5,teacherId);p.executeUpdate();}
    }
    public void notifyAssignedTeacher(String year,long sectionId,long subjectId,String title,String message,String targetPath) throws SQLException {
        String sql="INSERT INTO notifications(user_id,title,message,target_path) SELECT DISTINCT t.user_id,?,?,? FROM teacher_subject ts JOIN teachers t ON t.id=ts.teacher_id WHERE ts.section_id=? AND ts.subject_id=? AND ts.academic_year=?";
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setString(1,title);p.setString(2,message);p.setString(3,targetPath);p.setLong(4,sectionId);p.setLong(5,subjectId);p.setString(6,year);p.executeUpdate();}
    }
    public void notifyPublishedResults(long examId,long actorUserId) throws SQLException {
        String sql="INSERT INTO notifications(user_id,title,message,target_path) SELECT DISTINCT u.id,'Results published',CONCAT(e.exam_name,' results are now available.'),'/student/result' FROM exams e JOIN student_class sc ON sc.section_id=e.section_id AND sc.academic_year=e.academic_year JOIN students s ON s.id=sc.student_id JOIN users u ON u.id=s.user_id JOIN teachers t ON t.user_id=? JOIN teacher_subject ts ON ts.teacher_id=t.id AND ts.section_id=e.section_id AND ts.subject_id=e.subject_id AND ts.academic_year=e.academic_year WHERE e.id=? AND e.results_published_at IS NOT NULL AND u.status=1 AND u.deleted_at IS NULL";
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,actorUserId);p.setLong(2,examId);p.executeUpdate();}
    }
}
