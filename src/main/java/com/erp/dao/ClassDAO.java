package com.erp.dao;

import com.erp.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** CRUD for academic classes and their sections. */
public class ClassDAO {
    public List<Map<String, Object>> getClasses() throws SQLException {
        List<Map<String, Object>> classes = new ArrayList<>();
        Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id, class_name, academic_year, status FROM classes ORDER BY academic_year DESC, class_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> record = new LinkedHashMap<>();
                long id = rs.getLong("id");
                record.put("id", id);
                record.put("class_name", rs.getString("class_name"));
                record.put("academic_year", rs.getString("academic_year"));
                record.put("status", rs.getInt("status"));
                record.put("sections", new ArrayList<Map<String, Object>>());
                classes.add(record);
                byId.put(id, record);
            }
        }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id, class_id, section_name FROM sections ORDER BY section_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> parent = byId.get(rs.getLong("class_id"));
                if (parent == null) continue;
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> sections = (List<Map<String, Object>>) parent.get("sections");
                Map<String, Object> section = new LinkedHashMap<>();
                section.put("id", rs.getLong("id"));
                section.put("class_id", rs.getLong("class_id"));
                section.put("section_name", rs.getString("section_name"));
                sections.add(section);
            }
        }
        return classes;
    }

    public Map<String, Object> getClass(long id) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id, class_name, academic_year, status FROM classes WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Map<String, Object> record = new LinkedHashMap<>();
                record.put("id", rs.getLong("id"));
                record.put("class_name", rs.getString("class_name"));
                record.put("academic_year", rs.getString("academic_year"));
                record.put("status", rs.getInt("status"));
                return record;
            }
        }
    }

    public List<Map<String, Object>> getClassesForSelect() throws SQLException {
        List<Map<String, Object>> classes = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id, class_name, academic_year, status FROM classes ORDER BY academic_year DESC, class_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> record = new LinkedHashMap<>();
                record.put("id", rs.getLong("id"));
                record.put("class_name", rs.getString("class_name"));
                record.put("academic_year", rs.getString("academic_year"));
                record.put("status", rs.getInt("status"));
                classes.add(record);
            }
        }
        return classes;
    }

    public List<Map<String, Object>> getActiveClassesForSelect() throws SQLException {
        List<Map<String, Object>> classes = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id, class_name, academic_year, status FROM classes WHERE status = 1 ORDER BY academic_year DESC, class_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> record = new LinkedHashMap<>();
                record.put("id", rs.getLong("id"));
                record.put("class_name", rs.getString("class_name"));
                record.put("academic_year", rs.getString("academic_year"));
                record.put("status", rs.getInt("status"));
                classes.add(record);
            }
        }
        return classes;
    }

    public Map<String, Object> getSection(long id) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id, class_id, section_name FROM sections WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Map<String, Object> section = new LinkedHashMap<>();
                section.put("id", rs.getLong("id"));
                section.put("class_id", rs.getLong("class_id"));
                section.put("section_name", rs.getString("section_name"));
                return section;
            }
        }
    }

    public void createClass(String name, String year) throws SQLException {
        execute("INSERT INTO classes (class_name, academic_year) VALUES (?, ?)", name, year);
    }

    public void updateClass(long id, String name, String year) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE classes SET class_name = ?, academic_year = ? WHERE id = ?")) {
            ps.setString(1, name);
            ps.setString(2, year);
            ps.setLong(3, id);
            if (ps.executeUpdate() == 0 && !exists(con, "classes", id)) throw new SQLException("Class not found.");
        }
    }

    public void toggleClassStatus(long id) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE classes SET status = 1 - status WHERE id = ?")) {
            ps.setLong(1, id);
            if (ps.executeUpdate() != 1) throw new SQLException("Class not found.");
        }
    }

    public void createSection(long classId, String name) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO sections (class_id, section_name) VALUES (?, ?)")) {
            ps.setLong(1, classId);
            ps.setString(2, name);
            ps.executeUpdate();
        }
    }

    public void updateSection(long id, long classId, String name) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE sections SET class_id = ?, section_name = ? WHERE id = ?")) {
            ps.setLong(1, classId);
            ps.setString(2, name);
            ps.setLong(3, id);
            if (ps.executeUpdate() == 0 && !exists(con, "sections", id)) throw new SQLException("Section not found.");
        }
    }

    public void deleteSection(long id) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM sections WHERE id = ?")) {
            ps.setLong(1, id);
            if (ps.executeUpdate() != 1) throw new SQLException("Section not found.");
        }
    }

    private boolean exists(Connection con, String table, long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM " + table + " WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private void execute(String sql, String name, String year) throws SQLException {
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, year);
            ps.executeUpdate();
        }
    }
}
