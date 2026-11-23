package com.erp.dao;

import com.erp.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Bulk enrollment rollover; prior academic-year records remain intact. */
public class PromotionDAO {
    public int promote(String[] studentIds,long sectionId,String[] subjectIds)throws SQLException{
        if(studentIds==null||studentIds.length==0)throw new SQLException("Select at least one student.");
        try(Connection c=DBConnection.getConnection()){c.setAutoCommit(false);try{
            long classId;String year;
            try(PreparedStatement p=c.prepareStatement("SELECT c.id,c.academic_year FROM sections s JOIN classes c ON c.id=s.class_id WHERE s.id=? AND c.status=1")){p.setLong(1,sectionId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Choose an active target section.");classId=r.getLong(1);year=r.getString(2);}}
            if(subjectIds==null||subjectIds.length==0)throw new SQLException("Choose at least one subject for the new enrollment.");
            try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM subjects WHERE id=? AND status=1")){for(String raw:subjectIds){long subject=Long.parseLong(raw);p.setLong(1,subject);try(ResultSet r=p.executeQuery()){r.next();if(r.getInt(1)!=1)throw new SQLException("One selected subject is not active.");}}}
            int changed=0;
            for(String raw:studentIds){long student=Long.parseLong(raw);
                try(PreparedStatement p=c.prepareStatement("INSERT INTO student_class(student_id,section_id,roll_no,academic_year) SELECT s.id,?,s.roll_no,? FROM students s JOIN users u ON u.id=s.user_id WHERE s.id=? AND u.role='STUDENT' AND u.status=1 AND u.deleted_at IS NULL ON DUPLICATE KEY UPDATE section_id=VALUES(section_id),roll_no=VALUES(roll_no)")){p.setLong(1,sectionId);p.setString(2,year);p.setLong(3,student);p.executeUpdate();try(PreparedStatement check=c.prepareStatement("SELECT 1 FROM students s JOIN users u ON u.id=s.user_id WHERE s.id=? AND u.status=1 AND u.deleted_at IS NULL")){check.setLong(1,student);try(ResultSet r=check.executeQuery()){if(!r.next())throw new SQLException("A selected student is inactive or unavailable.");}}}
                long enrollmentId;try(PreparedStatement p=c.prepareStatement("SELECT id FROM student_class WHERE student_id=? AND academic_year=?")){p.setLong(1,student);p.setString(2,year);try(ResultSet r=p.executeQuery()){r.next();enrollmentId=r.getLong(1);}}
                try(PreparedStatement p=c.prepareStatement("DELETE FROM student_subject WHERE student_class_id=?")){p.setLong(1,enrollmentId);p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement("INSERT INTO student_subject(student_class_id,subject_id) VALUES(?,?)")){for(String subject:subjectIds){p.setLong(1,enrollmentId);p.setLong(2,Long.parseLong(subject));p.addBatch();}p.executeBatch();}
                changed++;
            }
            c.commit();return changed;
        }catch(Exception e){c.rollback();if(e instanceof SQLException se)throw se;throw new SQLException("Enrollment rollover failed.",e);}}
    }
}
