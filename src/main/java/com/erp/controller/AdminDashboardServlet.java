package com.erp.controller;

import com.erp.dao.AdminDashboardDAO;
import com.erp.dao.AppSettingsDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class AdminDashboardServlet extends HttpServlet {
    private final AdminDashboardDAO dashboardDAO=new AdminDashboardDAO();private final AppSettingsDAO settingsDAO=new AppSettingsDAO();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        req.setAttribute("activeAdminSection","dashboard");
        try{req.setAttribute("dashboardMetrics",dashboardDAO.getMetrics());}
        catch(SQLException e){getServletContext().log("Could not load admin dashboard metrics",e);req.setAttribute("dashboardError","Dashboard statistics could not be loaded. Check the database connection.");req.setAttribute("dashboardMetrics",java.util.Collections.emptyMap());}
        try{req.setAttribute("appSettings",settingsDAO.getSettings());}catch(SQLException e){getServletContext().log("Could not load website settings for dashboard",e);}
        req.getRequestDispatcher("/admin/dashboard.jsp").forward(req,resp);
    } 
}
