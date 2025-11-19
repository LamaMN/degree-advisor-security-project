package view;


import javax.swing.SwingUtilities;

import controllers.ProgramAdminService;
import model.ProgramRepository;
import security.AuthService;

public class App {
	public static void main(String[] args) {
		var repo = ProgramRepository.getInstance();
		var authService = new AuthService();

		SwingUtilities.invokeLater(() -> {
			var loginFrame = new LoginFrame(authService, user -> {
				if (user.isAdmin()) {
					var adminService = new ProgramAdminService(repo, user);
					var frame = new AdminFrame(adminService);
					frame.setVisible(true);
				} else {
					var frame = new MainFrame(repo, user);
					frame.setVisible(true);
				}
			});
			loginFrame.setVisible(true);
		});
	}
}
