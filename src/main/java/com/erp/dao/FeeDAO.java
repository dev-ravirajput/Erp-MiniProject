package com.erp.dao;

import com.erp.util.DBConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Invoice, plan, payment, and refund ledger operations. */
public class FeeDAO {
    public List<Map<String,Object>> plans()throws SQLException{return rows("SELECT p.id,p.class_id,p.academic_year,p.title,p.amount,p.due_date,p.status,c.class_name FROM fee_plans p JOIN classes c ON c.id=p.class_id WHERE p.status=1 ORDER BY p.academic_year DESC,c.class_name,p.title");}
    public List<Map<String,Object>> invoiceStudentOptions()throws SQLException{return rows("SELECT s.id,u.name,s.admission_no,c.id class_id,c.class_name,sc.academic_year FROM students s JOIN users u ON u.id=s.user_id LEFT JOIN student_class sc ON sc.id=(SELECT x.id FROM student_class x WHERE x.student_id=s.id ORDER BY x.academic_year DESC,x.id DESC LIMIT 1) LEFT JOIN sections sec ON sec.id=sc.section_id LEFT JOIN classes c ON c.id=sec.class_id WHERE u.role='STUDENT' AND u.status=1 AND u.deleted_at IS NULL ORDER BY u.name");}
    public void createPlan(long classId,String title,BigDecimal amount,String dueDate,long actor)throws SQLException{
        String sql="INSERT INTO fee_plans(class_id,academic_year,title,amount,due_date,created_by) SELECT id,academic_year,?,?,?,? FROM classes WHERE id=? AND status=1";
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setString(1,title);p.setBigDecimal(2,amount);p.setDate(3,Date.valueOf(dueDate));p.setLong(4,actor);p.setLong(5,classId);if(p.executeUpdate()!=1)throw new SQLException("Selected class is not active.");}
    }
    public List<Map<String,Object>> invoices()throws SQLException{
        String sql="SELECT i.id,i.student_id,i.title,i.academic_year,i.amount,i.concession_amount,i.due_date,i.status,u.name student_name,s.admission_no,COALESCE(pt.paid,0)-COALESCE(rt.refunded,0) paid_amount,COALESCE(rt.refunded,0) refunded_amount,i.amount-i.concession_amount-COALESCE(pt.paid,0)+COALESCE(rt.refunded,0) balance,CASE WHEN i.amount-i.concession_amount-COALESCE(pt.paid,0)+COALESCE(rt.refunded,0)>0 AND i.due_date<CURRENT_DATE() THEN 1 ELSE 0 END overdue FROM fee_invoices i JOIN students s ON s.id=i.student_id JOIN users u ON u.id=s.user_id LEFT JOIN (SELECT invoice_id,SUM(amount) paid FROM fee_payments GROUP BY invoice_id) pt ON pt.invoice_id=i.id LEFT JOIN (SELECT p.invoice_id,SUM(r.amount) refunded FROM fee_payments p JOIN fee_refunds r ON r.payment_id=p.id GROUP BY p.invoice_id) rt ON rt.invoice_id=i.id ORDER BY i.due_date DESC,i.id DESC";
        return rows(sql);
    }
    public List<Map<String,Object>> payments(long invoiceId)throws SQLException{return rows("SELECT p.id,p.amount,p.payment_method,p.reference_no,p.paid_at,p.notes,u.name received_by,COALESCE(r.refunded,0) refunded_amount FROM fee_payments p LEFT JOIN users u ON u.id=p.received_by LEFT JOIN (SELECT payment_id,SUM(amount) refunded FROM fee_refunds GROUP BY payment_id) r ON r.payment_id=p.id WHERE p.invoice_id=? ORDER BY p.paid_at DESC,p.id DESC",invoiceId);}
    public Map<String,Object> paymentReceipt(long paymentId)throws SQLException{
        List<Map<String,Object>> found=rows("SELECT p.id receipt_id,p.amount,p.payment_method,p.reference_no,p.paid_at,p.notes,i.title,i.academic_year,i.amount invoice_amount,i.concession_amount,i.due_date,s.admission_no,u.name student_name,COALESCE(r.refunded,0) refunded_amount,COALESCE(receiver.name,'') received_by FROM fee_payments p JOIN fee_invoices i ON i.id=p.invoice_id JOIN students s ON s.id=i.student_id JOIN users u ON u.id=s.user_id LEFT JOIN users receiver ON receiver.id=p.received_by LEFT JOIN (SELECT payment_id,SUM(amount) refunded FROM fee_refunds GROUP BY payment_id) r ON r.payment_id=p.id WHERE p.id=?",paymentId);
        return found.isEmpty()?null:found.get(0);
    }
    public void createInvoiceFromPlan(long studentId,long planId,BigDecimal concession,long actor)throws SQLException{
        String sql="INSERT INTO fee_invoices(student_id,plan_id,academic_year,title,amount,concession_amount,due_date,created_by) SELECT s.id,p.id,p.academic_year,p.title,p.amount,?,p.due_date,? FROM students s JOIN users u ON u.id=s.user_id JOIN fee_plans p ON p.id=? AND p.status=1 JOIN student_class sc ON sc.student_id=s.id AND sc.academic_year COLLATE utf8mb4_unicode_ci=p.academic_year COLLATE utf8mb4_unicode_ci JOIN sections sec ON sec.id=sc.section_id AND sec.class_id=p.class_id WHERE s.id=? AND u.role='STUDENT' AND u.status=1 AND u.deleted_at IS NULL AND ?<=p.amount";
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setBigDecimal(1,concession);p.setLong(2,actor);p.setLong(3,planId);p.setLong(4,studentId);p.setBigDecimal(5,concession);if(p.executeUpdate()!=1)throw new SQLException("The selected student is not enrolled in the fee plan's class and year, or the concession exceeds the fee.");}
    }
    public void recordPayment(long invoiceId,BigDecimal amount,String method,String reference,String notes,long actor)throws SQLException{
        try(Connection c=DBConnection.getConnection()){c.setAutoCommit(false);try{
            BigDecimal invoiceAmount,concession;String status;
            try(PreparedStatement p=c.prepareStatement("SELECT amount,concession_amount,status FROM fee_invoices WHERE id=? FOR UPDATE")){p.setLong(1,invoiceId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Invoice was not found.");invoiceAmount=r.getBigDecimal("amount");concession=r.getBigDecimal("concession_amount");status=r.getString("status");}}
            if(!"OPEN".equals(status))throw new SQLException("Payments cannot be added to a void invoice.");
            BigDecimal paid=sum(c,"SELECT COALESCE(SUM(amount),0) FROM fee_payments WHERE invoice_id=?",invoiceId),refunded=sum(c,"SELECT COALESCE(SUM(r.amount),0) FROM fee_refunds r JOIN fee_payments p ON p.id=r.payment_id WHERE p.invoice_id=?",invoiceId);
            if(amount.compareTo(invoiceAmount.subtract(concession).subtract(paid).add(refunded))>0)throw new SQLException("Payment exceeds the outstanding balance.");
            try(PreparedStatement p=c.prepareStatement("INSERT INTO fee_payments(invoice_id,amount,payment_method,reference_no,received_by,notes) VALUES(?,?,?,?,?,?)")){p.setLong(1,invoiceId);p.setBigDecimal(2,amount);p.setString(3,method);p.setString(4,reference.isBlank()?null:reference);p.setLong(5,actor);p.setString(6,notes.isBlank()?null:notes);p.executeUpdate();}c.commit();
        }catch(Exception e){c.rollback();if(e instanceof SQLException se)throw se;throw new SQLException("Payment could not be recorded.",e);}}
    }
    public void refund(long invoiceId,long paymentId,BigDecimal amount,String reference,String notes,long actor)throws SQLException{
        try(Connection c=DBConnection.getConnection()){c.setAutoCommit(false);try{BigDecimal paid;try(PreparedStatement p=c.prepareStatement("SELECT amount FROM fee_payments WHERE id=? AND invoice_id=? FOR UPDATE")){p.setLong(1,paymentId);p.setLong(2,invoiceId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Payment was not found for this invoice.");paid=r.getBigDecimal(1);}}
            BigDecimal refunded=sum(c,"SELECT COALESCE(SUM(amount),0) FROM fee_refunds WHERE payment_id=?",paymentId);if(amount.compareTo(paid.subtract(refunded))>0)throw new SQLException("Refund exceeds the unrefunded payment amount.");
            try(PreparedStatement p=c.prepareStatement("INSERT INTO fee_refunds(payment_id,amount,reference_no,processed_by,notes) VALUES(?,?,?,?,?)")){p.setLong(1,paymentId);p.setBigDecimal(2,amount);p.setString(3,reference.isBlank()?null:reference);p.setLong(4,actor);p.setString(5,notes.isBlank()?null:notes);p.executeUpdate();}c.commit();
        }catch(Exception e){c.rollback();if(e instanceof SQLException se)throw se;throw new SQLException("Refund could not be recorded.",e);}}
    }
    public Map<String,Object> invoice(long id)throws SQLException{List<Map<String,Object>> rows=rows("SELECT id,amount,status FROM fee_invoices WHERE id=?",id);return rows.isEmpty()?null:rows.get(0);}
    private BigDecimal sum(Connection c,String sql,long id)throws SQLException{try(PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,id);try(ResultSet r=p.executeQuery()){r.next();return r.getBigDecimal(1);}}}
    private List<Map<String,Object>> rows(String sql,Object... values)throws SQLException{List<Map<String,Object>> out=new ArrayList<>();try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){for(int i=0;i<values.length;i++)p.setObject(i+1,values[i]);try(ResultSet r=p.executeQuery()){int n=r.getMetaData().getColumnCount();while(r.next()){Map<String,Object> m=new LinkedHashMap<>();for(int i=1;i<=n;i++)m.put(r.getMetaData().getColumnLabel(i),r.getObject(i));out.add(m);}}}return out;}
}
