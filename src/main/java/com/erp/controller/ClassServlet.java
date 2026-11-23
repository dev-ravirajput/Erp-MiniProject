package com.erp.controller;

import com.erp.dao.ClassDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClassServlet extends HttpServlet {
    private final ClassDAO classDAO = new ClassDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        request.setAttribute("activeAdminSection", "classes");
        String action = request.getParameter("action");
        try {
            if ("create-class".equals(action)) { forwardClassForm(request, response, "create", new LinkedHashMap<>(), null); return; }
            if ("edit-class".equals(action)) {
                Long id = parseId(request.getParameter("id"));
                if (id == null) { response.sendError(400, "A valid class ID is required."); return; }
                Map<String, Object> record = classDAO.getClass(id);
                if (record == null) { response.sendError(404, "Class not found."); return; }
                forwardClassForm(request, response, "update", record, null); return;
            }
            if ("create-section".equals(action)) {
                Map<String, Object> section = new LinkedHashMap<>();
                section.put("class_id", request.getParameter("classId"));
                forwardSectionForm(request, response, "create", section, null); return;
            }
            if ("edit-section".equals(action)) {
                Long id = parseId(request.getParameter("id"));
                if (id == null) { response.sendError(400, "A valid section ID is required."); return; }
                Map<String, Object> section = classDAO.getSection(id);
                if (section == null) { response.sendError(404, "Section not found."); return; }
                forwardSectionForm(request, response, "update", section, null); return;
            }
            showList(request, response);
        } catch (SQLException exception) {
            getServletContext().log("Could not load class or section data", exception);
            response.sendError(500, "Class and section data could not be loaded.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        request.setAttribute("activeAdminSection", "classes");
        String action = request.getParameter("action");
        try {
            switch (action == null ? "" : action) {
                case "create-class": case "update-class": saveClass(request, response, "update-class".equals(action)); return;
                case "toggle-class": {
                    Long id = parseId(request.getParameter("id"));
                    if (id == null) { response.sendError(400, "A valid class ID is required."); return; }
                    classDAO.toggleClassStatus(id);
                    redirect(request, response, "class-status-updated"); return;
                }
                case "create-section": case "update-section": saveSection(request, response, "update-section".equals(action)); return;
                case "delete-section": {
                    Long id = parseId(request.getParameter("id"));
                    if (id == null) { response.sendError(400, "A valid section ID is required."); return; }
                    classDAO.deleteSection(id);
                    redirect(request, response, "section-deleted"); return;
                }
                default: response.sendError(400, "Unsupported class or section action.");
            }
        } catch (SQLException exception) {
            getServletContext().log("Class or section change failed", exception);
            String notice = exception.getSQLState() != null && exception.getSQLState().startsWith("23") ? "duplicate" : "error";
            redirect(request, response, notice);
        }
    }

    private void saveClass(HttpServletRequest request, HttpServletResponse response, boolean update)
            throws ServletException, IOException, SQLException {
        String name = value(request, "class_name");
        String year = value(request, "academic_year");
        Long id = update ? parseId(request.getParameter("id")) : null;
        if (update && id == null) { response.sendError(400, "A valid class ID is required."); return; }
        String error = name.isEmpty() || year.isEmpty() ? "Class name and academic year are required."
                : name.length() > 50 || year.length() > 20 ? "Class name or academic year exceeds the allowed length." : null;
        Map<String, String> values = new LinkedHashMap<>();
        values.put("class_name", name);
        values.put("academic_year", year);
        if (error != null) { forwardClassForm(request, response, update ? "update" : "create", values, error); return; }
        try {
            if (update) classDAO.updateClass(id, name, year); else classDAO.createClass(name, year);
            redirect(request, response, update ? "class-updated" : "class-created");
        } catch (SQLException exception) {
            if (exception.getSQLState() != null && exception.getSQLState().startsWith("23")) {
                forwardClassForm(request, response, update ? "update" : "create", values,
                        "That class already exists for this academic year.");
            } else throw exception;
        }
    }

    private void saveSection(HttpServletRequest request, HttpServletResponse response, boolean update)
            throws ServletException, IOException, SQLException {
        String name = value(request, "section_name");
        Long classId = parseId(request.getParameter("class_id"));
        Long id = update ? parseId(request.getParameter("id")) : null;
        if (update && id == null) { response.sendError(400, "A valid section ID is required."); return; }
        String error = classId == null || name.isEmpty() ? "Select a class and enter a section name."
                : name.length() > 20 ? "Section name must be 20 characters or fewer." : null;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("class_id", classId == null ? "" : classId);
        values.put("section_name", name);
        if (error != null) { forwardSectionForm(request, response, update ? "update" : "create", values, error); return; }
        try {
            if (update) classDAO.updateSection(id, classId, name); else classDAO.createSection(classId, name);
            redirect(request, response, update ? "section-updated" : "section-created");
        } catch (SQLException exception) {
            if (exception.getSQLState() != null && exception.getSQLState().startsWith("23")) {
                forwardSectionForm(request, response, update ? "update" : "create", values,
                        "That section already exists in the selected class or is referenced by another record.");
            } else throw exception;
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException {
        request.setAttribute("classes", classDAO.getClasses());
        String notice = request.getParameter("notice");
        request.setAttribute("notice", notice);
        request.getRequestDispatcher("/admin/classes.jsp").forward(request, response);
    }

    private void forwardClassForm(HttpServletRequest request, HttpServletResponse response, String mode,
            Map<String, ?> record, String error) throws ServletException, IOException {
        request.setAttribute("mode", mode);
        request.setAttribute("classRecord", record);
        request.setAttribute("formError", error);
        request.setAttribute("pageTitle", "update".equals(mode) ? "Edit Class" : "Add Class");
        request.getRequestDispatcher("/admin/class-form.jsp").forward(request, response);
    }

    private void forwardSectionForm(HttpServletRequest request, HttpServletResponse response, String mode,
            Map<String, ?> record, String error) throws ServletException, IOException, SQLException {
        request.setAttribute("mode", mode);
        request.setAttribute("sectionRecord", record);
        request.setAttribute("formError", error);
        request.setAttribute("pageTitle", "update".equals(mode) ? "Edit Section" : "Add Section");
        request.setAttribute("classes", classDAO.getClassesForSelect());
        request.getRequestDispatcher("/admin/section-form.jsp").forward(request, response);
    }

    private String value(HttpServletRequest request, String key) {
        String value = request.getParameter(key);
        return value == null ? "" : value.trim();
    }

    private Long parseId(String raw) {
        try { return raw == null ? null : Long.valueOf(raw); }
        catch (NumberFormatException exception) { return null; }
    }

    private void redirect(HttpServletRequest request, HttpServletResponse response, String notice) throws IOException {
        response.sendRedirect(request.getContextPath() + "/admin/classes?notice=" + notice);
    }
}
