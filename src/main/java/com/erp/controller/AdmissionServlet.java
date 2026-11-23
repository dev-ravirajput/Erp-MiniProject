package com.erp.controller;

import com.erp.dao.AdmissionDAO;
import com.erp.dao.AcademicScheduleDAO;
import com.erp.dao.ClassDAO;
import com.erp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class AdmissionServlet extends HttpServlet {
    private final AdmissionDAO dao=new AdmissionDAO();private final ClassDAO classDAO=new ClassDAO();private final AcademicScheduleDAO scheduleDAO=new AcademicScheduleDAO();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        req.setAttribute("activeAdminSection","admissions");try{req.setAttribute("applications",dao.list());req.setAttribute("classOptions",classDAO.getActiveClassesForSelect());req.setAttribute("sectionOptions",scheduleDAO.options("sections"));}
        catch(SQLException e){getServletContext().log("Could not load admission records",e);req.setAttribute("loadError","Admissions could not be loaded. Apply database/008_academic_workflows.sql.");}
        req.getRequestDispatcher("/admin/admissions.jsp").forward(req,resp);
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        req.setCharacterEncoding("UTF-8");User user=(User)req.getSession(false).getAttribute("user");String action=value(req,"action");
        try{
            if("create".equals(action)){String name=value(req,"applicant_name"),email=value(req,"applicant_email").toLowerCase(),phone=value(req,"applicant_phone"),dob=value(req,"date_of_birth"),year=value(req,"academic_year"),notes=value(req,"notes"),classRaw=value(req,"requested_class_id");Long classId=classRaw.isEmpty()?null:Long.valueOf(classRaw);if(name.isEmpty()||name.length()>150||year.isEmpty()||year.length()>20||email.length()>150||phone.length()>30||notes.length()>10000)throw new IllegalArgumentException("Check the applicant name, contact details, academic year, and notes.");if(!email.isEmpty()&&!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))throw new IllegalArgumentException("Enter a valid email address.");if(!dob.isEmpty())LocalDate.parse(dob);String appNo="APP-"+java.util.UUID.randomUUID().toString().substring(0,8).toUpperCase();dao.create(appNo,name,email,phone,dob,classId,year,notes,user.getId());resp.sendRedirect(req.getContextPath()+"/admin/admissions?notice=created");return;}
            if("status".equals(action)){long id=positive(req.getParameter("id"));String status=value(req,"status");if(!List.of("RECEIVED","UNDER_REVIEW","ACCEPTED","REJECTED").contains(status))throw new IllegalArgumentException("Choose a valid application status.");dao.updateStatus(id,status);resp.sendRedirect(req.getContextPath()+"/admin/admissions?notice=updated");return;}
            if("enroll".equals(action)){long id=positive(req.getParameter("id")),section=positive(req.getParameter("section_id"));String admission=value(req,"admission_no"),roll=value(req,"roll_no"),phone=value(req,"phone"),password=req.getParameter("password"),year=value(req,"academic_year");if(admission.isEmpty()||admission.length()>50||roll.length()>50||phone.length()>20||year.isEmpty()||year.length()>20||password==null||password.length()<8)throw new IllegalArgumentException("Provide an admission number, academic year, and initial password of at least 8 characters.");dao.enroll(id,admission,roll,phone,password,section,year);resp.sendRedirect(req.getContextPath()+"/admin/admissions?notice=enrolled");return;}
            resp.sendError(400);
        }catch(IllegalArgumentException|java.time.DateTimeException e){req.setAttribute("formError",e.getMessage());doGet(req,resp);}catch(SQLException e){getServletContext().log("Admission update failed",e);req.setAttribute("formError",e.getSQLState()!=null&&e.getSQLState().startsWith("23")?"An email, admission number, or application number is already in use.":e.getMessage());doGet(req,resp);}
    }
    private String value(HttpServletRequest r,String key){String v=r.getParameter(key);return v==null?"":v.trim();}
    private long positive(String raw){try{long id=Long.parseLong(raw);if(id>0)return id;}catch(Exception ignored){}throw new IllegalArgumentException("Select a valid application or section.");}
}
