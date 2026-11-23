package com.erp.controller;

import com.erp.dao.TeacherDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public class TeacherServlet extends HttpServlet {
    private final TeacherDAO teacherDAO = new TeacherDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        request.setAttribute("activeAdminSection", "teachers");
        String action = request.getParameter("action");
        if ("create".equals(action)) {
            forwardForm(request, response, "create", new LinkedHashMap<>(), null);
            return;
        }
        if ("edit".equals(action)) {
            Long id = parseId(request.getParameter("id"));
            if (id == null) { response.sendError(HttpServletResponse.SC_BAD_REQUEST, "A valid teacher ID is required."); return; }
            try {
                Map<String, Object> teacher = teacherDAO.getTeacher(id);
                if (teacher == null) { response.sendError(HttpServletResponse.SC_NOT_FOUND, "Teacher account not found."); return; }
                forwardForm(request, response, "update", teacher, null);
            } catch (SQLException exception) {
                getServletContext().log("Could not load teacher for editing", exception);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            return;
        }
        showList(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        try {
            if ("create".equals(action) || "update".equals(action)) {
                saveTeacher(request, response, "update".equals(action));
                return;
            }
            if (!"toggle".equals(action) && !"delete".equals(action)) { response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported teacher action."); return; }
            Long id = parseId(request.getParameter("id"));
            if (id == null) { response.sendError(HttpServletResponse.SC_BAD_REQUEST, "A valid teacher ID is required."); return; }
            if ("delete".equals(action)) {
                teacherDAO.softDeleteTeacher(id);
                response.sendRedirect(request.getContextPath() + "/admin/teachers?notice=deleted");
            } else {
                teacherDAO.toggleStatus(id);
                response.sendRedirect(request.getContextPath() + "/admin/teachers?notice=status-updated");
            }
        } catch (SQLException exception) {
            getServletContext().log("Teacher account update failed", exception);
            response.sendRedirect(request.getContextPath() + "/admin/teachers?notice=error");
        }
    }

    private void saveTeacher(HttpServletRequest request, HttpServletResponse response, boolean update)
            throws ServletException, IOException {
        Map<String, String> values = new LinkedHashMap<>();
        for (String field : new String[] {"name", "email", "employee_code", "phone", "qualification", "joining_date", "department"}) {
            String value = request.getParameter(field);
            values.put(field, value == null ? "" : value.trim());
        }
        for (String field : new String[] {"class_id", "section_id", "academic_year"}) { String value = request.getParameter(field); values.put(field, value == null ? "" : value.trim()); }
        String[] subjects = request.getParameterValues("subject_ids");
        values.put("subject_ids", subjects == null ? "" : String.join(",", subjects));
        values.put("email", values.get("email").toLowerCase());
        String password = request.getParameter("password");
        Long id = update ? parseId(request.getParameter("id")) : null;
        if (update && id == null) { response.sendError(HttpServletResponse.SC_BAD_REQUEST, "A valid teacher ID is required."); return; }

        String error = null;
        if (values.get("name").isEmpty() || values.get("email").isEmpty() || values.get("employee_code").isEmpty()) {
            error = "Name, email, and employee code are required.";
        } else if (values.get("name").length() > 100 || values.get("email").length() > 150
                || values.get("employee_code").length() > 50 || values.get("phone").length() > 20
                || values.get("qualification").length() > 150 || values.get("department").length() > 100) {
            error = "One or more fields exceed the allowed length.";
        } else if (!values.get("email").matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            error = "Enter a valid email address.";
        } else if (!update && (password == null || password.length() < 8)) {
            error = "The initial password must be at least 8 characters.";
        } else if (!values.get("joining_date").isEmpty()) {
            try { LocalDate.parse(values.get("joining_date")); }
            catch (DateTimeParseException exception) { error = "Enter a valid joining date."; }
        }
        if (error != null) { forwardForm(request, response, update ? "update" : "create", values, error); return; }
        try {
            if (update) teacherDAO.updateTeacher(id, values);
            else teacherDAO.createTeacher(values, password);
            response.sendRedirect(request.getContextPath() + "/admin/teachers?notice=" + (update ? "updated" : "created"));
        } catch (SQLException exception) {
            getServletContext().log("Teacher account save failed", exception);
            String message = exception.getSQLState() != null && exception.getSQLState().startsWith("23")
                    ? "That email address or employee code is already in use."
                    : "The teacher account could not be saved. Check the database connection and try again.";
            forwardForm(request, response, update ? "update" : "create", values, message);
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String status = request.getParameter("status");
        if (!"active".equals(status) && !"inactive".equals(status)) status = "all";
        String search = request.getParameter("q");
        int page=1;try{page=Math.max(1,Integer.parseInt(request.getParameter("page")));}catch(Exception ignored){}
        final int pageSize=25;
        request.setAttribute("status", status);
        request.setAttribute("query", search == null ? "" : search.trim());
        try {
            long total=teacherDAO.countTeachers(search,status);int pages=(int)Math.max(1,(total+pageSize-1)/pageSize);page=Math.min(page,pages);
            request.setAttribute("teachers", teacherDAO.getTeachers(search,status,pageSize,(page-1)*pageSize));request.setAttribute("page",page);request.setAttribute("pageCount",pages);request.setAttribute("totalCount",total);
        } catch (SQLException exception) {
            getServletContext().log("Could not load teacher accounts", exception);
            request.setAttribute("loadError", "Teacher accounts could not be loaded. Check the database connection.");
        }
        request.getRequestDispatcher("/admin/teachers.jsp").forward(request, response);
    }

    private void forwardForm(HttpServletRequest request, HttpServletResponse response, String mode,
            Map<String, ?> teacher, String error) throws ServletException, IOException {
        request.setAttribute("activeAdminSection", "teachers");
        request.setAttribute("mode", mode);
        request.setAttribute("teacher", teacher);
        request.setAttribute("formError", error);
        try {
            request.setAttribute("classOptions", teacherDAO.getClassOptions());
            request.setAttribute("sectionOptions", teacherDAO.getSectionOptions());
            request.setAttribute("subjectOptions", teacherDAO.getSubjectOptions());
        } catch (SQLException exception) {
            getServletContext().log("Could not load teacher assignment options", exception);
            request.setAttribute("formError", "Class, section, or subject options could not be loaded. Check the database setup.");
        }
        request.getRequestDispatcher("/admin/teacher-form.jsp").forward(request, response);
    }

    private Long parseId(String raw) {
        try { return raw == null ? null : Long.valueOf(raw); }
        catch (NumberFormatException exception) { return null; }
    }
}
