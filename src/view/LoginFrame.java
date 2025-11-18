package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import security.AuthService;
import security.User;

/**
 * Simple login + registration frame shown before accessing the main UI.
 */
public class LoginFrame extends JFrame {
	private final AuthService authService;
	private final Consumer<User> onAuthenticated;

	private final JTextField loginUsernameField = new JTextField(18);
	private final JPasswordField loginPasswordField = new JPasswordField(18);
	private final JLabel loginStatusLabel = createStatusLabel();

	private final JTextField registerUsernameField = new JTextField(18);
	private final JPasswordField registerPasswordField = new JPasswordField(18);
	private final JLabel registerStatusLabel = createStatusLabel();

	public LoginFrame(AuthService authService, Consumer<User> onAuthenticated) {
		super("Degree Advisor - Sign In");
		this.authService = Objects.requireNonNull(authService, "authService");
		this.onAuthenticated = Objects.requireNonNull(onAuthenticated, "onAuthenticated");

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(420, 360);
		setLocationRelativeTo(null);

		initUI();
	}

	private void initUI() {
		JPanel container = new JPanel(new BorderLayout(10, 10));
		container.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		JLabel title = new JLabel("Degree Advisor Access", SwingConstants.CENTER);
		title.setFont(new Font("SansSerif", Font.BOLD, 18));
		container.add(title, BorderLayout.NORTH);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Sign In", createLoginPanel());
		tabs.addTab("Register", createRegisterPanel());
		container.add(tabs, BorderLayout.CENTER);

		setContentPane(container);
	}

	private JPanel createLoginPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridBagConstraints c = defaultConstraints();

		addLabeledField(panel, c, "Username", loginUsernameField);
		addLabeledField(panel, c, "Password", loginPasswordField);

		JButton loginButton = new JButton("Sign In");
		loginButton.addActionListener(e -> handleLogin());
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 2;
		panel.add(loginButton, c);

		c.gridy++;
		panel.add(loginStatusLabel, c);

		return panel;
	}

	private JPanel createRegisterPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridBagConstraints c = defaultConstraints();

		addLabeledField(panel, c, "Username", registerUsernameField);
		addLabeledField(panel, c, "Password", registerPasswordField);

		JLabel hint = new JLabel("Password must be 6+ characters.");
		hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
		hint.setForeground(new Color(108, 117, 125));
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 2;
		panel.add(hint, c);

		JButton registerButton = new JButton("Create Account");
		registerButton.addActionListener(e -> handleRegistration());
		c.gridy++;
		panel.add(registerButton, c);

		c.gridy++;
		panel.add(registerStatusLabel, c);

		return panel;
	}

	private void handleLogin() {
		loginStatusLabel.setText(" ");
		String username = loginUsernameField.getText().trim();
		char[] password = loginPasswordField.getPassword();

		if (username.isEmpty() || password.length == 0) {
			setStatus(loginStatusLabel, "Please provide username and password.", Color.RED);
			Arrays.fill(password, '\0');
			return;
		}

		try {
			Optional<User> user = authService.authenticate(username, password);
			if (user.isPresent()) {
				setStatus(loginStatusLabel, "Login successful. Loading dashboard…", new Color(40, 167, 69));
				SwingUtilities.invokeLater(() -> {
					dispose();
					onAuthenticated.accept(user.get());
				});
			} else {
				setStatus(loginStatusLabel, "Invalid username or password.", Color.RED);
			}
		} catch (SQLException ex) {
			showError("Unable to sign in", ex);
		} catch (IllegalStateException ex) {
			showError("Database unavailable", ex);
		} finally {
			Arrays.fill(password, '\0');
			loginPasswordField.setText("");
		}
	}

	private void handleRegistration() {
		registerStatusLabel.setText(" ");
		String username = registerUsernameField.getText().trim();
		char[] password = registerPasswordField.getPassword();

		try {
			User user = authService.register(username, password);
			setStatus(registerStatusLabel,
					"Account created! Switch to the Sign In tab to continue, " + user.getUsername() + ".", new Color(40, 167, 69));
			registerPasswordField.setText("");
		} catch (IllegalArgumentException ex) {
			setStatus(registerStatusLabel, ex.getMessage(), Color.RED);
		} catch (SQLException ex) {
			showError("Unable to register user", ex);
		} catch (IllegalStateException ex) {
			showError("Database unavailable", ex);
		} finally {
			Arrays.fill(password, '\0');
		}
	}

	private static void addLabeledField(JPanel panel, GridBagConstraints c, String labelText, JTextField field) {
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		panel.add(new JLabel(labelText), c);

		c.gridx = 1;
		panel.add(field, c);
	}

	private static GridBagConstraints defaultConstraints() {
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5, 5, 5, 5);
		c.fill = GridBagConstraints.HORIZONTAL;
		return c;
	}

	private static JLabel createStatusLabel() {
		JLabel label = new JLabel(" ", SwingConstants.CENTER);
		label.setFont(new Font("SansSerif", Font.PLAIN, 11));
		return label;
	}

	private static void setStatus(JLabel label, String message, Color color) {
		label.setText(message);
		label.setForeground(color);
	}

	private void showError(String heading, Exception ex) {
		JOptionPane.showMessageDialog(this, ex.getMessage(), heading, JOptionPane.ERROR_MESSAGE);
	}
}

