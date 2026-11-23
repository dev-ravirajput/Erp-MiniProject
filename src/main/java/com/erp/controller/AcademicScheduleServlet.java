package com.erp.controller;

import com.erp.dao.AcademicScheduleDAO;
import com.erp.dao.NotificationDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
 
/** Shared controller for admin exam schedules and weekly section timetables. */
public class AcademicScheduleServlet extends HttpServlet {
    private final AcademicScheduleDAO dao=new AcademicScheduleDAO();
    private final NotificationDAO notificationDAO=new NotificationDAO();
    private boolean exams(HttpServletRequest req){return req.getRequestURI().endsWith("/examinations");}
    private String route(HttpServletRequest req){return exams(req)?"examinations":"timetable";}

    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        req.setCharacterEncoding("UTF-8");boolean exam=exams(req);String route=route(req);req.setAttribute("activeAdminSection",route);
        String action=req.getParameter("action");
        if("create".equals(action)){forwardForm(req,resp,exam,"create",new LinkedHashMap<>(),null);return;}
        if("edit".equals(action)){Long id=parseId(req.getParameter("id"));if(id==null){resp.sendError(400);return;}try{Map<String,Object> row=exam?dao.getExam(id):dao.getTimetableEntry(id);if(row==null){resp.sendError(404);return;}forwardForm(req,resp,exam,"update",row,null);}catch(SQLException e){throw new ServletException(e);}return;}
        String section=req.getParameter("section_id");req.setAttribute("sectionFilter",section==null?"":section);
        try{req.setAttribute(exam?"exams":"entries",exam?dao.getExams(section):dao.getTimetable(section));}
        catch(Exception e){getServletContext().log("Could not load "+route+" schedules",e);req.setAttribute("loadError","Schedules could not be loaded. Check that database migration 004 has been applied.");req.setAttribute(exam?"exams":"entries",java.util.Collections.emptyList());}
        try{req.setAttribute("sectionOptions",dao.options("sections"));}catch(SQLException e){getServletContext().log("Could not load section options",e);req.setAttribute("loadError","Class and section options could not be loaded. Check the database setup.");}
        req.getRequestDispatcher(exam?"/admin/examinations.jsp":"/admin/timetable.jsp").forward(req,resp);
    }

    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        req.setCharacterEncoding("UTF-8");boolean exam=exams(req);String route=route(req),base=req.getContextPath()+"/admin/"+route,action=req.getParameter("action");
        if("delete".equals(action)){Long id=parseId(req.getParameter("id"));if(id==null){resp.sendError(400);return;}try{if(exam)dao.deleteExam(id);else dao.deleteTimetable(id);resp.sendRedirect(base+"?notice=deleted");}catch(SQLException e){getServletContext().log("Could not delete schedule",e);resp.sendRedirect(base+"?notice=error");}return;}
        if(!"create".equals(action)&&!"update".equals(action)){resp.sendError(400);return;}boolean update="update".equals(action);Long id=update?parseId(req.getParameter("id")):null;if(update&&id==null){resp.sendError(400);return;}
        String[] fields=exam?new String[]{"exam_name","academic_year","section_id","subject_id","exam_date","start_time","end_time","room"}:new String[]{"academic_year","section_id","subject_id","teacher_id","day_of_week","start_time","end_time","room"};
        Map<String,String> values=new LinkedHashMap<>();for(String f:fields){String v=req.getParameter(f);values.put(f,v==null?"":v.trim());}
        String error=validate(values,exam);
        if(error!=null){forwardForm(req,resp,exam,update?"update":"create",values,error);return;}
        try{if(exam){dao.saveExam(values,id);String examName=values.get("exam_name"),year=values.get("academic_year"),date=values.get("exam_date"),message=examName+" is scheduled for "+date+".";try{long section=Long.parseLong(values.get("section_id")),subject=Long.parseLong(values.get("subject_id"));notificationDAO.notifySectionStudents(year,section,"Exam scheduled",message,"/student/dashboard");notificationDAO.notifyAssignedTeacher(year,section,subject,"Exam scheduled",message,"/teacher/marks");}catch(SQLException notificationError){getServletContext().log("Exam schedule saved, but notifications could not be created",notificationError);}}else dao.saveTimetable(values,id);resp.sendRedirect(base+"?notice="+(update?"updated":"created"));}
        catch(SQLException e){getServletContext().log("Could not save schedule",e);String msg=e.getSQLState()!=null&&e.getSQLState().startsWith("23")?"A conflicting schedule already exists. Check the date, period, class, and selected records.":e.getMessage();forwardForm(req,resp,exam,update?"update":"create",values,msg==null?"The schedule could not be saved.":msg);}
    }

    private String validate(Map<String,String> v,boolean exam){
        for(String f:new String[]{"section_id","subject_id"})try{if(Long.parseLong(v.get(f))<=0)return "Select a class section and subject.";}catch(Exception e){return "Select a class section and subject.";}
        if(!exam)try{if(Long.parseLong(v.get("teacher_id"))<=0)return "Select a teacher.";}catch(Exception e){return "Select a teacher.";}
        if(v.get("academic_year").isEmpty()||v.get("academic_year").length()>20)return "Enter a valid academic year.";
        if(exam&&(v.get("exam_name").isEmpty()||v.get("exam_name").length()>100))return "Enter an exam name (up to 100 characters).";
        if(v.get("room").length()>50)return "Room must be 50 characters or fewer.";
        try{if(exam)LocalDate.parse(v.get("exam_date"));LocalTime start=LocalTime.parse(v.get("start_time")),end=LocalTime.parse(v.get("end_time"));if(!end.isAfter(start))return "End time must be after start time.";}catch(Exception e){return "Enter a valid date and start/end time.";}
        if(!exam&&!java.util.Arrays.asList("MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY").contains(v.get("day_of_week")))return "Select a valid weekday.";
        return null;
    }

    private void forwardForm(HttpServletRequest req,HttpServletResponse resp,boolean exam,String mode,Map<String,?> value,String error)throws ServletException,IOException{
        req.setAttribute("activeAdminSection",exam?"examinations":"timetable");req.setAttribute("mode",mode);req.setAttribute("schedule",value);req.setAttribute("formError",error);
        try{req.setAttribute("sectionOptions",dao.options("sections"));req.setAttribute("subjectOptions",dao.options("subjects"));if(!exam)req.setAttribute("teacherOptions",dao.options("teachers"));}
        catch(SQLException e){getServletContext().log("Schedule form options could not be loaded",e);req.setAttribute("formError","Class, section, subject, and teacher options could not be loaded. Check database migrations.");}
        req.getRequestDispatcher(exam?"/admin/examination-form.jsp":"/admin/timetable-form.jsp").forward(req,resp);
    }
    private Long parseId(String raw){try{return raw==null?null:Long.valueOf(raw);}catch(NumberFormatException e){return null;}}
}
