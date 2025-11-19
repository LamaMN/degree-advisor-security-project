package security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import model.Program;

// Handles initialization n access to the embedded SQLite database

public final class DatabaseManager {
	private static final String DATA_DIRECTORY = "data";
	private static final String DATABASE_FILE = "users.db";
	private static final String JDBC_URL = "jdbc:sqlite:" + DATA_DIRECTORY + "/" + DATABASE_FILE;

	private static final String[][] DEFAULT_CATEGORIES = {
			{ "Finance", "Finance & Financial Analysis programs" },
			{ "Marketing", "Marketing & Brand programs" },
			{ "Accounting", "Accounting & Audit programs" },
			{ "HRM", "Human Resources programs" },
			{ "Operations", "Operations & Supply Chain programs" } };

	private static final Program[] DEFAULT_PROGRAMS = {
<<<<<<< HEAD
			new Program("Finance - Financial Analyst", "Finance", 5000, 3.0,
					Program.InterestLevel.LOW, 3.5),
			new Program("Finance - Corporate Finance", "Finance", 5500, 3.0,
					Program.InterestLevel.LOW, 3.5),
			new Program("Marketing - Digital Marketing", "Marketing", 7000, 3.5,
					Program.InterestLevel.VERY_HIGH, 4.0),
			new Program("Marketing - Brand Management", "Marketing", 7200, 3.5,
					Program.InterestLevel.VERY_HIGH, 4.0),
			new Program("Accounting - Audit & Assurance", "Accounting", 5000, 3.0,
					Program.InterestLevel.HIGH, 3.5),
			new Program("Accounting - Management Accounting", "Accounting", 5200, 3.0,
=======
			ProgramFactory.create("Finance - Financial Analyst", "FN", 5000, 3.0,
					Program.InterestLevel.LOW, 3.5),
			ProgramFactory.create("Finance - Corporate Finance", "FN", 5500, 3.0,
					Program.InterestLevel.LOW, 3.5),
			ProgramFactory.create("Marketing - Digital Marketing", "MK", 7000, 3.5,
					Program.InterestLevel.VERY_HIGH, 4.0),
			ProgramFactory.create("Marketing - Brand Management", "MK", 7200, 3.5,
					Program.InterestLevel.VERY_HIGH, 4.0),
			ProgramFactory.create("Accounting - Audit & Assurance", "AC", 5000, 3.0,
					Program.InterestLevel.HIGH, 3.5),
			ProgramFactory.create("Accounting - Management Accounting", "AC", 5200, 3.0,
>>>>>>> 4fccca6f682d349f44175086621cbadca5eb8c32
					Program.InterestLevel.HIGH, 3.5),
			new Program("HRM - HR Specialist", "HRM", 5000, 3.0,
					Program.InterestLevel.HIGH, 3.5),
			new Program("HRM - Organizational Development", "HRM", 5400, 3.0,
					Program.InterestLevel.HIGH, 3.5),
<<<<<<< HEAD
			new Program("Operations - Operations Analyst", "Operations", 6000, 3.5,
					Program.InterestLevel.MEDIUM, 3.5),
			new Program("Operations - Supply Chain Management", "Operations", 6200, 3.5,
=======
			ProgramFactory.create("Operations - Operations Analyst", "OM", 6000, 3.5,
					Program.InterestLevel.MEDIUM, 3.5),
			ProgramFactory.create("Operations - Supply Chain Management", "OM", 6200, 3.5,
>>>>>>> 4fccca6f682d349f44175086621cbadca5eb8c32
					Program.InterestLevel.MEDIUM, 3.5) };

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
			seedDefaults();
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
		try (Connection connection = DriverManager.getConnection(JDBC_URL)) {
			enableForeignKeys(connection);
			try (Statement statement = connection.createStatement()) {
				statement.execute("""
						CREATE TABLE IF NOT EXISTS users (
							id INTEGER PRIMARY KEY AUTOINCREMENT,
							username TEXT NOT NULL UNIQUE,
							password_hash TEXT NOT NULL,
							salt TEXT NOT NULL,
							role TEXT NOT NULL DEFAULT 'STUDENT',
							created_at TEXT DEFAULT CURRENT_TIMESTAMP
						);
						""");
				statement.execute("""
						CREATE TABLE IF NOT EXISTS categories (
							id INTEGER PRIMARY KEY AUTOINCREMENT,
							name TEXT NOT NULL UNIQUE,
							description TEXT
						);
						""");
				statement.execute("""
						CREATE TABLE IF NOT EXISTS programs (
							id INTEGER PRIMARY KEY AUTOINCREMENT,
							name TEXT NOT NULL UNIQUE,
							category_id INTEGER NOT NULL,
							min_salary REAL NOT NULL,
							min_previous_gpa REAL NOT NULL,
							interest_level TEXT NOT NULL,
							post_degree_gpa REAL NOT NULL,
							created_at TEXT DEFAULT CURRENT_TIMESTAMP,
							FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE CASCADE
						);
						""");
			}
			ensureRoleColumn(connection);
		}
	}

	private static void ensureRoleColumn(Connection connection) throws SQLException {
		DatabaseMetaData meta = connection.getMetaData();
		try (ResultSet rs = meta.getColumns(null, null, "users", "role")) {
			if (!rs.next()) {
				try (Statement statement = connection.createStatement()) {
					statement.execute("ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'STUDENT'");
				}
			}
		}
	}

	private static void seedDefaults() throws SQLException {
		try (Connection connection = getConnection()) {
			seedCategories(connection);
			seedPrograms(connection);
		}
	}

	private static void seedCategories(Connection connection) throws SQLException {
		if (hasRows(connection, "categories")) {
			return;
		}
		try (PreparedStatement stmt = connection
				.prepareStatement("INSERT INTO categories(name, description) VALUES (?, ?)")) {
			for (String[] category : DEFAULT_CATEGORIES) {
				stmt.setString(1, category[0]);
				stmt.setString(2, category[1]);
				stmt.addBatch();
			}
			stmt.executeBatch();
		}
	}

	private static void seedPrograms(Connection connection) throws SQLException {
		if (hasRows(connection, "programs")) {
			return;
		}

		Map<String, Integer> categoryIds = loadCategoryIds(connection);
		try (PreparedStatement stmt = connection.prepareStatement(
				"INSERT INTO programs(name, category_id, min_salary, min_previous_gpa, interest_level, post_degree_gpa)"
						+ " VALUES (?, ?, ?, ?, ?, ?)")) {
			for (Program program : DEFAULT_PROGRAMS) {
				Integer categoryId = categoryIds.get(program.getCategory());
				if (categoryId == null) {
					continue;
				}
				stmt.setString(1, program.getName());
				stmt.setInt(2, categoryId);
				stmt.setDouble(3, program.getMinIndustrySalary());
				stmt.setDouble(4, program.getMinRequiredPreviousGPA());
				stmt.setString(5, program.getAnalyticalInterestRequired().name());
				stmt.setDouble(6, program.getRequiredAcceptableGPAAfterDegree());
				stmt.addBatch();
			}
			stmt.executeBatch();
		}
	}

	private static Map<String, Integer> loadCategoryIds(Connection connection) throws SQLException {
		Map<String, Integer> ids = new HashMap<>();
		try (Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT id, name FROM categories")) {
			while (rs.next()) {
				ids.put(rs.getString("name"), rs.getInt("id"));
			}
		}
		return ids;
	}

	private static boolean hasRows(Connection connection, String table) throws SQLException {
		try (Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT COUNT(1) AS total FROM " + table)) {
			return rs.next() && rs.getInt("total") > 0;
		}
	}

	private static void enableForeignKeys(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute("PRAGMA foreign_keys = ON");
		}
	}

	public static Connection getConnection() throws SQLException {
		Connection connection = DriverManager.getConnection(JDBC_URL);
		enableForeignKeys(connection);
		return connection;
	}
}
