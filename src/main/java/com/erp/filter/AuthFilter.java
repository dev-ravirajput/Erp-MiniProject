package com.erp.filter;

import com.erp.model.User;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(urlPatterns = {"/admin/*", "/teacher/*", "/student/*", "/parent/*", "/notifications"})
public class AuthFilter implements Filter {
    @Override public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        Object value = request.getSession(false) == null ? null : request.getSession(false).getAttribute("user");
        if (!(value instanceof User user)) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=session");
            return;
        }
        String area = request.getRequestURI().substring(request.getContextPath().length()).split("/")[1].toUpperCase();
        if (!"NOTIFICATIONS".equals(area) && !user.getRole().name().equals(area)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have access to this area.");
            return;
        }
        chain.doFilter(req, res);
    }
}
