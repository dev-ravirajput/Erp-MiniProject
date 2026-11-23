package com.erp.controller;

import com.erp.dao.SubjectDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SubjectServlet extends HttpServlet {
    private final SubjectDAO subjectDAO = new SubjectDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        request.setAttribute("activeAdminSection", "subjects");
        String action = request.getParameter("action");
        if ("create".equals(action)) { forwardForm(request, response, "create", new LinkedHashMap<>(), null); return; }
        if ("edit".equals(action)) {
            Long id = parseId(request.getParameter("id"));
            if (id == null) { response.sendError(400, "A valid subject ID is required."); return; }
            try {
                Map<String, Object> subject = subjectDAO.getSubject(id);
                if (subject == null) { response.sendError(404, "Subject not found."); return; }
                forwardForm(request, response, "update", subject, null);
            } catch (SQLException exception) {
                getServletContext().log("Could not load subject for editing", exception);
                response.sendError(500, "Subject could not be loaded.");
            }
            return;
        }
        showList(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        request.setAttribute("activeAdminSection", "subjects");
        String action = request.getParameter("action");
        try {
            if ("create".equals(action) || "update".equals(action)) {
                saveSubject(request, response, "update".equals(action));
                return;
            }
            if (!"toggle".equals(action)) { response.sendError(400, "Unsupported subject action."); return; }
            Long id = parseId(request.getParameter("id"));
            if (id == null) { response.sendError(400, "A valid subject ID is required."); return; }
            subjectDAO.toggleStatus(id);
            response.sendRedirect(request.getContextPath() + "/admin/subjects?notice=status-updated");
        } catch (SQLException exception) {
            getServletContext().log("Subject change failed", exception);
            response.sendRedirect(request.getContextPath() + "/admin/subjects?notice=error");
        }
    }

    private void saveSubject(HttpServletRequest request, HttpServletResponse response, boolean update)
            throws ServletException, IOException {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("subject_code", value(request, "subject_code").toUpperCase());
        values.put("subject_name", value(request, "subject_name"));
        values.put("max_marks", value(request, "max_marks"));
        Long id = update ? parseId(request.getParameter("id")) : null;
        if (update && id == null) { response.sendError(400, "A valid subject ID is required."); return; }

        String error = null;
        int maxMarks = 0;
        if (values.get("subject_code").isEmpty() || values.get("subject_name").isEmpty() || values.get("max_marks").isEmpty()) {
            error = "Subject code, subject name, and maximum marks are required.";
        } else if (values.get("subject_code").length() > 30 || values.get("subject_name").length() > 100) {
            error = "Subject code or name exceeds the allowed length.";
        } else {
            try { maxMarks = Integer.parseInt(values.get("max_marks")); }
            catch (NumberFormatException exception) { error = "Maximum marks must be a whole number."; }
            if (error == null && maxMarks < 1) error = "Maximum marks must be greater than zero.";
        }
        if (error != null) { forwardForm(request, response, update ? "update" : "create", values, error); return; }
        try {
            if (update) subjectDAO.updateSubject(id, values.get("subject_code"), values.get("subject_name"), maxMarks);
            else subjectDAO.createSubject(values.get("subject_code"), values.get("subject_name"), maxMarks);
            response.sendRedirect(request.getContextPath() + "/admin/subjects?notice=" + (update ? "updated" : "created"));
        } catch (SQLException exception) {
            getServletContext().log("Subject save failed", exception);
            if (exception.getSQLState() != null && exception.getSQLState().startsWith("23")) {
                forwardForm(request, response, update ? "update" : "create", values, "That subject code is already in use.");
            } else {
                forwardForm(request, response, update ? "update" : "create", values,
                        "The subject could not be saved. Check the database connection and try again.");
            }
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String status = request.getParameter("status");
        if (!"active".equals(status) && !"inactive".equals(status)) status = "all";
        String search = request.getParameter("q");
        request.setAttribute("status", status);
        request.setAttribute("query", search == null ? "" : search.trim());
        request.setAttribute("notice", request.getParameter("notice"));
        try { request.setAttribute("subjects", subjectDAO.getSubjects(search, status)); }
        catch (SQLException exception) {
            getServletContext().log("Could not load subjects", exception);
            request.setAttribute("loadError", "Subjects could not be loaded. Check the database connection.");
        }
        request.getRequestDispatcher("/admin/subjects.jsp").forward(request, response);
    }

    private void forwardForm(HttpServletRequest request, HttpServletResponse response, String mode,
            Map<String, ?> subject, String error) throws ServletException, IOException {
        request.setAttribute("mode", mode);
        request.setAttribute("subject", subject);
        request.setAttribute("formError", error);
        request.setAttribute("pageTitle", "update".equals(mode) ? "Edit Subject" : "Add Subject");
        request.getRequestDispatcher("/admin/subject-form.jsp").forward(request, response);
    }

    private String value(HttpServletRequest request, String key) {
        String value = request.getParameter(key);
        return value == null ? "" : value.trim();
    }

    private Long parseId(String raw) {
        try { return raw == null ? null : Long.valueOf(raw); }
        catch (NumberFormatException exception) { return null; }
    }
}
