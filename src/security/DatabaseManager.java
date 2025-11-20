package security;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Handles initialization n access to the embedded SQLite database

public final class DatabaseManager {
	private static final String DATA_DIRECTORY = "data";
	private static final String DATABASE_FILE = "users.db";
	private static final String JDBC_URL = "jdbc:sqlite:" + DATA_DIRECTORY + "/" + DATABASE_FILE;

	public static Connection getConnection() throws SQLException {
		Connection connection = DriverManager.getConnection(JDBC_URL);
		return connection;
	}
}
