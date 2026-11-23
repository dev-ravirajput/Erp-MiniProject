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

public class SubjectDAO {
    public List<Map<String, Object>> getSubjects(String search, String status) throws SQLException {
        List<Map<String, Object>> subjects = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id, subject_code, subject_name, max_marks, status FROM subjects WHERE 1=1 ");
        boolean hasSearch = search != null && !search.trim().isEmpty();
        if (hasSearch) sql.append("AND (subject_code LIKE ? OR subject_name LIKE ?) ");
        if ("active".equals(status) || "inactive".equals(status)) sql.append("AND status = ? ");
        sql.append("ORDER BY subject_name, subject_code");
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int parameter = 1;
            if (hasSearch) {
                String pattern = "%" + search.trim() + "%";
                ps.setString(parameter++, pattern);
                ps.setString(parameter++, pattern);
            }
            if ("active".equals(status) || "inactive".equals(status)) ps.setInt(parameter, "active".equals(status) ? 1 : 0);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> subject = new LinkedHashMap<>();
                    subject.put("id", rs.getLong("id"));
                    subject.put("subject_code", rs.getString("subject_code"));
                    subject.put("subject_name", rs.getString("subject_name"));
                    subject.put("max_marks", rs.getInt("max_marks"));
                    subject.put("status", rs.getInt("status"));
                    subjects.add(subject);
                }
            }
        }
        return subjects;
    }

    public Map<String, Object> getSubject(long id) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id, subject_code, subject_name, max_marks, status FROM subjects WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Map<String, Object> subject = new LinkedHashMap<>();
                subject.put("id", rs.getLong("id"));
                subject.put("subject_code", rs.getString("subject_code"));
                subject.put("subject_name", rs.getString("subject_name"));
                subject.put("max_marks", rs.getInt("max_marks"));
                subject.put("status", rs.getInt("status"));
                return subject;
            }
        }
    }

    public void createSubject(String code, String name, int maxMarks) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO subjects (subject_code, subject_name, max_marks) VALUES (?, ?, ?)")) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setInt(3, maxMarks);
            ps.executeUpdate();
        }
    }

    public void updateSubject(long id, String code, String name, int maxMarks) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE subjects SET subject_code = ?, subject_name = ?, max_marks = ? WHERE id = ?")) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setInt(3, maxMarks);
            ps.setLong(4, id);
            if (ps.executeUpdate() == 0 && !exists(con, id)) throw new SQLException("Subject not found.");
        }
    }

    public void toggleStatus(long id) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE subjects SET status = 1 - status WHERE id = ?")) {
            ps.setLong(1, id);
            if (ps.executeUpdate() != 1) throw new SQLException("Subject not found.");
        }
    }

    private boolean exists(Connection con, long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM subjects WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }
}
