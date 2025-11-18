package security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

// Handles initialization n access to the embedded SQLite database
 
public final class DatabaseManager {
	private static final String DATA_DIRECTORY = "data";
	private static final String DATABASE_FILE = "users.db";
	private static final String JDBC_URL = "jdbc:sqlite:" + DATA_DIRECTORY + "/" + DATABASE_FILE;

	static {
		initialize();
	}

	private DatabaseManager() {
	}

	private static void initialize() {
		try {
			ensureDataDirectory();
			System.out.println("Using DB file: "
					+ java.nio.file.Paths.get(DATA_DIRECTORY, DATABASE_FILE).toAbsolutePath());
			loadDriver();
			createSchema();
		} catch (IOException | SQLException | ClassNotFoundException ex) {
			throw new IllegalStateException("Failed to initialize local database", ex);
		}
	}


	private static void ensureDataDirectory() throws IOException {
		Path dataDir = Paths.get(DATA_DIRECTORY);
		if (Files.notExists(dataDir)) {
			Files.createDirectories(dataDir);
		}
	}
	

	private static void loadDriver() throws ClassNotFoundException {
		Class.forName("org.sqlite.JDBC");
	}

	private static void createSchema() throws SQLException {
		try (Connection connection = DriverManager.getConnection(JDBC_URL);
				Statement statement = connection.createStatement()) {
			String sql = "CREATE TABLE IF NOT EXISTS users (" + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "username TEXT NOT NULL UNIQUE," + "password_hash TEXT NOT NULL," + "salt TEXT NOT NULL,"
					+ "created_at TEXT DEFAULT CURRENT_TIMESTAMP" + ");";
			statement.execute(sql);
		}
	}

	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(JDBC_URL);
	}
}

