package com.erp.controller;

import com.erp.dao.UserDAO;
import com.erp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class LoginServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            request.setAttribute("error", "Email and password are required.");
            request.getRequestDispatcher("/login.jsp").forward(request, response);
            return;
        }
        try {
            Optional<User> result = userDAO.authenticate(email, password);
            if (result.isEmpty()) {
                request.setAttribute("error", "Invalid email, password, or inactive account.");
                request.getRequestDispatcher("/login.jsp").forward(request, response);
                return;
            }
            User user = result.get();
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) oldSession.invalidate();
            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(30 * 60);
            session.setAttribute("user", user);
            String rolePath=user.getRole().name().toLowerCase();
            response.sendRedirect(request.getContextPath() + "/" + rolePath + (("student".equals(rolePath)||"teacher".equals(rolePath)||"parent".equals(rolePath))?"/dashboard":"/dashboard.jsp"));
        } catch (SQLException exception) {
            getServletContext().log("Login database failure", exception);
            request.setAttribute("error", "The database is unavailable. Check the MySQL configuration.");
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        }
    }
}
