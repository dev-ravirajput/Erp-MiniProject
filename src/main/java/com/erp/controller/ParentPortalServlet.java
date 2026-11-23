package com.erp.controller;

import com.erp.dao.GuardianDAO;
import com.erp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ParentPortalServlet extends HttpServlet {
    private final GuardianDAO dao=new GuardianDAO();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        User user=(User)req.getSession(false).getAttribute("user");
        try{
            List<Map<String,Object>> children=dao.children(user.getId());req.setAttribute("children",children);
            if(!children.isEmpty()){
                long selected;
                try{selected=Long.parseLong(req.getParameter("student_id"));}catch(Exception e){selected=((Number)children.get(0).get("student_id")).longValue();}
                Map<String,Object> child=dao.child(user.getId(),selected);if(child==null){resp.sendError(403,"That student is not linked to this account.");return;}
                req.setAttribute("selectedChild",child);req.setAttribute("attendance",dao.attendance(user.getId(),selected));req.setAttribute("assignments",dao.assignments(user.getId(),selected));req.setAttribute("results",dao.results(user.getId(),selected));req.setAttribute("invoices",dao.invoices(user.getId(),selected));req.setAttribute("timetable",dao.timetable(user.getId(),selected));
            }
        }catch(SQLException e){getServletContext().log("Could not load parent portal",e);req.setAttribute("loadError","Family information could not be loaded. Apply database/008_academic_workflows.sql.");}
        req.getRequestDispatcher("/parent/dashboard.jsp").forward(req,resp);
    }
}
