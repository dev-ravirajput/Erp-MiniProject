package com.erp.dao;

import com.erp.util.DBConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistence and option lookups for admin examination and timetable scheduling. */
public class AcademicScheduleDAO {
    private static final String[] DAYS={"MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"};

    public List<Map<String,Object>> options(String kind) throws SQLException {
        String sql;
        if("sections".equals(kind)) sql="SELECT s.id,s.section_name,c.id class_id,c.class_name,c.academic_year FROM sections s JOIN classes c ON c.id=s.class_id WHERE c.status=1 ORDER BY c.academic_year DESC,c.class_name,s.section_name";
        else if("subjects".equals(kind)) sql="SELECT id,subject_code,subject_name FROM subjects WHERE status=1 ORDER BY subject_name";
        else if("teachers".equals(kind)) sql="SELECT t.id,u.name,u.email FROM teachers t JOIN users u ON u.id=t.user_id WHERE u.role='TEACHER' AND u.status=1 AND u.deleted_at IS NULL ORDER BY u.name";
        else throw new IllegalArgumentException("Unknown option type");
        List<Map<String,Object>> rows=new ArrayList<>();
        try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement(sql);ResultSet rs=ps.executeQuery()) {
            while(rs.next()){Map<String,Object> row=new LinkedHashMap<>();row.put("id",rs.getLong("id"));
                if("sections".equals(kind)){row.put("section_name",rs.getString("section_name"));row.put("class_id",rs.getLong("class_id"));row.put("class_name",rs.getString("class_name"));row.put("academic_year",rs.getString("academic_year"));}
                else if("subjects".equals(kind)){row.put("subject_code",rs.getString("subject_code"));row.put("subject_name",rs.getString("subject_name"));}
                else {row.put("name",rs.getString("name"));row.put("email",rs.getString("email"));} rows.add(row);
            }
        }return rows;
    }

    public List<Map<String,Object>> getExams(String sectionFilter) throws SQLException {
        String sql="SELECT e.id,e.exam_name,e.academic_year,e.exam_date,e.start_time,e.end_time,e.room,e.status,"
                +"s.section_name,c.class_name,sub.subject_name,sub.subject_code FROM exams e JOIN sections s ON s.id=e.section_id "
                +"JOIN classes c ON c.id=s.class_id JOIN subjects sub ON sub.id=e.subject_id WHERE e.status=1";
        if(sectionFilter!=null&&!sectionFilter.isEmpty())sql+=" AND e.section_id=?";
        sql+=" ORDER BY e.exam_date,e.start_time,c.class_name,s.section_name,sub.subject_name";
        List<Map<String,Object>> rows=new ArrayList<>();try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement(sql)){
            if(sectionFilter!=null&&!sectionFilter.isEmpty())ps.setLong(1,Long.parseLong(sectionFilter));
            try(ResultSet rs=ps.executeQuery()){while(rs.next()){Map<String,Object> row=new LinkedHashMap<>();for(String k:new String[]{"id","status"})row.put(k,rs.getObject(k));for(String k:new String[]{"exam_name","academic_year","exam_date","start_time","end_time","room","section_name","class_name","subject_name","subject_code"})row.put(k,rs.getObject(k));rows.add(row);}}
        }return rows;
    }

    public Map<String,Object> getExam(long id)throws SQLException{
        try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement("SELECT id,exam_name,academic_year,section_id,subject_id,exam_date,start_time,end_time,room FROM exams WHERE id=? AND status=1")){
            ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){if(!rs.next())return null;Map<String,Object> r=new LinkedHashMap<>();for(String k:new String[]{"id","section_id","subject_id"})r.put(k,rs.getLong(k));for(String k:new String[]{"exam_name","academic_year","exam_date","start_time","end_time","room"})r.put(k,rs.getObject(k));return r;}
        }
    }

    public void saveExam(Map<String,String> r,Long id)throws SQLException{
        long section=number(r,"section_id"),subject=number(r,"subject_id");String year=r.get("academic_year");Time start=Time.valueOf(r.get("start_time")+":00"),end=Time.valueOf(r.get("end_time")+":00");
        if(!end.after(start))throw new SQLException("Exam end time must be after its start time.");validateSectionYear(section,year);
        String overlap="SELECT 1 FROM exams WHERE status=1 AND section_id=? AND exam_date=? AND start_time < ? AND end_time > ?"+(id==null?"":" AND id<>?");
        try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement(overlap)){ps.setLong(1,section);ps.setDate(2,Date.valueOf(r.get("exam_date")));ps.setTime(3,end);ps.setTime(4,start);if(id!=null)ps.setLong(5,id);try(ResultSet rs=ps.executeQuery()){if(rs.next())throw new SQLException("This section already has an exam scheduled at an overlapping time.");}}
        String sql=id==null?"INSERT INTO exams(exam_name,academic_year,section_id,subject_id,exam_date,start_time,end_time,room) VALUES(?,?,?,?,?,?,?,?)":"UPDATE exams SET exam_name=?,academic_year=?,section_id=?,subject_id=?,exam_date=?,start_time=?,end_time=?,room=? WHERE id=?";
        try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement(sql)){bindSchedule(ps,r,section,subject,start,end);if(id!=null)ps.setLong(9,id);ps.executeUpdate();}
    }

    private void bindSchedule(PreparedStatement ps,Map<String,String> r,long section,long subject,Time start,Time end)throws SQLException{
        ps.setString(1,r.get("exam_name"));ps.setString(2,r.get("academic_year"));ps.setLong(3,section);ps.setLong(4,subject);ps.setDate(5,Date.valueOf(r.get("exam_date")));ps.setTime(6,start);ps.setTime(7,end);if(r.get("room").isEmpty())ps.setNull(8,java.sql.Types.VARCHAR);else ps.setString(8,r.get("room"));
    }

    public void deleteExam(long id)throws SQLException{try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement("UPDATE exams SET status=0 WHERE id=? AND status=1")){ps.setLong(1,id);if(ps.executeUpdate()!=1)throw new SQLException("Exam schedule was not found.");}}

    public List<Map<String,Object>> getTimetable(String sectionFilter)throws SQLException{
        String sql="SELECT tt.id,tt.academic_year,tt.day_of_week,tt.start_time,tt.end_time,tt.room,s.section_name,c.class_name,sub.subject_code,sub.subject_name,u.name teacher_name "
                +"FROM timetable tt JOIN sections s ON s.id=tt.section_id JOIN classes c ON c.id=s.class_id JOIN subjects sub ON sub.id=tt.subject_id JOIN teachers t ON t.id=tt.teacher_id JOIN users u ON u.id=t.user_id WHERE tt.status=1";
        if(sectionFilter!=null&&!sectionFilter.isEmpty())sql+=" AND tt.section_id=?";sql+=" ORDER BY c.class_name,s.section_name,FIELD(tt.day_of_week,'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'),tt.start_time";
        List<Map<String,Object>> rows=new ArrayList<>();try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement(sql)){if(sectionFilter!=null&&!sectionFilter.isEmpty())ps.setLong(1,Long.parseLong(sectionFilter));try(ResultSet rs=ps.executeQuery()){while(rs.next()){Map<String,Object> r=new LinkedHashMap<>();r.put("id",rs.getLong("id"));for(String k:new String[]{"academic_year","day_of_week","start_time","end_time","room","section_name","class_name","subject_code","subject_name","teacher_name"})r.put(k,rs.getObject(k));rows.add(r);}}}return rows;
    }

    public Map<String,Object> getTimetableEntry(long id)throws SQLException{try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement("SELECT id,section_id,subject_id,teacher_id,academic_year,day_of_week,start_time,end_time,room FROM timetable WHERE id=? AND status=1")){ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){if(!rs.next())return null;Map<String,Object> r=new LinkedHashMap<>();for(String k:new String[]{"id","section_id","subject_id","teacher_id"})r.put(k,rs.getLong(k));for(String k:new String[]{"academic_year","day_of_week","start_time","end_time","room"})r.put(k,rs.getObject(k));return r;}}}

    public void saveTimetable(Map<String,String> r,Long id)throws SQLException{
        long section=number(r,"section_id"),subject=number(r,"subject_id"),teacher=number(r,"teacher_id");String year=r.get("academic_year"),day=r.get("day_of_week");if(!List.of(DAYS).contains(day))throw new SQLException("Choose a valid weekday.");
        Time start=Time.valueOf(r.get("start_time")+":00"),end=Time.valueOf(r.get("end_time")+":00");if(!end.after(start))throw new SQLException("Period end time must be after its start time.");validateSectionYear(section,year);
        try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement("SELECT 1 FROM teacher_subject WHERE teacher_id=? AND subject_id=? AND section_id=? AND academic_year=?")){ps.setLong(1,teacher);ps.setLong(2,subject);ps.setLong(3,section);ps.setString(4,year);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new SQLException("The selected teacher is not assigned to this subject and section for this academic year.");}}
        checkTimetableConflict("section_id",section,day,year,start,end,id);checkTimetableConflict("teacher_id",teacher,day,year,start,end,id);
        if(r.get("room")!=null&&!r.get("room").isEmpty())checkRoomConflict(r.get("room"),day,year,start,end,id);
        String sql=id==null?"INSERT INTO timetable(section_id,subject_id,teacher_id,academic_year,day_of_week,start_time,end_time,room) VALUES(?,?,?,?,?,?,?,?)":"UPDATE timetable SET section_id=?,subject_id=?,teacher_id=?,academic_year=?,day_of_week=?,start_time=?,end_time=?,room=? WHERE id=?";
        try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement(sql)){ps.setLong(1,section);ps.setLong(2,subject);ps.setLong(3,teacher);ps.setString(4,year);ps.setString(5,day);ps.setTime(6,start);ps.setTime(7,end);if(r.get("room").isEmpty())ps.setNull(8,java.sql.Types.VARCHAR);else ps.setString(8,r.get("room"));if(id!=null)ps.setLong(9,id);ps.executeUpdate();}
    }

    private void checkTimetableConflict(String column,long value,String day,String year,Time start,Time end,Long id)throws SQLException{
        String sql="SELECT 1 FROM timetable WHERE status=1 AND "+column+"=? AND day_of_week=? AND academic_year=? AND start_time < ? AND end_time > ?"+(id==null?"":" AND id<>?");
        try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement(sql)){ps.setLong(1,value);ps.setString(2,day);ps.setString(3,year);ps.setTime(4,end);ps.setTime(5,start);if(id!=null)ps.setLong(6,id);try(ResultSet rs=ps.executeQuery()){if(rs.next())throw new SQLException("A section or teacher already has a timetable period at an overlapping time.");}}
    }

    private void checkRoomConflict(String room,String day,String year,Time start,Time end,Long id)throws SQLException{
        String sql="SELECT 1 FROM timetable WHERE status=1 AND room=? AND day_of_week=? AND academic_year=? AND start_time < ? AND end_time > ?"+(id==null?"":" AND id<>?");
        try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement(sql)){ps.setString(1,room);ps.setString(2,day);ps.setString(3,year);ps.setTime(4,end);ps.setTime(5,start);if(id!=null)ps.setLong(6,id);try(ResultSet rs=ps.executeQuery()){if(rs.next())throw new SQLException("This room is already reserved for an overlapping timetable period.");}}
    }

    public void deleteTimetable(long id)throws SQLException{try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement("UPDATE timetable SET status=0 WHERE id=? AND status=1")){ps.setLong(1,id);if(ps.executeUpdate()!=1)throw new SQLException("Timetable entry was not found.");}}

    private void validateSectionYear(long section,String year)throws SQLException{try(Connection con=DBConnection.getConnection();PreparedStatement ps=con.prepareStatement("SELECT 1 FROM sections s JOIN classes c ON c.id=s.class_id WHERE s.id=? AND c.status=1 AND c.academic_year=?")){ps.setLong(1,section);ps.setString(2,year);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new SQLException("Selected section does not match the active class and academic year.");}}}
    private long number(Map<String,String> values,String key)throws SQLException{try{return Long.parseLong(values.get(key));}catch(Exception e){throw new SQLException("A required class, subject, or teacher selection is missing.",e);}}
}
