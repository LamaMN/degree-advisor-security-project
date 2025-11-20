package model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import security.DatabaseManager;
import security.User;

public class ProgramRepository {
	private static final ProgramRepository INSTANCE = new ProgramRepository();

	private ProgramRepository() {
	}

	public static ProgramRepository getInstance() {
		return INSTANCE;
	}

	public List<Program> getPrograms() {
		List<Program> programs = new ArrayList<>();
		String sql = """
				SELECT p.id, p.name, c.name AS category, p.min_salary, p.min_previous_gpa,
				       p.interest_level, p.post_degree_gpa
				FROM programs p
				JOIN categories c ON p.category_id = c.id
				ORDER BY p.name ASC
				""";
		try (Connection connection = DatabaseManager.getConnection();
				PreparedStatement stmt = connection.prepareStatement(sql);
				ResultSet rs = stmt.executeQuery()) {
			while (rs.next()) {
				programs.add(mapProgram(rs));
			}
		} catch (SQLException ex) {
			throw new IllegalStateException("Unable to load programs", ex);
		}
		return Collections.unmodifiableList(programs);
	}

	public List<ProgramCategory> getCategories() {
		List<ProgramCategory> categories = new ArrayList<>();
		String sql = "SELECT id, name, description FROM categories ORDER BY name ASC";
		try (Connection connection = DatabaseManager.getConnection();
				PreparedStatement stmt = connection.prepareStatement(sql);
				ResultSet rs = stmt.executeQuery()) {
			while (rs.next()) {
				categories.add(mapCategory(rs));
			}
		} catch (SQLException ex) {
			throw new IllegalStateException("Unable to load categories", ex);
		}
		return Collections.unmodifiableList(categories);
	}

	public ProgramCategory addCategory(User actor, String name, String description) throws SQLException {
		requireAdmin(actor);

		validation.Validator.validateCategory(name, description);
		String sql = "INSERT INTO categories(name, description) VALUES (?, ?)";
		try (Connection connection = DatabaseManager.getConnection();
				PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			stmt.setString(1, name.trim());
			stmt.setString(2, description == null ? null : description.trim());
			stmt.executeUpdate();

			try (ResultSet keys = stmt.getGeneratedKeys()) {
				if (keys.next()) {
					return new ProgramCategory(keys.getInt(1), name.trim(),
							description == null ? "" : description.trim());
				}
			}
		}
		throw new SQLException("Unable to create category");
	}

	public void deleteCategory(User actor, int categoryId) throws SQLException {
		requireAdmin(actor);
		String sql = "DELETE FROM categories WHERE id = ?";
		try (Connection connection = DatabaseManager.getConnection();
				PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setInt(1, categoryId);
			stmt.executeUpdate();
		}
	}

	public ProgramCategory findCategoryByName(String name) throws SQLException {
		String sql = "SELECT id, name, description FROM categories WHERE lower(name) = lower(?)";
		try (Connection connection = DatabaseManager.getConnection();
				PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setString(1, name.trim());
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return mapCategory(rs);
				}
			}
		}
		return null;
	}

	public Program addProgram(User actor, String name, int categoryId, double minSalary, double minPrevGpa,
			Program.InterestLevel interest, double postDegreeGpa) throws SQLException {
		requireAdmin(actor);
		 
		if(findProgramByName(name)!=null) {
			// to check if program exists
			//handle error
			throw new IllegalArgumentException("Program already exists.");
		}

		String sql = """
				INSERT INTO programs(name, category_id, min_salary, min_previous_gpa, interest_level, post_degree_gpa)
				VALUES (?, ?, ?, ?, ?, ?)
				""";
		try (Connection connection = DatabaseManager.getConnection();
				PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			stmt.setString(1, name.trim());
			stmt.setInt(2, categoryId);
			stmt.setDouble(3, minSalary);
			stmt.setDouble(4, minPrevGpa);
			stmt.setString(5, interest.name());
			stmt.setDouble(6, postDegreeGpa);
			stmt.executeUpdate();

			try (ResultSet keys = stmt.getGeneratedKeys()) {
				if (keys.next()) {
					return fetchProgramById(keys.getInt(1));
				}
			}
		} catch (SQLException ex) {
			if (isUniqueConstraintViolation(ex)) {
				//error to uniqe constraint
				throw new IllegalArgumentException("Program already exists.", ex);
			}
			throw ex;
		}
		throw new SQLException("Unable to create program");
	}

	private static boolean isUniqueConstraintViolation(SQLException ex) {
		// SQLite constraint violation code
		return ex.getErrorCode() == 19 || "23000".equals(ex.getSQLState());
	}

	public void deleteProgram(User actor, int programId) throws SQLException {
		requireAdmin(actor);
		String sql = "DELETE FROM programs WHERE id = ?";
		try (Connection connection = DatabaseManager.getConnection();
				PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setInt(1, programId);
			stmt.executeUpdate();
		}
	}

	public void updateProgram(User actor, int programId, String name, int categoryId, double minSalary,
			double minPrevGpa,
			Program.InterestLevel interest, double postDegreeGpa) throws SQLException {
		requireAdmin(actor);
		
		Program p1 = findProgramByName(name); // to check if program exists
		Program p2 = fetchProgramById(programId);
		if((p1!=null && p2!=null) &&(p1.getId()!=p2.getId())) {
			//this statement to check if new name have been used in another program
			//handle error
			throw new IllegalArgumentException("Program already exists.");
		}

		String sql = """
				UPDATE programs
				SET name = ?, category_id = ?, min_salary = ?, min_previous_gpa = ?, interest_level = ?, post_degree_gpa = ?
				WHERE id = ?
				""";
		try (Connection connection = DatabaseManager.getConnection();
				PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setString(1, name.trim());
			stmt.setInt(2, categoryId);
			stmt.setDouble(3, minSalary);
			stmt.setDouble(4, minPrevGpa);
			stmt.setString(5, interest.name());
			stmt.setDouble(6, postDegreeGpa);
			stmt.setInt(7, programId);
			stmt.executeUpdate();
		}
	}

	private Program fetchProgramById(int programId) throws SQLException {
		String sql = """
				SELECT p.id, p.name, c.name AS category, p.min_salary, p.min_previous_gpa,
				       p.interest_level, p.post_degree_gpa
				FROM programs p
				JOIN categories c ON p.category_id = c.id
				WHERE p.id = ?
				""";
		try (Connection connection = DatabaseManager.getConnection();
				PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setInt(1, programId);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return mapProgram(rs);
				}
			}
		}
		throw new SQLException("Program not found after insert");
	}

	private Program mapProgram(ResultSet rs) throws SQLException {
		int id = rs.getInt("id");
		String name = rs.getString("name");
		String category = rs.getString("category");
		double minSalary = rs.getDouble("min_salary");
		double minPrevGpa = rs.getDouble("min_previous_gpa");
		String interest = rs.getString("interest_level");
		double postDegree = rs.getDouble("post_degree_gpa");
		return new Program(id, name, category, minSalary, minPrevGpa,
				Program.InterestLevel.valueOf(interest.toUpperCase()), postDegree);
	}

	private ProgramCategory mapCategory(ResultSet rs) throws SQLException {
		return new ProgramCategory(rs.getInt("id"), rs.getString("name"), rs.getString("description"));
	}

	private static void requireAdmin(User actor) {
		if (actor == null) {
			throw new SecurityException("User context is required for privileged operations.");
		}
		if (!actor.isAdmin()) {
			throw new SecurityException("Admin privileges are required for this operation.");
		}
	}


	public Program findProgramByName(String name) throws SQLException {
		String sql = "SELECT p.id, p.name, c.name AS category, p.min_salary, p.min_previous_gpa,p.interest_level, p.post_degree_gpa FROM programs p JOIN categories c ON p.category_id = c.id WHERE lower(p.name) = lower(?)";
		try (Connection connection = DatabaseManager.getConnection();
				PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setString(1, name.trim());
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return mapProgram(rs);
				}
			}
		}
		return null;
	}



}
