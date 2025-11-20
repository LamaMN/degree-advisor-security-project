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

	public User findUserByName(String name) throws SQLException {
		String sql = "SELECT id , username , role FROM users WHERE lower(username) = lower(?)";
		try (Connection connection = DatabaseManager.getConnection();
				PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setString(1, name.trim());
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return mapUser(rs);
				}
			}
		}
		return null;
	}

	public User register(String username, char[] password) throws SQLException {
		// Delegate validation
		List<String> errors = Validator.validateRegistraionCredentials(username, password);
		if (!errors.isEmpty()) {
			throw new IllegalArgumentException(String.join(" ", errors));
		}
		if (findUserByName(username) != null) {
			throw new IllegalArgumentException("Username already exists. Pick another one.");
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
			throw new SQLException("Unable to create user record");
		}

		throw new SQLException("Unable to create user record");
	}

	public Optional<User> authenticate(String username, char[] password) throws SQLException {
		String errors = Validator.validateLoginCredentials(username, password);
		if (!errors.isEmpty()) {
			return Optional.empty();
		}
	
		try (Connection connection = DatabaseManager.getConnection();
			 PreparedStatement statement = connection.prepareStatement(
					 "SELECT id, username, password_hash, salt, role FROM users WHERE lower(username) = lower(?)")) {
	
			statement.setString(1, username);
	
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return Optional.empty();
				}
	
				boolean matches = PasswordHasher.matches(password,
						resultSet.getString("salt"),
						resultSet.getString("password_hash"));
	
				if (!matches) {
					System.out.println("Wrong username or password.");
					return Optional.empty();
				}
	
				return Optional.of(mapUser(resultSet));
			}
		}
	}
	

	private static User mapUser(ResultSet resultSet) throws SQLException {
		int id = resultSet.getInt("id");
		String username = resultSet.getString("username");
		String roleValue = resultSet.getString("role");
		User.Role role = User.Role.valueOf(roleValue == null ? "STUDENT" : roleValue.toUpperCase());
		return new User(id, username, role);
	}

}
