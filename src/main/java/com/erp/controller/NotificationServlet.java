package com.erp.controller;

import com.erp.dao.NotificationDAO;
import com.erp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class NotificationServlet extends HttpServlet {
    private final NotificationDAO dao=new NotificationDAO();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        User user=(User)req.getSession(false).getAttribute("user");
        if("open".equals(req.getParameter("action"))){try{long id=Long.parseLong(req.getParameter("id"));String target=dao.markRead(user.getId(),id);if(target!=null&&target.startsWith("/")&&!target.startsWith("//")){resp.sendRedirect(req.getContextPath()+target);return;}resp.sendRedirect(req.getContextPath()+"/notifications");}catch(NumberFormatException|SQLException e){if(e instanceof SQLException)getServletContext().log("Could not mark notification read",e);resp.sendRedirect(req.getContextPath()+"/notifications");}return;}
        try{List<Map<String,Object>> rows=dao.recent(user.getId());req.setAttribute("notifications",rows);req.setAttribute("unreadCount",dao.unreadCount(user.getId()));}
        catch(SQLException e){getServletContext().log("Could not load notifications",e);req.setAttribute("notifications",List.of());req.setAttribute("unreadCount",0);req.setAttribute("loadError","Notifications could not be loaded. Apply database/007_notifications.sql and retry.");}
        req.getRequestDispatcher("/notifications.jsp").forward(req,resp);
    }
}
