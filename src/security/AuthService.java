package security;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

//Provide registration and authentication workflows backed by the embedded DB
 
public class AuthService {
	private static final int MIN_USERNAME_LENGTH = 3;
	private static final int MIN_PASSWORD_LENGTH = 6;

	public User register(String username, char[] password) throws SQLException {
		String normalizedUsername = normalize(username);
		validateCredentials(normalizedUsername, password);

		String salt = PasswordHasher.generateSalt();
		String hash = PasswordHasher.hash(password, salt);

		try (Connection connection = DatabaseManager.getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"INSERT INTO users(username, password_hash, salt, role) VALUES (?, ?, ?, ?)",
						Statement.RETURN_GENERATED_KEYS)) {
			statement.setString(1, normalizedUsername);
			statement.setString(2, hash);
			statement.setString(3, salt);
			statement.setString(4, User.Role.STUDENT.name());
			statement.executeUpdate();

			try (ResultSet keys = statement.getGeneratedKeys()) {
				if (keys.next()) {
					return new User(keys.getInt(1), normalizedUsername, User.Role.STUDENT);
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
		String normalizedUsername = normalize(username);
		if (normalizedUsername.isEmpty() || password == null || password.length == 0) {
			return Optional.empty();
		}

		try (Connection connection = DatabaseManager.getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT id, username, password_hash, salt, role FROM users WHERE username = ?")) {
			statement.setString(1, normalizedUsername);
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

	private static void validateCredentials(String username, char[] password) {
		if (username.isEmpty()) {
			throw new IllegalArgumentException("Username is required.");
		}
		if (username.length() < MIN_USERNAME_LENGTH) {
			throw new IllegalArgumentException("Username must be at least " + MIN_USERNAME_LENGTH + " characters.");
		}
		if (password == null || password.length < MIN_PASSWORD_LENGTH) {
			throw new IllegalArgumentException(
					"Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.");
		}
	}

	private static String normalize(String username) {
		return username == null ? "" : username.trim();
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

