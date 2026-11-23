package com.erp.controller;

import com.erp.dao.AttendanceDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class AttendanceServlet extends HttpServlet {
    private final AttendanceDAO dao = new AttendanceDAO();

    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
        req.setCharacterEncoding("UTF-8"); req.setAttribute("activeAdminSection","attendance");
        String action=req.getParameter("action");
        if("create".equals(action)){forwardForm(req,resp,"create",new LinkedHashMap<>(),null);return;}
        if("edit".equals(action)){Long id=parseId(req.getParameter("id")); if(id==null){resp.sendError(400);return;} try{Map<String,Object> row=dao.getRecord(id);if(row==null){resp.sendError(404);return;}forwardForm(req,resp,"update",row,null);}catch(SQLException e){throw new ServletException(e);}return;}
        String date=req.getParameter("date"),status=req.getParameter("status");
        if(date==null)date=""; if(status==null)status="";
        try{if(!date.isEmpty())LocalDate.parse(date); if(!status.isEmpty()&&!validStatus(status))status=""; req.setAttribute("records",dao.getRecords(date,status));}
        catch(Exception e){getServletContext().log("Could not load attendance records",e);req.setAttribute("loadError","Attendance could not be loaded. Check the database setup and date filter.");req.setAttribute("records",java.util.Collections.emptyList());}
        req.setAttribute("dateFilter",date);req.setAttribute("statusFilter",status);req.getRequestDispatcher("/admin/attendance.jsp").forward(req,resp);
    }

    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
        req.setCharacterEncoding("UTF-8");String action=req.getParameter("action");
        if("delete".equals(action)){Long id=parseId(req.getParameter("id"));if(id==null){resp.sendError(400);return;}try{dao.delete(id);resp.sendRedirect(req.getContextPath()+"/admin/attendance?notice=deleted");}catch(SQLException e){getServletContext().log("Attendance delete failed",e);resp.sendRedirect(req.getContextPath()+"/admin/attendance?notice=error");}return;}
        if(!"create".equals(action)&&!"update".equals(action)){resp.sendError(400);return;}
        boolean update="update".equals(action);Long id=update?parseId(req.getParameter("id")):null;if(update&&id==null){resp.sendError(400);return;}
        Map<String,String> r=new LinkedHashMap<>();for(String f:new String[]{"student_id","teacher_id","subject_id","section_id","attendance_date","status","remarks"}){String v=req.getParameter(f);r.put(f,v==null?"":v.trim());}
        String error=null;
        try{for(String f:new String[]{"student_id","teacher_id","subject_id","section_id"})if(Long.parseLong(r.get(f))<=0)throw new NumberFormatException();LocalDate.parse(r.get("attendance_date"));}catch(Exception e){error="Select the student, teacher, subject, section, and a valid attendance date.";}
        if(error==null&&!validStatus(r.get("status")))error="Select a valid attendance status.";
        if(error==null&&r.get("remarks").length()>255)error="Remarks must be 255 characters or fewer.";
        if(error!=null){forwardForm(req,resp,update?"update":"create",r,error);return;}
        try{dao.save(r,id);resp.sendRedirect(req.getContextPath()+"/admin/attendance?notice="+(update?"updated":"created"));}
        catch(SQLException e){getServletContext().log("Attendance save failed",e);String msg=e.getSQLState()!=null&&e.getSQLState().startsWith("23")?"This student already has an attendance record for that subject and date, or one of the selected records is invalid.":"Attendance could not be saved. Check the database setup.";forwardForm(req,resp,update?"update":"create",r,msg);}
    }

    private void forwardForm(HttpServletRequest req,HttpServletResponse resp,String mode,Map<String,?> record,String error)throws ServletException,IOException {
        req.setAttribute("activeAdminSection","attendance");req.setAttribute("mode",mode);req.setAttribute("record",record);req.setAttribute("formError",error);
        try{req.setAttribute("studentOptions",dao.getOptions("students"));req.setAttribute("teacherOptions",dao.getOptions("teachers"));req.setAttribute("subjectOptions",dao.getOptions("subjects"));req.setAttribute("sectionOptions",dao.getOptions("sections"));}
        catch(SQLException e){getServletContext().log("Attendance form options failed",e);req.setAttribute("formError","Attendance options could not be loaded. Check the database setup.");}
        req.getRequestDispatcher("/admin/attendance-form.jsp").forward(req,resp);
    }
    private boolean validStatus(String s){return "PRESENT".equals(s)||"ABSENT".equals(s)||"LATE".equals(s)||"LEAVE".equals(s);}
    private Long parseId(String raw){try{return raw==null?null:Long.valueOf(raw);}catch(NumberFormatException e){return null;}}
}
