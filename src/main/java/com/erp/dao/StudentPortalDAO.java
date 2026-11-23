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

/** Read-only data for the signed-in student's portal. */
public class StudentPortalDAO {
    public Map<String,Object> getProfile(long userId) throws SQLException {
        String sql="SELECT u.id,u.name,u.email,u.status,s.admission_no,COALESCE(sc.roll_no,s.roll_no) roll_no,s.phone,s.dob,s.gender,s.address,s.admission_date,sc.academic_year,c.class_name,sec.section_name FROM users u LEFT JOIN students s ON s.user_id=u.id LEFT JOIN student_class sc ON sc.id=(SELECT sc2.id FROM student_class sc2 WHERE sc2.student_id=s.id ORDER BY sc2.academic_year DESC,sc2.id DESC LIMIT 1) LEFT JOIN sections sec ON sec.id=sc.section_id LEFT JOIN classes c ON c.id=sec.class_id WHERE u.id=? AND u.role='STUDENT' AND u.deleted_at IS NULL";
        try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement(sql)){ps.setLong(1,userId);try(ResultSet rs=ps.executeQuery()){if(!rs.next())return null;Map<String,Object> row=new LinkedHashMap<>();for(String key:new String[]{"id","name","email","status","admission_no","roll_no","phone","dob","gender","address","admission_date","academic_year","class_name","section_name"})row.put(key,rs.getObject(key));return row;}}
    }
    public List<Map<String,Object>> getAttendance(long userId)throws SQLException{
        String sql="SELECT a.attendance_date,a.status,a.remarks,sub.subject_name,c.class_name,sec.section_name FROM attendance a JOIN students s ON s.id=a.student_id JOIN student_class sc ON sc.student_id=s.id AND sc.section_id=a.section_id JOIN sections sec ON sec.id=a.section_id JOIN classes c ON c.id=sec.class_id JOIN subjects sub ON sub.id=a.subject_id JOIN users u ON u.id=s.user_id WHERE u.id=? AND u.role='STUDENT' AND u.deleted_at IS NULL ORDER BY a.attendance_date DESC,a.id DESC";
        return query(userId,sql,new String[]{"attendance_date","status","remarks","subject_name","class_name","section_name"});
    }
    public List<Map<String,Object>> getAssignments(long userId)throws SQLException{
        String sql="SELECT a.id,a.title,a.description,a.assigned_date,a.due_date,a.max_grade,sub.subject_name,c.class_name,sec.section_name,tu.name teacher_name,subm.submitted_at,CASE WHEN DATE(subm.submitted_at)>a.due_date THEN 1 ELSE 0 END submitted_late,subm.submission_text,subm.stored_filename,subm.original_filename,subm.grade,subm.feedback FROM assignments a JOIN users u ON u.id=? AND u.role='STUDENT' AND u.deleted_at IS NULL JOIN students s ON s.user_id=u.id JOIN student_class sc ON sc.student_id=s.id AND sc.academic_year=a.academic_year AND sc.section_id=a.section_id JOIN sections sec ON sec.id=a.section_id JOIN classes c ON c.id=sec.class_id JOIN subjects sub ON sub.id=a.subject_id JOIN teachers t ON t.id=a.teacher_id JOIN users tu ON tu.id=t.user_id LEFT JOIN assignment_submissions subm ON subm.assignment_id=a.id AND subm.student_id=s.id WHERE a.status=1 ORDER BY a.due_date,a.id DESC";
        return query(userId,sql,new String[]{"id","title","description","assigned_date","due_date","max_grade","subject_name","class_name","section_name","teacher_name","submitted_at","submitted_late","submission_text","stored_filename","original_filename","grade","feedback"});
    }
    public List<Map<String,Object>> getMarks(long userId)throws SQLException{
        String sql="SELECT e.exam_name,e.academic_year,e.exam_date,sub.subject_name,sub.max_marks,m.marks,m.remarks,(SELECT gs.grade_label FROM grade_scales gs WHERE gs.status=1 AND gs.min_percent<=100*m.marks/sub.max_marks ORDER BY gs.min_percent DESC LIMIT 1) grade FROM marks m JOIN students s ON s.id=m.student_id JOIN users u ON u.id=s.user_id JOIN exams e ON e.id=m.exam_id AND e.results_published_at IS NOT NULL AND e.section_id=(SELECT sc.section_id FROM student_class sc WHERE sc.student_id=s.id AND sc.academic_year=e.academic_year ORDER BY sc.id DESC LIMIT 1) JOIN subjects sub ON sub.id=m.subject_id AND sub.id=e.subject_id WHERE u.id=? AND u.role='STUDENT' AND u.deleted_at IS NULL ORDER BY e.exam_date DESC,e.exam_name,sub.subject_name";
        return query(userId,sql,new String[]{"exam_name","academic_year","exam_date","subject_name","max_marks","marks","remarks","grade"});
    }
    public List<Map<String,Object>> getInvoices(long userId)throws SQLException{
        String sql="SELECT i.id,i.title,i.academic_year,i.amount,i.concession_amount,i.due_date,i.status,COALESCE(pt.paid,0)-COALESCE(rt.refunded,0) paid_amount,i.amount-i.concession_amount-COALESCE(pt.paid,0)+COALESCE(rt.refunded,0) balance,CASE WHEN i.amount-i.concession_amount-COALESCE(pt.paid,0)+COALESCE(rt.refunded,0)>0 AND i.due_date<CURRENT_DATE() THEN 1 ELSE 0 END overdue FROM fee_invoices i JOIN students s ON s.id=i.student_id JOIN users u ON u.id=s.user_id LEFT JOIN (SELECT invoice_id,SUM(amount) paid FROM fee_payments GROUP BY invoice_id) pt ON pt.invoice_id=i.id LEFT JOIN (SELECT p.invoice_id,SUM(r.amount) refunded FROM fee_payments p JOIN fee_refunds r ON r.payment_id=p.id GROUP BY p.invoice_id) rt ON rt.invoice_id=i.id WHERE u.id=? AND u.role='STUDENT' AND u.deleted_at IS NULL ORDER BY i.due_date DESC";
        return query(userId,sql,new String[]{"id","title","academic_year","amount","concession_amount","due_date","status","paid_amount","balance","overdue"});
    }
    private List<Map<String,Object>> query(long userId,String sql,String[] columns)throws SQLException{
        List<Map<String,Object>> rows=new ArrayList<>();try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement(sql)){ps.setLong(1,userId);try(ResultSet rs=ps.executeQuery()){while(rs.next()){Map<String,Object> row=new LinkedHashMap<>();for(String column:columns)row.put(column,rs.getObject(column));rows.add(row);}}}return rows;
    }
}
