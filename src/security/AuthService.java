package security;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import validation.Validator;

//Provide registration and authentication workflows backed by the embedded DB

public class AuthService {

	public User register(String username, char[] password) throws SQLException {
		// Delegate validation
		List<String> errors = Validator.validateCredentials(username, password);
		if (!errors.isEmpty()) {
			throw new IllegalArgumentException(String.join(" ", errors));
		}

		String salt = PasswordHasher.generateSalt();
		String hash = PasswordHasher.hash(password, salt);

		try (Connection connection = DatabaseManager.getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"INSERT INTO users(username, password_hash, salt, role) VALUES (?, ?, ?, ?)",
						Statement.RETURN_GENERATED_KEYS)) {
			statement.setString(1, username);
			statement.setString(2, hash);
			statement.setString(3, salt);
			statement.setString(4, User.Role.STUDENT.name());
			statement.executeUpdate();

			try (ResultSet keys = statement.getGeneratedKeys()) {
				if (keys.next()) {
					return new User(keys.getInt(1), username, User.Role.STUDENT);
				}
			}
		} catch (SQLException ex) {
			if (isUniqueConstraintViolation(ex)) {
				throw new IllegalArgumentException("Username already exists. Pick another one.", ex);
			}
			throw ex;
		}

		throw new SQLException("Unable to create user record");
	}

	public Optional<User> authenticate(String username, char[] password) throws SQLException {
		// Delegate validation
		List<String> errors = Validator.validateCredentials(username, password);
		if (!errors.isEmpty()) {
			return Optional.empty(); 
		}

		try (Connection connection = DatabaseManager.getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT id, username, password_hash, salt, role FROM users WHERE username = ?")) {
			statement.setString(1, username);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					String storedHash = resultSet.getString("password_hash");
					String salt = resultSet.getString("salt");
					boolean matches = PasswordHasher.matches(password, salt, storedHash);
					if (matches) {
						return Optional.of(mapUser(resultSet));
					}
				}
			}
		}

		return Optional.empty();
	}

	private static User mapUser(ResultSet resultSet) throws SQLException {
		int id = resultSet.getInt("id");
		String username = resultSet.getString("username");
		String roleValue = resultSet.getString("role");
		User.Role role = User.Role.valueOf(roleValue == null ? "STUDENT" : roleValue.toUpperCase());
		return new User(id, username, role);
	}

	private static boolean isUniqueConstraintViolation(SQLException ex) {
		// SQLite constraint violation code
		return ex.getErrorCode() == 19 || "23000".equals(ex.getSQLState());
	}
}
