package com.erp.controller;

import com.erp.dao.AcademicScheduleDAO;
import com.erp.dao.GuardianDAO;
import com.erp.dao.PromotionDAO;
import com.erp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class PromotionServlet extends HttpServlet {
    private final PromotionDAO dao=new PromotionDAO();private final GuardianDAO guardianDAO=new GuardianDAO();private final AcademicScheduleDAO scheduleDAO=new AcademicScheduleDAO();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        req.setAttribute("activeAdminSection","promotions");try{req.setAttribute("students",guardianDAO.studentOptions());req.setAttribute("sections",scheduleDAO.options("sections"));req.setAttribute("subjects",scheduleDAO.options("subjects"));}
        catch(SQLException e){getServletContext().log("Could not load rollover options",e);req.setAttribute("loadError","Enrollment options could not be loaded.");}
        req.getRequestDispatcher("/admin/promotions.jsp").forward(req,resp);
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        User user=(User)req.getSession(false).getAttribute("user");String[] students=req.getParameterValues("student_ids"),subjects=req.getParameterValues("subject_ids");try{long section=Long.parseLong(req.getParameter("section_id"));int count=dao.promote(students,section,subjects);getServletContext().log("Academic year rollover completed by user "+user.getId()+" for "+count+" students.");resp.sendRedirect(req.getContextPath()+"/admin/promotions?notice="+count);}
        catch(Exception e){getServletContext().log("Academic year rollover failed",e);req.setAttribute("formError",e.getMessage());doGet(req,resp);}
    }
}
