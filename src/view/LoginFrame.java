package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
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

	// For dragging window
	private Point mouseDownCompCoords;

	public LoginFrame(AuthService authService, Consumer<User> onAuthenticated) {
		super("Degree Advisor - Sign In");
		this.authService = Objects.requireNonNull(authService, "authService");
		this.onAuthenticated = Objects.requireNonNull(onAuthenticated, "onAuthenticated");

		// Remove default window decorations
		setUndecorated(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(480, 440);
		setLocationRelativeTo(null);

		initUI();
	}

	private void initUI() {
		// Main container with border
		JPanel mainContainer = new JPanel(new BorderLayout(0, 0));
		mainContainer.setBackground(new Color(250, 251, 252));

		// Custom title bar
		JPanel titleBar = createCustomTitleBar();

		// Content container
		JPanel container = new JPanel(new BorderLayout(10, 15));
		container.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
		container.setBackground(new Color(250, 251, 252));

		// Header with icon
		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setBackground(new Color(250, 251, 252));

		JLabel iconLabel = new JLabel("👤", SwingConstants.CENTER);
		iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
		iconLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		headerPanel.add(iconLabel, BorderLayout.CENTER);

		JLabel title = new JLabel("Degree Advisor", SwingConstants.CENTER);
		title.setFont(new Font("Segoe UI", Font.BOLD, 22));
		title.setForeground(new Color(52, 58, 64));

		JPanel titleWrapper = new JPanel(new BorderLayout());
		titleWrapper.setBackground(new Color(250, 251, 252));
		titleWrapper.add(headerPanel, BorderLayout.NORTH);
		titleWrapper.add(title, BorderLayout.CENTER);

		container.add(titleWrapper, BorderLayout.NORTH);

		// Tabbed pane with custom styling
		JTabbedPane tabs = new JTabbedPane();
		tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		tabs.setBackground(Color.WHITE);
		tabs.addTab("Sign In", createLoginPanel());
		tabs.addTab("Register", createRegisterPanel());
		container.add(tabs, BorderLayout.CENTER);

		mainContainer.add(titleBar, BorderLayout.NORTH);
		mainContainer.add(container, BorderLayout.CENTER);

		setContentPane(mainContainer);
	}

	private JPanel createCustomTitleBar() {
		JPanel titleBar = new JPanel(new BorderLayout());
		titleBar.setBackground(new Color(88, 86, 214));
		titleBar.setPreferredSize(new Dimension(0, 35));

		// Make window draggable
		titleBar.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				mouseDownCompCoords = e.getPoint();
			}
		});

		titleBar.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				Point currCoords = e.getLocationOnScreen();
				setLocation(currCoords.x - mouseDownCompCoords.x, currCoords.y - mouseDownCompCoords.y);
			}
		});

		// Title label
		JLabel titleLabel = new JLabel("  Login");
		titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
		titleLabel.setForeground(Color.WHITE);

		// Window control buttons
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		controlPanel.setOpaque(false);

		JButton minimizeBtn = createWindowButton("-");
		JButton closeBtn = createWindowButton("×");

		minimizeBtn.addActionListener(e -> setState(JFrame.ICONIFIED));
		closeBtn.addActionListener(e -> System.exit(0));

		closeBtn.setBackground(new Color(220, 53, 69));

		controlPanel.add(minimizeBtn);
		controlPanel.add(closeBtn);

		titleBar.add(titleLabel, BorderLayout.WEST);
		titleBar.add(controlPanel, BorderLayout.EAST);

		return titleBar;
	}

	private JButton createWindowButton(String text) {
		JButton btn = new JButton(text);
		btn.setPreferredSize(new Dimension(40, 35));
		btn.setFont(new Font("Arial", Font.PLAIN, 22));
		btn.setForeground(Color.WHITE);
		btn.setBackground(new Color(88, 86, 214));
		btn.setBorderPainted(false);
		btn.setFocusPainted(false);
		btn.setContentAreaFilled(true);
		btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		btn.setMargin(new Insets(0, 0, 0, 0));

		btn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				if (text.equals("\u00D7")) {
					btn.setBackground(new Color(200, 35, 51));
				} else {
					btn.setBackground(new Color(108, 106, 224));
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (text.equals("\u00D7")) {
					btn.setBackground(new Color(220, 53, 69));
				} else {
					btn.setBackground(new Color(88, 86, 214));
				}
			}
		});

		return btn;
	}

	private JPanel createLoginPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		panel.setBackground(Color.WHITE);
		GridBagConstraints c = defaultConstraints();

		addLabeledField(panel, c, "Username", loginUsernameField);
		addLabeledField(panel, c, "Password", loginPasswordField);

		// Style the button
		JButton loginButton = new JButton("Sign In");
		styleButton(loginButton);
		loginButton.addActionListener(e -> handleLogin());
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 2;
		c.insets = new Insets(15, 5, 10, 5);
		panel.add(loginButton, c);


		// Add Enter key listener to both fields
		loginUsernameField.addActionListener(e -> loginButton.doClick());
		loginPasswordField.addActionListener(e -> loginButton.doClick());

		c.gridy++;
		c.insets = new Insets(5, 5, 5, 5);
		panel.add(loginStatusLabel, c);

		return panel;
	}

	private JPanel createRegisterPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		panel.setBackground(Color.WHITE);
		GridBagConstraints c = defaultConstraints();

		addLabeledField(panel, c, "Username", registerUsernameField);
		addLabeledField(panel, c, "Password", registerPasswordField);

		JLabel hint = new JLabel("Password must be 6+ characters.");
		hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		hint.setForeground(new Color(108, 117, 125));
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 2;
		c.insets = new Insets(2, 5, 5, 5);
		panel.add(hint, c);

		JButton registerButton = new JButton("Create Account");
		styleButton(registerButton);
		registerButton.addActionListener(e -> handleRegistration());
		c.gridy++;
		c.insets = new Insets(12, 5, 10, 5);
		panel.add(registerButton, c);

		
		// Add Enter key listener to both fields
		registerUsernameField.addActionListener(e -> registerButton.doClick());
		registerPasswordField.addActionListener(e -> registerButton.doClick());

		c.gridy++;
		c.insets = new Insets(5, 5, 5, 5);
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
					"Account created! Switch to Sign In tab, " + user.getUsername() + ".", new Color(40, 167, 69));
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

	private void addLabeledField(JPanel panel, GridBagConstraints c, String labelText, JTextField field) {
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		JLabel label = new JLabel(labelText);
		label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		label.setForeground(new Color(73, 80, 87));
		panel.add(label, c);

		c.gridx = 1;
		c.anchor = GridBagConstraints.EAST;

		// Style the text field
		field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		field.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(206, 212, 218), 1, true),
				BorderFactory.createEmptyBorder(5, 10, 5, 10)));

		panel.add(field, c);
	}

	private static GridBagConstraints defaultConstraints() {
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(8, 5, 8, 5);
		c.fill = GridBagConstraints.HORIZONTAL;
		return c;
	}

	private static JLabel createStatusLabel() {
		JLabel label = new JLabel(" ", SwingConstants.CENTER);
		label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		return label;
	}

	private static void setStatus(JLabel label, String message, Color color) {
		label.setText(message);
		label.setForeground(color);
	}

	private void styleButton(JButton button) {
		button.setPreferredSize(new Dimension(0, 38));
		button.setBackground(new Color(88, 86, 214));
		button.setForeground(Color.WHITE);
		button.setFocusPainted(false);
		button.setFont(new Font("Segoe UI", Font.BOLD, 14));
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));

		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				button.setBackground(new Color(108, 106, 224));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				button.setBackground(new Color(88, 86, 214));
			}
		});
	}

	private void showError(String heading, Exception ex) {
		JOptionPane.showMessageDialog(this, ex.getMessage(), heading, JOptionPane.ERROR_MESSAGE);
	}
}
