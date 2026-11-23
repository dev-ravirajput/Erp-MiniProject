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

/** Student accounts come from users; student-specific fields are read from students. */
public class StudentDAO {
    public List<Map<String, Object>> getStudents(String search, String status) throws SQLException { return getStudents(search,status,200,0); }

    public List<Map<String, Object>> getStudents(String search, String status,int limit,int offset) throws SQLException {
        List<Map<String, Object>> students = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT u.id, u.name, u.email, u.status, u.created_at, "
                + "s.admission_no, s.roll_no, s.phone, s.dob, s.gender, s.address, s.admission_date "
                + "FROM users u LEFT JOIN students s ON s.user_id = u.id WHERE u.role = 'STUDENT' "
                + "AND u.deleted_at IS NULL ");
        boolean hasSearch = search != null && !search.trim().isEmpty();
        if (hasSearch) sql.append("AND (u.name LIKE ? OR u.email LIKE ? OR s.admission_no LIKE ? OR s.roll_no LIKE ?) ");
        if ("active".equals(status) || "inactive".equals(status)) sql.append("AND u.status = ? ");
        sql.append("ORDER BY u.name, u.email LIMIT ? OFFSET ?");
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int parameter = 1;
            if (hasSearch) {
                String pattern = "%" + search.trim() + "%";
                ps.setString(parameter++, pattern);
                ps.setString(parameter++, pattern);
                ps.setString(parameter++, pattern);
                ps.setString(parameter++, pattern);
            }
            if ("active".equals(status) || "inactive".equals(status)) ps.setInt(parameter, "active".equals(status) ? 1 : 0);
            parameter += ("active".equals(status) || "inactive".equals(status)) ? 1 : 0;
            ps.setInt(parameter++,limit);ps.setInt(parameter,offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> student = new LinkedHashMap<>();
                    student.put("id", rs.getLong("id"));
                    student.put("name", rs.getString("name"));
                    student.put("email", rs.getString("email"));
                    student.put("status", rs.getInt("status"));
                    student.put("created_at", rs.getTimestamp("created_at"));
                    student.put("admission_no", rs.getString("admission_no"));
                    student.put("roll_no", rs.getString("roll_no"));
                    student.put("phone", rs.getString("phone"));
                    student.put("dob", rs.getDate("dob"));
                    student.put("gender", rs.getString("gender"));
                    student.put("address", rs.getString("address"));
                    student.put("admission_date", rs.getDate("admission_date"));
                    students.add(student);
                }
            }
        }
        return students;
    }

    public long countStudents(String search,String status)throws SQLException{
        StringBuilder sql=new StringBuilder("SELECT COUNT(*) FROM users u LEFT JOIN students s ON s.user_id=u.id WHERE u.role='STUDENT' AND u.deleted_at IS NULL ");boolean hasSearch=search!=null&&!search.trim().isEmpty();if(hasSearch)sql.append("AND (u.name LIKE ? OR u.email LIKE ? OR s.admission_no LIKE ? OR s.roll_no LIKE ?) ");if("active".equals(status)||"inactive".equals(status))sql.append("AND u.status=? ");
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql.toString())){int i=1;if(hasSearch){String pattern="%"+search.trim()+"%";for(int x=0;x<4;x++)p.setString(i++,pattern);}if("active".equals(status)||"inactive".equals(status))p.setInt(i,"active".equals(status)?1:0);try(ResultSet r=p.executeQuery()){r.next();return r.getLong(1);}}
    }

    public Map<String, Object> getStudent(long id) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT u.id, s.id AS student_id, u.name, u.email, s.admission_no, s.roll_no, "
                     + "s.phone, s.dob, s.gender, s.address, s.admission_date, sc.id AS student_class_id, "
                     + "sc.academic_year, sc.section_id, sec.class_id FROM users u "
                     + "LEFT JOIN students s ON s.user_id = u.id "
                     + "LEFT JOIN student_class sc ON sc.id = (SELECT sc2.id FROM student_class sc2 "
                     + "WHERE sc2.student_id = s.id ORDER BY sc2.academic_year DESC, sc2.id DESC LIMIT 1) "
                     + "LEFT JOIN sections sec ON sec.id = sc.section_id "
                     + "WHERE u.id = ? AND u.role = 'STUDENT' AND u.deleted_at IS NULL")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Map<String, Object> student = new LinkedHashMap<>();
                student.put("id", rs.getLong("id"));
                student.put("name", rs.getString("name"));
                student.put("email", rs.getString("email"));
                student.put("admission_no", rs.getString("admission_no"));
                student.put("roll_no", rs.getString("roll_no"));
                student.put("phone", rs.getString("phone"));
                student.put("dob", rs.getDate("dob"));
                student.put("gender", rs.getString("gender"));
                student.put("address", rs.getString("address"));
                student.put("admission_date", rs.getDate("admission_date"));
                student.put("student_id", rs.getLong("student_id"));
                long studentClassId = rs.getLong("student_class_id");
                boolean hasEnrollment = !rs.wasNull();
                student.put("student_class_id", studentClassId);
                student.put("academic_year", rs.getString("academic_year"));
                student.put("class_id", rs.getLong("class_id"));
                student.put("section_id", rs.getLong("section_id"));
                student.put("subject_ids", hasEnrollment ? getSubjectIds(con, studentClassId) : new ArrayList<Long>());
                return student;
            }
        }
    }

    public void createStudent(Map<String, String> student, String password) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                long userId;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, 'STUDENT')",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, student.get("name"));
                    ps.setString(2, student.get("email"));
                    ps.setString(3, PasswordUtil.sha256(password));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Could not create the student account.");
                        userId = keys.getLong(1);
                    }
                }
                saveProfile(con, userId, student);
                long studentId = getStudentProfileId(con, userId);
                saveEnrollment(con, studentId, student);
                con.commit();
            } catch (SQLException exception) {
                con.rollback();
                throw exception;
            }
        }
    }

    public void updateStudent(long id, Map<String, String> student) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement("UPDATE users SET name = ?, email = ? "
                        + "WHERE id = ? AND role = 'STUDENT' AND deleted_at IS NULL")) {
                    ps.setString(1, student.get("name"));
                    ps.setString(2, student.get("email"));
                    ps.setLong(3, id);
                    if (ps.executeUpdate() != 1) throw new SQLException("Student account was not found.");
                }
                saveProfile(con, id, student);
                long studentId = getStudentProfileId(con, id);
                saveEnrollment(con, studentId, student);
                con.commit();
            } catch (SQLException exception) {
                con.rollback();
                throw exception;
            }
        }
    }

    public void softDeleteStudent(long id) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE users SET status = 0, deleted_at = CURRENT_TIMESTAMP "
                     + "WHERE id = ? AND role = 'STUDENT' AND deleted_at IS NULL")) {
            ps.setLong(1, id);
            if (ps.executeUpdate() != 1) throw new SQLException("Student account was not found.");
        }
    }

    public void toggleStatus(long id) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE users SET status = 1 - status WHERE id = ? AND role = 'STUDENT' AND deleted_at IS NULL")) {
            ps.setLong(1, id);
            if (ps.executeUpdate() != 1) throw new SQLException("Student account was not found.");
        }
    }

    private void saveProfile(Connection con, long userId, Map<String, String> student) throws SQLException {
        String sql = "INSERT INTO students (user_id, admission_no, roll_no, phone, dob, gender, address, admission_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE admission_no = VALUES(admission_no), "
                + "roll_no = VALUES(roll_no), phone = VALUES(phone), dob = VALUES(dob), gender = VALUES(gender), "
                + "address = VALUES(address), admission_date = VALUES(admission_date)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, student.get("admission_no"));
            setNullableString(ps, 3, student.get("roll_no"));
            setNullableString(ps, 4, student.get("phone"));
            setNullableDate(ps, 5, student.get("dob"));
            setNullableString(ps, 6, student.get("gender"));
            setNullableString(ps, 7, student.get("address"));
            setNullableDate(ps, 8, student.get("admission_date"));
            ps.executeUpdate();
        }
    }

    public List<Map<String, Object>> getClassOptions() throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(
                "SELECT id, class_name, academic_year FROM classes WHERE status = 1 ORDER BY academic_year DESC, class_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getLong("id")); row.put("class_name", rs.getString("class_name"));
                row.put("academic_year", rs.getString("academic_year")); result.add(row);
            }
        }
        return result;
    }

    public List<Map<String, Object>> getSectionOptions() throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(
                "SELECT s.id, s.class_id, s.section_name FROM sections s JOIN classes c ON c.id = s.class_id "
                        + "WHERE c.status = 1 ORDER BY c.class_name, s.section_name"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getLong("id")); row.put("class_id", rs.getLong("class_id"));
                row.put("section_name", rs.getString("section_name")); result.add(row);
            }
        }
        return result;
    }

    public List<Map<String, Object>> getSubjectOptions() throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(
                "SELECT id, subject_code, subject_name FROM subjects WHERE status = 1 ORDER BY subject_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getLong("id")); row.put("subject_code", rs.getString("subject_code"));
                row.put("subject_name", rs.getString("subject_name")); result.add(row);
            }
        }
        return result;
    }

    private long getStudentProfileId(Connection con, long userId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT id FROM students WHERE user_id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Student profile was not created.");
                return rs.getLong(1);
            }
        }
    }

    private List<Long> getSubjectIds(Connection con, long studentClassId) throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT subject_id FROM student_subject WHERE student_class_id = ? ORDER BY subject_id")) {
            ps.setLong(1, studentClassId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) ids.add(rs.getLong(1)); }
        }
        return ids;
    }

    private void saveEnrollment(Connection con, long studentId, Map<String, String> student) throws SQLException {
        long classId;
        long sectionId;
        try { classId = Long.parseLong(student.get("class_id")); sectionId = Long.parseLong(student.get("section_id")); }
        catch (NumberFormatException exception) { throw new SQLException("Select a class and section.", exception); }
        String academicYear = student.get("academic_year");
        if (academicYear == null || academicYear.trim().isEmpty() || academicYear.length() > 20) throw new SQLException("Enter a valid academic year.");
        try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM sections s JOIN classes c ON c.id = s.class_id "
                + "WHERE s.id = ? AND c.id = ? AND c.status = 1 AND c.academic_year = ?")) {
            ps.setLong(1, sectionId); ps.setLong(2, classId); ps.setString(3, academicYear);
            try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw new SQLException("Selected section does not belong to the selected active class."); }
        }
        String sql = "INSERT INTO student_class (student_id, section_id, roll_no, academic_year) VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE section_id = VALUES(section_id), roll_no = VALUES(roll_no)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, studentId); ps.setLong(2, sectionId); setNullableString(ps, 3, student.get("roll_no")); ps.setString(4, academicYear); ps.executeUpdate();
        }
        long studentClassId;
        try (PreparedStatement ps = con.prepareStatement("SELECT id FROM student_class WHERE student_id = ? AND academic_year = ?")) {
            ps.setLong(1, studentId); ps.setString(2, academicYear);
            try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw new SQLException("Student class assignment could not be saved."); studentClassId = rs.getLong(1); }
        }
        try (PreparedStatement delete = con.prepareStatement("DELETE FROM student_subject WHERE student_class_id = ?")) {
            delete.setLong(1, studentClassId); delete.executeUpdate();
        }
        String ids = student.get("subject_ids");
        if (ids != null && !ids.isEmpty()) {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO student_subject (student_class_id, subject_id) VALUES (?, ?)")) {
                for (String rawId : ids.split(",")) { try { ps.setLong(1, studentClassId); ps.setLong(2, Long.parseLong(rawId)); ps.addBatch(); } catch (NumberFormatException exception) { throw new SQLException("Invalid subject selection.", exception); } }
                ps.executeBatch();
            }
        }
    }

    private void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isEmpty()) ps.setNull(index, java.sql.Types.VARCHAR);
        else ps.setString(index, value);
    }

    private void setNullableDate(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isEmpty()) ps.setNull(index, java.sql.Types.DATE);
        else ps.setDate(index, Date.valueOf(value));
    }
}
