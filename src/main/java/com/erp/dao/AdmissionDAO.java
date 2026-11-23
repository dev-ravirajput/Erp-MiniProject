package com.erp.dao;

import com.erp.util.DBConnection;
import com.erp.util.PasswordUtil;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Admissions intake, review, and conversion to student enrollment. */
public class AdmissionDAO {
    public List<Map<String,Object>> list()throws SQLException{return rows("SELECT a.*,c.class_name FROM admission_applications a LEFT JOIN classes c ON c.id=a.requested_class_id ORDER BY a.created_at DESC,a.id DESC");}
    public void create(String appNo,String name,String email,String phone,String dob,Long classId,String year,String notes,long actor)throws SQLException{
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO admission_applications(application_no,applicant_name,applicant_email,applicant_phone,date_of_birth,requested_class_id,academic_year,notes,created_by) VALUES(?,?,?,?,?,?,?,?,?)")){
            p.setString(1,appNo);p.setString(2,name);p.setString(3,email.isBlank()?null:email);p.setString(4,phone.isBlank()?null:phone);if(dob.isBlank())p.setNull(5,java.sql.Types.DATE);else p.setDate(5,Date.valueOf(dob));if(classId==null)p.setNull(6,java.sql.Types.BIGINT);else p.setLong(6,classId);p.setString(7,year);p.setString(8,notes.isBlank()?null:notes);p.setLong(9,actor);p.executeUpdate();
        }
    }
    public void updateStatus(long id,String status)throws SQLException{
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("UPDATE admission_applications SET status=? WHERE id=? AND status<>'ENROLLED'")){p.setString(1,status);p.setLong(2,id);if(p.executeUpdate()!=1)throw new SQLException("Application was not found or has already been enrolled.");}
    }
    public void enroll(long applicationId,String admissionNo,String rollNo,String phone,String password,long sectionId,String year)throws SQLException{
        try(Connection c=DBConnection.getConnection()){c.setAutoCommit(false);try{
            String name,email;java.sql.Date dob;
            Long requestedClassId;
            try(PreparedStatement p=c.prepareStatement("SELECT applicant_name,applicant_email,date_of_birth,status,requested_class_id FROM admission_applications WHERE id=? FOR UPDATE")){p.setLong(1,applicationId);try(ResultSet r=p.executeQuery()){if(!r.next()||!"ACCEPTED".equals(r.getString("status")))throw new SQLException("Only accepted applications can be enrolled.");name=r.getString("applicant_name");email=r.getString("applicant_email");dob=r.getDate("date_of_birth");long requested=r.getLong("requested_class_id");requestedClassId=r.wasNull()?null:requested;}}
            if(email==null||email.isBlank())throw new SQLException("Add an email address to the application before enrollment.");
            try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM sections s JOIN classes cl ON cl.id=s.class_id WHERE s.id=? AND cl.academic_year=? AND cl.status=1 AND (? IS NULL OR cl.id=?)")){p.setLong(1,sectionId);p.setString(2,year);if(requestedClassId==null)p.setNull(3,java.sql.Types.BIGINT);else p.setLong(3,requestedClassId);if(requestedClassId==null)p.setNull(4,java.sql.Types.BIGINT);else p.setLong(4,requestedClassId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Selected section does not match the requested class and academic year.");}}
            long userId;try(PreparedStatement p=c.prepareStatement("INSERT INTO users(name,email,password,role) VALUES(?,?,?,'STUDENT')",PreparedStatement.RETURN_GENERATED_KEYS)){p.setString(1,name);p.setString(2,email);p.setString(3,PasswordUtil.sha256(password));p.executeUpdate();try(ResultSet r=p.getGeneratedKeys()){r.next();userId=r.getLong(1);}}
            long studentId;try(PreparedStatement p=c.prepareStatement("INSERT INTO students(user_id,admission_no,roll_no,phone,dob,admission_date) VALUES(?,?,?,?,?,CURRENT_DATE())",PreparedStatement.RETURN_GENERATED_KEYS)){p.setLong(1,userId);p.setString(2,admissionNo);p.setString(3,rollNo.isBlank()?null:rollNo);p.setString(4,phone.isBlank()?null:phone);if(dob==null)p.setNull(5,java.sql.Types.DATE);else p.setDate(5,dob);p.executeUpdate();try(ResultSet r=p.getGeneratedKeys()){r.next();studentId=r.getLong(1);}}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO student_class(student_id,section_id,roll_no,academic_year) VALUES(?,?,?,?)")){p.setLong(1,studentId);p.setLong(2,sectionId);p.setString(3,rollNo.isBlank()?null:rollNo);p.setString(4,year);p.executeUpdate();}
            try(PreparedStatement p=c.prepareStatement("UPDATE admission_applications SET status='ENROLLED',converted_student_id=? WHERE id=?")){p.setLong(1,studentId);p.setLong(2,applicationId);p.executeUpdate();}c.commit();
        }catch(Exception e){c.rollback();if(e instanceof SQLException se)throw se;throw new SQLException("Application enrollment failed.",e);}}
    }
    private List<Map<String,Object>> rows(String sql,Object... args)throws SQLException{List<Map<String,Object>> out=new ArrayList<>();try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);try(ResultSet r=p.executeQuery()){int n=r.getMetaData().getColumnCount();while(r.next()){Map<String,Object> m=new LinkedHashMap<>();for(int i=1;i<=n;i++)m.put(r.getMetaData().getColumnLabel(i),r.getObject(i));out.add(m);}}}return out;}
}
