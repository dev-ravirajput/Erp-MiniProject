package com.erp.controller;

import com.erp.dao.StudentDAO;
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

public class StudentServlet extends HttpServlet {
    private final StudentDAO studentDAO = new StudentDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        request.setAttribute("activeAdminSection", "students");
        String action = request.getParameter("action");
        if ("create".equals(action)) {
            forwardForm(request, response, "create", new LinkedHashMap<>(), null);
            return;
        }
        if ("edit".equals(action)) {
            Long id = parseId(request.getParameter("id"));
            if (id == null) { response.sendError(HttpServletResponse.SC_BAD_REQUEST, "A valid student ID is required."); return; }
            try {
                Map<String, Object> student = studentDAO.getStudent(id);
                if (student == null) { response.sendError(HttpServletResponse.SC_NOT_FOUND, "Student account not found."); return; }
                forwardForm(request, response, "update", student, null);
            } catch (SQLException exception) {
                getServletContext().log("Could not load student account for editing", exception);
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
                saveStudent(request, response, "update".equals(action));
                return;
            }
            if (!"toggle".equals(action) && !"delete".equals(action)) { response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported student action."); return; }
            Long id = parseId(request.getParameter("id"));
            if (id == null) { response.sendError(HttpServletResponse.SC_BAD_REQUEST, "A valid student ID is required."); return; }
            if ("delete".equals(action)) {
                studentDAO.softDeleteStudent(id);
                response.sendRedirect(request.getContextPath() + "/admin/students?notice=deleted");
            } else {
                studentDAO.toggleStatus(id);
                response.sendRedirect(request.getContextPath() + "/admin/students?notice=status-updated");
            }
        } catch (SQLException exception) {
            getServletContext().log("Student account update failed", exception);
            response.sendRedirect(request.getContextPath() + "/admin/students?notice=error");
        }
    }

    private void saveStudent(HttpServletRequest request, HttpServletResponse response, boolean update)
            throws ServletException, IOException {
        String name = value(request, "name");
        String email = value(request, "email").toLowerCase();
        String password = request.getParameter("password");
        Long id = update ? parseId(request.getParameter("id")) : null;
        Map<String, String> values = new LinkedHashMap<>();
        values.put("name", name);
        values.put("email", email);
        for (String field : new String[] {"admission_no", "roll_no", "phone", "dob", "gender", "address", "admission_date"}) {
            values.put(field, value(request, field));
        }
        for (String field : new String[] {"class_id", "section_id", "academic_year"}) values.put(field, value(request, field));
        String[] selectedSubjects = request.getParameterValues("subject_ids");
        values.put("subject_ids", selectedSubjects == null ? "" : String.join(",", selectedSubjects));
        if (update && id == null) { response.sendError(HttpServletResponse.SC_BAD_REQUEST, "A valid student ID is required."); return; }
        String error = null;
        if (name.isEmpty() || email.isEmpty()) error = "Name and email are required.";
        else if (values.get("admission_no").isEmpty()) error = "Admission number is required.";
        else if (values.get("class_id").isEmpty() || values.get("section_id").isEmpty() || values.get("academic_year").isEmpty()) error = "Select a class and section and enter the academic year.";
        else if (name.length() > 100 || email.length() > 150) error = "Name or email exceeds the allowed length.";
        else if (values.get("admission_no").length() > 50 || values.get("roll_no").length() > 50
                || values.get("phone").length() > 20 || values.get("address").length() > 60000) error = "One or more profile fields exceed the allowed length.";
        else if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) error = "Enter a valid email address.";
        else if (!update && (password == null || password.length() < 8)) error = "The initial password must be at least 8 characters.";
        else if (!values.get("gender").isEmpty() && !("MALE".equals(values.get("gender"))
                || "FEMALE".equals(values.get("gender")) || "OTHER".equals(values.get("gender")))) error = "Select a valid gender option.";
        if (error == null) {
            try {
                if (!values.get("dob").isEmpty()) LocalDate.parse(values.get("dob"));
                if (!values.get("admission_date").isEmpty()) LocalDate.parse(values.get("admission_date"));
            } catch (DateTimeParseException exception) { error = "Enter valid dates in the date fields."; }
        }
        if (error != null) { forwardForm(request, response, update ? "update" : "create", values, error); return; }
        try {
            if (update) studentDAO.updateStudent(id, values);
            else studentDAO.createStudent(values, password);
            response.sendRedirect(request.getContextPath() + "/admin/students?notice=" + (update ? "updated" : "created"));
        } catch (SQLException exception) {
            getServletContext().log("Student account save failed", exception);
            String message = exception.getSQLState() != null && exception.getSQLState().startsWith("23")
                    ? "That email address or admission number is already in use."
                    : "The student account could not be saved. Check the database connection and try again.";
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
            long total=studentDAO.countStudents(search,status);int pages=(int)Math.max(1,(total+pageSize-1)/pageSize);page=Math.min(page,pages);
            request.setAttribute("students", studentDAO.getStudents(search,status,pageSize,(page-1)*pageSize));request.setAttribute("page",page);request.setAttribute("pageCount",pages);request.setAttribute("totalCount",total);
        } catch (SQLException exception) {
            getServletContext().log("Could not load student accounts", exception);
            request.setAttribute("loadError", "Student accounts could not be loaded. Check the database connection.");
        }
        request.getRequestDispatcher("/admin/students.jsp").forward(request, response);
    }

    private void forwardForm(HttpServletRequest request, HttpServletResponse response, String mode,
            Map<String, ?> student, String error) throws ServletException, IOException {
        request.setAttribute("activeAdminSection", "students");
        request.setAttribute("mode", mode);
        request.setAttribute("student", student);
        request.setAttribute("formError", error);
        try {
            request.setAttribute("classOptions", studentDAO.getClassOptions());
            request.setAttribute("sectionOptions", studentDAO.getSectionOptions());
            request.setAttribute("subjectOptions", studentDAO.getSubjectOptions());
        } catch (SQLException exception) {
            getServletContext().log("Could not load student enrollment options", exception);
            request.setAttribute("formError", "Class, section, or subject options could not be loaded. Check the database setup.");
        }
        request.getRequestDispatcher("/admin/student-form.jsp").forward(request, response);
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
