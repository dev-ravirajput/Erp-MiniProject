package com.erp.dao;

import com.erp.model.User;
import com.erp.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistence and access checks for assignment submissions. */
public class AssignmentSubmissionDAO {
    public long studentId(long userId) throws SQLException {
        try (Connection c=DBConnection.getConnection(); PreparedStatement p=c.prepareStatement("SELECT id FROM students WHERE user_id=?")) {
            p.setLong(1,userId); try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Student profile was not found.");return r.getLong(1);}
        }
    }

    public void submit(long userId,long assignmentId,String text,String stored,String original,String type,long size)throws SQLException{
        String scope="SELECT s.id FROM assignments a JOIN students s ON s.user_id=? JOIN users u ON u.id=s.user_id JOIN student_class sc ON sc.student_id=s.id AND sc.section_id=a.section_id AND sc.academic_year=a.academic_year WHERE a.id=? AND a.status=1 AND u.status=1 AND u.deleted_at IS NULL";
        try(Connection c=DBConnection.getConnection()){
            long studentId;
            try(PreparedStatement p=c.prepareStatement(scope)){p.setLong(1,userId);p.setLong(2,assignmentId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("This assignment is not available to your account.");studentId=r.getLong(1);}}
            String sql="INSERT INTO assignment_submissions(assignment_id,student_id,submission_text,stored_filename,original_filename,content_type,file_size) VALUES(?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE submission_text=VALUES(submission_text),stored_filename=COALESCE(VALUES(stored_filename),stored_filename),original_filename=COALESCE(VALUES(original_filename),original_filename),content_type=COALESCE(VALUES(content_type),content_type),file_size=IF(VALUES(stored_filename) IS NULL,file_size,VALUES(file_size)),submitted_at=CURRENT_TIMESTAMP,grade=NULL,feedback=NULL,graded_by=NULL,graded_at=NULL";
            try(PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,assignmentId);p.setLong(2,studentId);p.setString(3,text.isBlank()?null:text);p.setString(4,stored);p.setString(5,original);p.setString(6,type);p.setLong(7,size);p.executeUpdate();}
        }
    }

    public List<Map<String,Object>> teacherSubmissions(long teacherId,long assignmentId)throws SQLException{
        return query("SELECT sub.id,sub.submission_text,sub.stored_filename,sub.original_filename,sub.submitted_at,CASE WHEN DATE(sub.submitted_at)>a.due_date THEN 1 ELSE 0 END submitted_late,sub.grade,sub.feedback,u.name student_name,COALESCE(sc.roll_no,s.roll_no) roll_no,a.max_grade FROM assignments a JOIN assignment_submissions sub ON sub.assignment_id=a.id JOIN students s ON s.id=sub.student_id JOIN users u ON u.id=s.user_id JOIN student_class sc ON sc.student_id=s.id AND sc.section_id=a.section_id AND sc.academic_year=a.academic_year WHERE a.id=? AND a.teacher_id=? ORDER BY u.name",assignmentId,teacherId);
    }

    public void grade(long teacherId,long submissionId,String rawGrade,String feedback)throws SQLException{
        String sql="UPDATE assignment_submissions sub JOIN assignments a ON a.id=sub.assignment_id SET sub.grade=?,sub.feedback=?,sub.graded_by=(SELECT user_id FROM teachers WHERE id=?),sub.graded_at=CURRENT_TIMESTAMP WHERE sub.id=? AND a.teacher_id=? AND (? IS NULL OR (? >= 0 AND ? <= a.max_grade))";
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){
            java.math.BigDecimal grade=rawGrade==null||rawGrade.isBlank()?null:new java.math.BigDecimal(rawGrade);
            if(grade==null)p.setNull(1,java.sql.Types.DECIMAL);else p.setBigDecimal(1,grade);
            p.setString(2,feedback.isBlank()?null:feedback);p.setLong(3,teacherId);p.setLong(4,submissionId);p.setLong(5,teacherId);
            if(grade==null){p.setNull(6,java.sql.Types.DECIMAL);p.setNull(7,java.sql.Types.DECIMAL);p.setNull(8,java.sql.Types.DECIMAL);}else{p.setBigDecimal(6,grade);p.setBigDecimal(7,grade);p.setBigDecimal(8,grade);}
            if(p.executeUpdate()!=1)throw new SQLException("Submission was not found or its grade is outside the assignment range.");
        }catch(NumberFormatException e){throw new SQLException("Enter a valid grade.",e);}
    }

    public Map<String,Object> fileForViewer(String filename,User user)throws SQLException{
        String scope=switch(user.getRole()){
            case STUDENT -> "s.user_id=?";
            case TEACHER -> "a.teacher_id=(SELECT id FROM teachers WHERE user_id=?)";
            case PARENT -> "EXISTS(SELECT 1 FROM guardians g JOIN guardian_student gs ON gs.guardian_id=g.id WHERE g.user_id=? AND gs.student_id=s.id)";
            default -> "1=1";
        };
        String sql="SELECT sub.stored_filename,sub.original_filename,sub.content_type FROM assignment_submissions sub JOIN assignments a ON a.id=sub.assignment_id JOIN students s ON s.id=sub.student_id WHERE sub.stored_filename=? AND "+scope;
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setString(1,filename);if(user.getRole()!=com.erp.model.Role.ADMIN)p.setLong(2,user.getId());try(ResultSet r=p.executeQuery()){if(!r.next())return null;Map<String,Object> row=new LinkedHashMap<>();row.put("stored_filename",r.getString(1));row.put("original_filename",r.getString(2));row.put("content_type",r.getString(3));return row;}}
    }

    private List<Map<String,Object>> query(String sql,Object... values)throws SQLException{List<Map<String,Object>> out=new ArrayList<>();try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){for(int i=0;i<values.length;i++)p.setObject(i+1,values[i]);try(ResultSet r=p.executeQuery()){int n=r.getMetaData().getColumnCount();while(r.next()){Map<String,Object> row=new LinkedHashMap<>();for(int i=1;i<=n;i++)row.put(r.getMetaData().getColumnLabel(i),r.getObject(i));out.add(row);}}}return out;}
}
