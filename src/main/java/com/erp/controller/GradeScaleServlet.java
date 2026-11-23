package com.erp.controller;

import com.erp.dao.GradeScaleDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

public class GradeScaleServlet extends HttpServlet {
    private final GradeScaleDAO dao=new GradeScaleDAO();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{req.setAttribute("activeAdminSection","grades");try{req.setAttribute("gradeScales",dao.list());}catch(SQLException e){getServletContext().log("Could not load grade scales",e);req.setAttribute("loadError","Grade scales could not be loaded. Apply database/008_academic_workflows.sql.");}req.getRequestDispatcher("/admin/grade-scales.jsp").forward(req,resp);}
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        req.setCharacterEncoding("UTF-8");String action=req.getParameter("action");try{
            if("deactivate".equals(action)){dao.deactivate(Long.parseLong(req.getParameter("id")));resp.sendRedirect(req.getContextPath()+"/admin/grade-scales?notice=deactivated");return;}
            Long id=req.getParameter("id")==null||req.getParameter("id").isBlank()?null:Long.valueOf(req.getParameter("id"));BigDecimal min=new BigDecimal(req.getParameter("min_percent"));String label=req.getParameter("grade_label")==null?"":req.getParameter("grade_label").trim();if(min.signum()<0||min.compareTo(BigDecimal.valueOf(100))>0||min.scale()>2||label.isEmpty()||label.length()>10)throw new IllegalArgumentException("Use a threshold from 0 to 100 and a label up to 10 characters.");dao.save(id,min,label);resp.sendRedirect(req.getContextPath()+"/admin/grade-scales?notice=saved");
        }catch(IllegalArgumentException e){req.setAttribute("formError",e.getMessage());doGet(req,resp);}catch(SQLException e){getServletContext().log("Could not save grade scale",e);req.setAttribute("formError",e.getSQLState()!=null&&e.getSQLState().startsWith("23")?"Each percentage threshold must be unique.":e.getMessage());doGet(req,resp);}
    }
}
