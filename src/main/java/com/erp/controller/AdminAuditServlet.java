package com.erp.controller;

import com.erp.dao.AuditDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class AdminAuditServlet extends HttpServlet {
    private final AuditDAO dao=new AuditDAO();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        req.setAttribute("activeAdminSection","audit");try{req.setAttribute("auditRows",dao.recent());}catch(SQLException e){getServletContext().log("Could not load audit log",e);req.setAttribute("loadError","Audit history could not be loaded. Apply database/008_academic_workflows.sql.");}
        req.getRequestDispatcher("/admin/audit.jsp").forward(req,resp);
    }
}
