package com.erp.dao;

import com.erp.model.Role;
import com.erp.model.User;
import com.erp.util.DBConnection;
import com.erp.util.PasswordUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserDAO {
    private static final String LOGIN_SQL = "SELECT id, name, email, role FROM users WHERE email = ? AND password = ? AND status = 1";

    public Optional<User> authenticate(String email, String password) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(LOGIN_SQL)) {
            statement.setString(1, email.trim().toLowerCase());
            statement.setString(2, PasswordUtil.sha256(password));
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) return Optional.empty();
                return Optional.of(new User(results.getLong("id"), results.getString("name"),
                        results.getString("email"), Role.from(results.getString("role"))));
            }
        }
    }
}
