package security;

// Simple representation of the authenticated user
 
public class User {
	public enum Role {
		ADMIN, STUDENT
	}

	private final int id;
	private final String username;
	private final Role role;

	public User(int id, String username, Role role) {
		this.id = id;
		this.username = username;
		this.role = role;
	}

	public int getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public Role getRole() {
		return role;
	}

	public boolean isAdmin() {
		return role == Role.ADMIN;
	}
}

