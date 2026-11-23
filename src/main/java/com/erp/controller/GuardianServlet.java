package com.erp.controller;

import com.erp.dao.GuardianDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class GuardianServlet extends HttpServlet {
    private final GuardianDAO dao=new GuardianDAO();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        req.setAttribute("activeAdminSection","guardians");
        try{req.setAttribute("guardians",dao.list());req.setAttribute("studentOptions",dao.studentOptions());}
        catch(SQLException e){getServletContext().log("Could not load parent accounts",e);req.setAttribute("loadError","Parent accounts could not be loaded. Apply database/008_academic_workflows.sql.");}
        req.getRequestDispatcher("/admin/guardians.jsp").forward(req,resp);
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        req.setCharacterEncoding("UTF-8");String name=value(req,"name"),email=value(req,"email").toLowerCase(),password=req.getParameter("password"),phone=value(req,"phone"),relationship=value(req,"relationship_label");String[] students=req.getParameterValues("student_ids");
        String error=null;if(name.isEmpty()||name.length()>100)error="Enter a name up to 100 characters.";else if(!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))error="Enter a valid email address.";else if(password==null||password.length()<8)error="The initial password must be at least 8 characters.";else if(phone.length()>20||relationship.length()>40)error="Phone or relationship exceeds its allowed length.";else if(students==null||students.length==0)error="Link this guardian to at least one student.";
        if(error!=null){req.setAttribute("formError",error);doGet(req,resp);return;}
        try{dao.create(name,email,password,phone,relationship,students);resp.sendRedirect(req.getContextPath()+"/admin/guardians?notice=created");}
        catch(SQLException e){getServletContext().log("Could not create parent account",e);req.setAttribute("formError",e.getSQLState()!=null&&e.getSQLState().startsWith("23")?"That email or student link already exists.":e.getMessage());doGet(req,resp);}
    }
    private String value(HttpServletRequest req,String key){String value=req.getParameter(key);return value==null?"":value.trim();}
}
