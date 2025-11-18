package view;


import javax.swing.SwingUtilities;
import logic.RecommendationEngine;
import model.ProgramRepository;
import security.AuthService;

public class App {
	public static void main(String[] args) {
		var repo = ProgramRepository.getInstance();
		var engine = new RecommendationEngine(repo.getPrograms());
		var authService = new AuthService();

		SwingUtilities.invokeLater(() -> {
			var loginFrame = new LoginFrame(authService, user -> {
				var frame = new MainFrame(engine, user);
				frame.setVisible(true);
			});
			loginFrame.setVisible(true);
		});
	}
}
