package controllers;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import model.Program;
import model.ProgramCategory;
import model.ProgramRepository;
import security.AuthService;
import security.User;

/**
 * Encapsulates all admin-level program and category management logic so it can
 * be exercised independently from the Swing UI.
 */
public class ProgramAdminService {

	private final ProgramRepository repository;
	private final User actor;

	public ProgramAdminService(ProgramRepository repository, User actor) {
		this.repository = Objects.requireNonNull(repository, "repository");
		this.actor = Objects.requireNonNull(actor, "actor");
	}

	public User getActor() {
		return actor;
	}

	public List<Program> listPrograms() {
		return repository.getPrograms();
	}

	public List<ProgramCategory> listCategories() {
		return repository.getCategories();
	}

	public ProgramCategory createCategory(String name, String description) throws SQLException {
		String sanitized = validation.Validator.sanitize(name);
		if (sanitized.isEmpty()) {
			throw new IllegalArgumentException("Category name is required.");
		}
		return repository.addCategory(actor, sanitized, description == null ? "" : description.trim());
	}

	public ProgramCategory ensureCategoryExists(String name) throws SQLException {
		String sanitized = validation.Validator.sanitize(name);
		if (sanitized.isEmpty()) {
			throw new IllegalArgumentException("Category is required.");
		}
		ProgramCategory existing = repository.findCategoryByName(sanitized);
		if (existing != null) {
			return existing;
		}
		return repository.addCategory(actor, sanitized, "");
	}

	public Program addProgram(String name, ProgramCategory category, double minSalary, double minPrevGpa,
			Program.InterestLevel interest, double postDegreeGpa) throws SQLException {
		validation.Validator.validateProgramInputs(name, category, minSalary, minPrevGpa, postDegreeGpa, interest);

		return repository.addProgram(actor, validation.Validator.sanitize(name), category.getId(), minSalary,
				minPrevGpa, interest,
				postDegreeGpa);
	}

	public void updateProgram(int programId, String name, ProgramCategory category, double minSalary, double minPrevGpa,
			Program.InterestLevel interest, double postDegreeGpa) throws SQLException {
		if (programId <= 0) {
			throw new IllegalArgumentException("Invalid program identifier.");
		}

		validation.Validator.validateProgramInputs(name, category, minSalary, minPrevGpa, postDegreeGpa, interest);

		repository.updateProgram(actor, programId, validation.Validator.sanitize(name), category.getId(), minSalary,
				minPrevGpa, interest,
				postDegreeGpa);
	}

	public void deleteProgram(int programId) throws SQLException {
		if (programId <= 0) {
			throw new IllegalArgumentException("Invalid program identifier.");
		}
		repository.deleteProgram(actor, programId);
	}

	public boolean authenticateAdminPassword(String username, char[] password) throws Exception {
		AuthService authService = new AuthService();
		Optional<User> user = authService.authenticate(username, password);
		if (user.isPresent()) {
			return true;
		}
		return false;
	}

}
