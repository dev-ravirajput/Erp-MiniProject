package com.erp.filter;

import com.erp.dao.AuditDAO;
import com.erp.model.User;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.sql.SQLException;

/** Audits successful POST operations without storing submitted personal data. */
@WebFilter(urlPatterns = "/*", dispatcherTypes = DispatcherType.REQUEST)
public class AuditFilter implements Filter {
    private final AuditDAO dao = new AuditDAO();

    @Override public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        User actor = null;
        if (req.getSession(false) != null && req.getSession(false).getAttribute("user") instanceof User u) actor = u;
        StatusResponseWrapper wrapped = new StatusResponseWrapper(resp);
        chain.doFilter(request, wrapped);
        if (!"POST".equalsIgnoreCase(req.getMethod()) || actor == null || wrapped.getStatus() < 300 || wrapped.getStatus() >= 400 || (wrapped.getRedirectLocation() != null && wrapped.getRedirectLocation().contains("notice=error"))) return;

        String path = req.getServletPath();
        String action = safe(req.getParameter("action"), "update");
        String entity = path.startsWith("/") ? path.substring(1).replace('/', '.') : path;
        Long entityId = entityId(req);
        String details = "path=" + path + "; action=" + action;
        try { dao.record(actor.getId(), action, entity, entityId, details); }
        catch (SQLException e) { req.getServletContext().log("Could not write audit record for " + path, e); }
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.length() > 80 ? value.substring(0, 80) : value;
    }
    private Long entityId(HttpServletRequest request) {
        for (String key : new String[]{"id", "student_id", "teacher_id", "class_id", "section_id", "assignment_id", "exam_id", "invoice_id", "payment_id"}) {
            Long parsed = parseId(request.getParameter(key));
            if (parsed != null) return parsed;
        }
        return null;
    }
    private Long parseId(String raw) {
        try { return raw == null ? null : Long.valueOf(raw); }
        catch (NumberFormatException e) { return null; }
    }

    private static final class StatusResponseWrapper extends HttpServletResponseWrapper {
        private String redirectLocation;
        StatusResponseWrapper(HttpServletResponse response) { super(response); }
        @Override public void sendRedirect(String location) throws IOException { redirectLocation = location; super.sendRedirect(location); }
        String getRedirectLocation() { return redirectLocation; }
    }
}
