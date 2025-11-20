package validation;

import java.util.ArrayList;
import java.util.List;
import model.Program;
import model.ProgramCategory;


public class Validator {

    // Regex: allow only letters, digits, underscore, and dot for username
    private static final String USERNAME_REGEX = "^[A-Za-z0-9._]{3,}$";

    // Regex: allow letters, digits, and special characters @#$%^&+=! for password
    private static final String PASSWORD_REGEX = "^[A-Za-z0-9@#$%^&+=!]{6,}$";

    // Regex: allow only letters for category name
    private static final String CATEGORY_REGEX = "^[A-Za-z]{2,}$";

    // Regex: allow only letters for category name
    private static final String PROGRAM_REGEX = "^[A-Za-z](?:[A-Za-z\\- ]*[A-Za-z])?$" ;
    
    
    private static final double MIN_GPA = 0.0;
	private static final double MAX_GPA = 4.0;

    

    /**
     * Validates username and password.
     * Returns a list of error messages (empty if valid).
     */
    public static List<String> validateRegistraionCredentials(String username, char[] password) {
        List<String> errors = new ArrayList<>();
        String normalizedUsername = sanitize(username);
        String normalizedPassword = sanitize(String.valueOf(password));

        // If either field is empty, return one unified error
        if (normalizedUsername.isEmpty() || normalizedPassword.isEmpty()) {
            errors.add("All fields must be filled.");
            return errors;
        }

        // Username checks
        if (normalizedUsername.length() < 3) {
            errors.add("Username must be at least 3 characters long.");
        } else if (!normalizedUsername.matches(USERNAME_REGEX)) {
            errors.add("Username contains invalid characters. Allowed: letters, digits, underscore, dot.");
        }

        // Password checks
        if (normalizedPassword.length() < 6) {
            errors.add("Password must be at least 6 characters long.");
        } else if (!(normalizedPassword).matches(PASSWORD_REGEX)) {
            errors.add("Password contains invalid characters. Allowed: letters, digits, @#$%^&+=!");
        }

        return errors;
    }
    public static List<String> validateLoginCredentials(String username, char[] password) {
        List<String> errors = new ArrayList<>();
        String normalizedUsername = sanitize(username);
        String normalizedPassword = sanitize(String.valueOf(password));

        // If either field is empty, return one unified error
        if (normalizedUsername.isEmpty() || normalizedPassword.isEmpty()) {
            errors.add("All fields must be filled.");
            return errors;
        }

        return errors;
    }

    /**
     * Validates program category name and description.
     * Throws IllegalArgumentException if invalid.
     */
    public static void validateCategory(String name, String description) {
        String normalizedName = sanitize(name);
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty.");
        }
        if (!normalizedName.matches(CATEGORY_REGEX)) {
            throw new IllegalArgumentException("Category name must contain only letters (A–Z or a–z).");
        }
        if (!description.isEmpty() && description.length() > 255) {
            throw new IllegalArgumentException("Description too long (max 255 chars).");
        }
    }

    /**
     * Validates program details.
     * Throws IllegalArgumentException if invalid.
     */

    	public static void validateProgramInputs(String name, ProgramCategory category, double minSalary,
			double minPrevGpa, double postDegreeGpa, Program.InterestLevel interest) {
		if (sanitize(name).isEmpty()) {
			throw new IllegalArgumentException("Program name is required.");
		}
        if (name.length() > 100) {
            throw new IllegalArgumentException("Program name too long (max 100 chars).");
        }
        if(!name.matches(PROGRAM_REGEX)){
            throw new IllegalArgumentException("Program name contains invalid characters.");
        }
		if (category == null) {
			throw new IllegalArgumentException("Category selection is required.");
		}
		if (interest == null) {
			throw new IllegalArgumentException("Interest level is required.");
		}
		if (minSalary < 1200 || minSalary > 1_000_000) {
            throw new IllegalArgumentException("Salary must be between 1200 and 1,000,000.");
        }
		if (!isGpaInRange(minPrevGpa)) {
			throw new IllegalArgumentException("Previous GPA must be between 0.0 and 4.0.");
		}
		if (!isGpaInRange(postDegreeGpa)) {
			throw new IllegalArgumentException("Post-degree GPA must be between 0.0 and 4.0.");
		}
	}

    private static boolean isGpaInRange(double value) {
		return value >= MIN_GPA && value <= MAX_GPA;
	}

	public static String sanitize(String value) {
		return value == null ? "" : value.trim();
	}

    

}
