package view;


import javax.swing.SwingUtilities;

import controllers.ProgramAdminService;
import model.ProgramRepository;
import security.AuthService;
import security.User;

public class App {
	public static void main(String[] args) {
		var repo = ProgramRepository.getInstance();
		var authService = new AuthService();

		SwingUtilities.invokeLater(() -> showLogin(authService, repo));
	}

	private static void showLogin(AuthService authService, ProgramRepository repo) {
		var loginFrame = new LoginFrame(authService, user -> openDashboard(repo, authService, user));
		loginFrame.setVisible(true);
	}

	private static void openDashboard(ProgramRepository repo, AuthService authService, User user) {
		Runnable onLogout = () -> SwingUtilities.invokeLater(() -> showLogin(authService, repo));
		if (user.isAdmin()) {
			var adminService = new ProgramAdminService(repo, user);
			var frame = new AdminFrame(adminService, onLogout);
			frame.setVisible(true);
		} else {
			var frame = new MainFrame(repo, user, onLogout);
			frame.setVisible(true);
		}
	}
}
