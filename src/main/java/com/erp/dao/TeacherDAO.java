package com.erp.dao;

import com.erp.util.DBConnection;
import com.erp.util.PasswordUtil;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Teacher accounts live in users; employment details live in teachers. */
public class TeacherDAO {
    public List<Map<String, Object>> getTeachers(String search, String status) throws SQLException { return getTeachers(search,status,200,0); }

    public List<Map<String, Object>> getTeachers(String search, String status,int limit,int offset) throws SQLException {
        List<Map<String, Object>> teachers = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT u.id, u.name, u.email, u.status, t.employee_code, t.phone, "
                + "t.qualification, t.joining_date, t.department FROM users u LEFT JOIN teachers t ON t.user_id = u.id "
                + "WHERE u.role = 'TEACHER' AND u.deleted_at IS NULL ");
        boolean hasSearch = search != null && !search.trim().isEmpty();
        if (hasSearch) sql.append("AND (u.name LIKE ? OR u.email LIKE ? OR t.employee_code LIKE ? OR t.department LIKE ?) ");
        if ("active".equals(status) || "inactive".equals(status)) sql.append("AND u.status = ? ");
        sql.append("ORDER BY u.name, u.email LIMIT ? OFFSET ?");
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int parameter = 1;
            if (hasSearch) {
                String pattern = "%" + search.trim() + "%";
                for (int i = 0; i < 4; i++) ps.setString(parameter++, pattern);
            }
            if ("active".equals(status) || "inactive".equals(status)) ps.setInt(parameter++, "active".equals(status) ? 1 : 0);
            ps.setInt(parameter++,limit);ps.setInt(parameter,offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> teacher = new LinkedHashMap<>();
                    teacher.put("id", rs.getLong("id"));
                    teacher.put("name", rs.getString("name"));
                    teacher.put("email", rs.getString("email"));
                    teacher.put("status", rs.getInt("status"));
                    teacher.put("employee_code", rs.getString("employee_code"));
                    teacher.put("phone", rs.getString("phone"));
                    teacher.put("qualification", rs.getString("qualification"));
                    teacher.put("joining_date", rs.getDate("joining_date"));
                    teacher.put("department", rs.getString("department"));
                    teachers.add(teacher);
                }
            }
        }
        return teachers;
    }

    public long countTeachers(String search,String status)throws SQLException{
        StringBuilder sql=new StringBuilder("SELECT COUNT(*) FROM users u LEFT JOIN teachers t ON t.user_id=u.id WHERE u.role='TEACHER' AND u.deleted_at IS NULL ");boolean hasSearch=search!=null&&!search.trim().isEmpty();if(hasSearch)sql.append("AND (u.name LIKE ? OR u.email LIKE ? OR t.employee_code LIKE ? OR t.department LIKE ?) ");if("active".equals(status)||"inactive".equals(status))sql.append("AND u.status=? ");
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql.toString())){int i=1;if(hasSearch){String pattern="%"+search.trim()+"%";for(int x=0;x<4;x++)p.setString(i++,pattern);}if("active".equals(status)||"inactive".equals(status))p.setInt(i,"active".equals(status)?1:0);try(ResultSet r=p.executeQuery()){r.next();return r.getLong(1);}}
    }

    public Map<String, Object> getTeacher(long id) throws SQLException {
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(
                "SELECT u.id, u.name, u.email, t.employee_code, t.phone, t.qualification, t.joining_date, t.department, "
                        + "ts.id AS assignment_id, ts.section_id, ts.academic_year, sec.class_id "
                        + "FROM users u LEFT JOIN teachers t ON t.user_id = u.id "
                        + "LEFT JOIN teacher_subject ts ON ts.id = (SELECT ts2.id FROM teacher_subject ts2 WHERE ts2.teacher_id = t.id ORDER BY ts2.academic_year DESC, ts2.id DESC LIMIT 1) "
                        + "LEFT JOIN sections sec ON sec.id = ts.section_id "
                        + "WHERE u.id = ? AND u.role = 'TEACHER' AND u.deleted_at IS NULL")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Map<String, Object> teacher = new LinkedHashMap<>();
                teacher.put("id", rs.getLong("id"));
                teacher.put("name", rs.getString("name"));
                teacher.put("email", rs.getString("email"));
                teacher.put("employee_code", rs.getString("employee_code"));
                teacher.put("phone", rs.getString("phone"));
                teacher.put("qualification", rs.getString("qualification"));
                teacher.put("joining_date", rs.getDate("joining_date"));
                teacher.put("department", rs.getString("department"));
                rs.getLong("assignment_id");
                boolean hasAssignment = !rs.wasNull();
                long sectionId = rs.getLong("section_id");
                teacher.put("section_id", rs.wasNull() ? "" : sectionId);
                teacher.put("class_id", rs.getObject("class_id"));
                String academicYear = rs.getString("academic_year");
                teacher.put("academic_year", academicYear);
                teacher.put("subject_ids", hasAssignment ? getAssignmentSubjectIds(con, id, sectionId, academicYear) : new ArrayList<Long>());
                return teacher;
            }
        }
    }

    public void createTeacher(Map<String, String> teacher, String password) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                long userId;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, 'TEACHER')",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, teacher.get("name"));
                    ps.setString(2, teacher.get("email"));
                    ps.setString(3, PasswordUtil.sha256(password));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Could not create the teacher account.");
                        userId = keys.getLong(1);
                    }
                }
                saveProfile(con, userId, teacher);
                saveAssignments(con, getTeacherProfileId(con, userId), teacher);
                con.commit();
            } catch (SQLException exception) {
                con.rollback();
                throw exception;
            }
        }
    }

    public void updateTeacher(long id, Map<String, String> teacher) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement("UPDATE users SET name = ?, email = ? "
                        + "WHERE id = ? AND role = 'TEACHER' AND deleted_at IS NULL")) {
                    ps.setString(1, teacher.get("name"));
                    ps.setString(2, teacher.get("email"));
                    ps.setLong(3, id);
                    if (ps.executeUpdate() != 1) throw new SQLException("Teacher account was not found.");
                }
                saveProfile(con, id, teacher);
                saveAssignments(con, getTeacherProfileId(con, id), teacher);
                con.commit();
            } catch (SQLException exception) {
                con.rollback();
                throw exception;
            }
        }
    }

    public void toggleStatus(long id) throws SQLException {
        updateAccountStatus(id, "UPDATE users SET status = 1 - status WHERE id = ? AND role = 'TEACHER' AND deleted_at IS NULL");
    }

    public void softDeleteTeacher(long id) throws SQLException {
        updateAccountStatus(id, "UPDATE users SET status = 0, deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND role = 'TEACHER' AND deleted_at IS NULL");
    }

    private void updateAccountStatus(long id, String sql) throws SQLException {
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            if (ps.executeUpdate() != 1) throw new SQLException("Teacher account was not found.");
        }
    }

    private void saveProfile(Connection con, long userId, Map<String, String> teacher) throws SQLException {
        String sql = "INSERT INTO teachers (user_id, employee_code, phone, qualification, joining_date, department) "
                + "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE employee_code = VALUES(employee_code), "
                + "phone = VALUES(phone), qualification = VALUES(qualification), joining_date = VALUES(joining_date), department = VALUES(department)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, teacher.get("employee_code"));
            setNullableString(ps, 3, teacher.get("phone"));
            setNullableString(ps, 4, teacher.get("qualification"));
            setNullableDate(ps, 5, teacher.get("joining_date"));
            setNullableString(ps, 6, teacher.get("department"));
            ps.executeUpdate();
        }
    }

    private void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isEmpty()) ps.setNull(index, java.sql.Types.VARCHAR);
        else ps.setString(index, value);
    }

    public List<Map<String, Object>> getClassOptions() throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement("SELECT id, class_name, academic_year FROM classes WHERE status = 1 ORDER BY academic_year DESC, class_name"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) { Map<String, Object> row = new LinkedHashMap<>(); row.put("id", rs.getLong("id")); row.put("class_name", rs.getString("class_name")); row.put("academic_year", rs.getString("academic_year")); result.add(row); }
        }
        return result;
    }

    public List<Map<String, Object>> getSectionOptions() throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement("SELECT s.id, s.class_id, s.section_name FROM sections s JOIN classes c ON c.id = s.class_id WHERE c.status = 1 ORDER BY c.class_name, s.section_name"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) { Map<String, Object> row = new LinkedHashMap<>(); row.put("id", rs.getLong("id")); row.put("class_id", rs.getLong("class_id")); row.put("section_name", rs.getString("section_name")); result.add(row); }
        }
        return result;
    }

    public List<Map<String, Object>> getSubjectOptions() throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement("SELECT id, subject_code, subject_name FROM subjects WHERE status = 1 ORDER BY subject_name"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) { Map<String, Object> row = new LinkedHashMap<>(); row.put("id", rs.getLong("id")); row.put("subject_code", rs.getString("subject_code")); row.put("subject_name", rs.getString("subject_name")); result.add(row); }
        }
        return result;
    }

    private long getTeacherProfileId(Connection con, long userId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT id FROM teachers WHERE user_id = ?")) { ps.setLong(1, userId); try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw new SQLException("Teacher profile was not created."); return rs.getLong(1); } }
    }

    private List<Long> getAssignmentSubjectIds(Connection con, long userId, long sectionId, String academicYear) throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT ts.subject_id FROM teacher_subject ts JOIN teachers t ON t.id = ts.teacher_id WHERE t.user_id = ? AND ts.section_id = ? AND ts.academic_year = ? ORDER BY ts.subject_id")) {
            ps.setLong(1, userId); ps.setLong(2, sectionId); ps.setString(3, academicYear); try (ResultSet rs = ps.executeQuery()) { while (rs.next()) ids.add(rs.getLong(1)); }
        }
        return ids;
    }

    private void saveAssignments(Connection con, long teacherId, Map<String, String> teacher) throws SQLException {
        String classRaw = teacher.get("class_id"), sectionRaw = teacher.get("section_id"), year = teacher.get("academic_year"), ids = teacher.get("subject_ids");
        boolean any = (classRaw != null && !classRaw.isEmpty()) || (sectionRaw != null && !sectionRaw.isEmpty()) || (year != null && !year.isEmpty()) || (ids != null && !ids.isEmpty());
        if (!any) return;
        long classId, sectionId;
        try { classId = Long.parseLong(classRaw); sectionId = Long.parseLong(sectionRaw); }
        catch (RuntimeException exception) { throw new SQLException("Select a class and section for the subject assignment.", exception); }
        if (year == null || year.trim().isEmpty() || year.length() > 20) throw new SQLException("Enter a valid academic year.");
        try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM sections s JOIN classes c ON c.id = s.class_id WHERE s.id = ? AND c.id = ? AND c.status = 1 AND c.academic_year = ?")) {
            ps.setLong(1, sectionId); ps.setLong(2, classId); ps.setString(3, year); try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw new SQLException("Selected section does not belong to the selected active class and academic year."); }
        }
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM teacher_subject WHERE teacher_id = ? AND section_id = ? AND academic_year = ?")) { ps.setLong(1, teacherId); ps.setLong(2, sectionId); ps.setString(3, year); ps.executeUpdate(); }
        if (ids != null && !ids.isEmpty()) try (PreparedStatement ps = con.prepareStatement("INSERT INTO teacher_subject (teacher_id, subject_id, section_id, academic_year) VALUES (?, ?, ?, ?)")) {
            for (String raw : ids.split(",")) { try { ps.setLong(1, teacherId); ps.setLong(2, Long.parseLong(raw)); ps.setLong(3, sectionId); ps.setString(4, year); ps.addBatch(); } catch (NumberFormatException exception) { throw new SQLException("Invalid subject selection.", exception); } }
            ps.executeBatch();
        }
    }

    private void setNullableDate(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isEmpty()) ps.setNull(index, java.sql.Types.DATE);
        else ps.setDate(index, Date.valueOf(value));
    }
}
