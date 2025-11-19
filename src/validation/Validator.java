package validation;

import java.util.ArrayList;
import java.util.List;

public class Validator {

    // Regex: allow only letters, digits, underscore, and dot for username
    private static final String USERNAME_REGEX = "^[A-Za-z0-9._]{3,}$";

    // Regex: allow letters, digits, and special characters @#$%^&+=! for password
    private static final String PASSWORD_REGEX = "^[A-Za-z0-9@#$%^&+=!]{6,}$";

    // Regex: allow only letters for category name
    private static final String CATEGORY_REGEX = "^[A-Za-z]{2,}$";

    /**
     * Normalizes input by trimming whitespace and converting to lowercase.
     */
    public static String normalize(String input) {
        return input == null ? null : input.trim().toLowerCase();
    }

    /**
     * Validates username and password.
     * Returns a list of error messages (empty if valid).
     */
    public static List<String> validateCredentials(String username, char[] password) {
        List<String> errors = new ArrayList<>();
        String normalizedUsername = normalize(username);
        String normalizedPassword = normalize(String.valueOf(password));

        // If either field is empty, return one unified error
        if (normalizedUsername == null || normalizedUsername.isEmpty() ||
                normalizedPassword == null || normalizedPassword.isEmpty()) {
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

    /**
     * Validates program category name and description.
     * Throws IllegalArgumentException if invalid.
     */
    public static void validateCategory(String name, String description) {
        String normalizedName = normalize(name);
        if (normalizedName == null || normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty.");
        }
        if (!normalizedName.matches(CATEGORY_REGEX)) {
            throw new IllegalArgumentException("Category name must contain only letters (A–Z or a–z).");
        }
        if (description != null && description.length() > 255) {
            throw new IllegalArgumentException("Description too long (max 255 chars).");
        }
    }

    /**
     * Validates program details.
     * Throws IllegalArgumentException if invalid.
     */
    public static void validateProgram(String name, double minSalary, double minPrevGpa, double postDegreeGpa) {
        String normalizedName = normalize(name);
        if (normalizedName == null || normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Program name cannot be empty.");
        }
        if (normalizedName.length() > 100) {
            throw new IllegalArgumentException("Program name too long (max 100 chars).");
        }
        if (minSalary < 0 || minSalary > 1_000_000) {
            throw new IllegalArgumentException("Salary must be between 0 and 1,000,000.");
        }
        if (minPrevGpa < 0.0 || minPrevGpa > 4.0) {
            throw new IllegalArgumentException("Previous GPA must be between 0.0 and 4.0.");
        }
        if (postDegreeGpa < 0.0 || postDegreeGpa > 4.0) {
            throw new IllegalArgumentException("Post-degree GPA must be between 0.0 and 4.0.");
        }
    }

}
