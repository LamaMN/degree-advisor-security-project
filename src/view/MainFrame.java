package view;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import controllers.RecommendationEngine;
import model.Program;
import model.ProgramRepository;
import security.User;

public class MainFrame extends JFrame {
private static final int SESSION_TIMEOUT_MS = 15_000; // the maximum should be 3 min to run timeout for the user page
	private final JTextField salaryField = new JTextField(15);
	private final JSpinner gpaSpinner = new JSpinner(new SpinnerNumberModel(3.0, 0.0, 4.0, 0.1));
	private JTextField gpaTextField = ((JSpinner.DefaultEditor) gpaSpinner.getEditor()).getTextField();
	private final JComboBox<String> interestCombo = new JComboBox<>(
			new String[] { "Low", "Medium", "High", "Very High" });
	private final JButton recommendBtn = new JButton("Find Programs");
	private final JButton clearBtn = new JButton("Clear");
	private final DefaultTableModel tableModel = new DefaultTableModel(
			new String[] { "Program", "Category", "Min Salary", "GPA Required",
					"Analytical Level", "Post-Degree GPA", "Extra Study Hours/Day" },
			0) {
		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
	};
	private final JTable resultTable = new JTable(tableModel);
	private final JLabel statusLabel = new JLabel(" ");
	private final ProgramRepository repository;
	private final User user;
	private final Runnable onLogout;
	private final JButton logoutButton = new JButton("Log Out");
	private Timer inactivityTimer;
	private AWTEventListener activityListener;

	// For dragging window
	private Point mouseDownCompCoords;

	public MainFrame(ProgramRepository repository, User user, Runnable onLogout) {
		super("  Degree Program Recommender");
		this.repository = Objects.requireNonNull(repository, "repository");
		this.user = Objects.requireNonNull(user, "user");
		this.onLogout = Objects.requireNonNull(onLogout, "onLogout");

		setUndecorated(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1300, 600);
		setLocationRelativeTo(null);

		initUI();
		statusLabel.setText("Signed in as " + user.getUsername());
		setupSessionMonitoring();
	}

	private void initUI() {
		// Main container with rounded border effect
		JPanel mainContainer = new JPanel(new BorderLayout(0, 0));
		mainContainer.setBackground(new Color(250, 251, 252));
		mainContainer.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(200, 205, 210), 2),
				BorderFactory.createEmptyBorder(0, 0, 0, 0)));

		// Custom title bar
		JPanel titleBar = createCustomTitleBar();

		// Content area
		JPanel contentArea = new JPanel(new BorderLayout(10, 10));
		contentArea.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
		contentArea.setBackground(new Color(250, 251, 252));

		// Welcome panel
		JPanel welcomePanel = createWelcomePanel();

		// Input panel with improved styling
		JPanel inputPanel = createInputPanel();

		// Results panel with better formatting
		JPanel resultsPanel = createResultsPanel();

		// Status bar at bottom
		JPanel statusPanel = createStatusPanel();

		// Add components
		contentArea.add(welcomePanel, BorderLayout.NORTH);
		contentArea.add(inputPanel, BorderLayout.WEST);
		contentArea.add(resultsPanel, BorderLayout.CENTER);
		contentArea.add(statusPanel, BorderLayout.SOUTH);

		mainContainer.add(titleBar, BorderLayout.NORTH);
		mainContainer.add(contentArea, BorderLayout.CENTER);

		setContentPane(mainContainer);

		// Event listeners
		salaryField.addActionListener(e -> recommendBtn.doClick());

		interestCombo.addActionListener(e -> {
			if (e.getModifiers() == ActionEvent.ACTION_PERFORMED) {
				recommendBtn.doClick();
			}
		});

		gpaTextField.addActionListener(e -> recommendBtn.doClick());
		recommendBtn.addActionListener(e -> onRecommend());
		clearBtn.addActionListener(e -> onClear());
	}

	private JPanel createCustomTitleBar() {
		JPanel titleBar = new JPanel(new BorderLayout());
		titleBar.setBackground(new Color(88, 86, 214));
		titleBar.setPreferredSize(new Dimension(0, 40));

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
		JLabel titleLabel = new JLabel("Degree Program Recommender");
		titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
		titleLabel.setForeground(Color.WHITE);

		// Window control buttons
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
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
		btn.setPreferredSize(new Dimension(45, 40));
		btn.setFont(new Font("Segoe UI", Font.PLAIN, 20));
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

	private JPanel createWelcomePanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
		panel.setBackground(new Color(250, 251, 252));

		JLabel welcomeLabel = new JLabel(String.format("Welcome back, %s!", user.getUsername()));
		welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
		welcomeLabel.setForeground(new Color(52, 58, 64));

		panel.add(welcomeLabel);
		return panel;
	}

	private JPanel createInputPanel() {
		JPanel container = new JPanel(new BorderLayout());
		container.setPreferredSize(new Dimension(340, 0));
		container.setBackground(Color.WHITE);
		container.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(222, 226, 230), 2, true),
				BorderFactory.createEmptyBorder(15, 20, 15, 20)));

		JPanel inputPanel = new JPanel(new GridBagLayout());
		inputPanel.setBackground(Color.WHITE);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(3, 5, 3, 5);

		// Icon and header using styled star symbol
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
		headerPanel.setBackground(Color.WHITE);

		JLabel iconLabel = new JLabel("🎯");
		iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));

		headerPanel.add(iconLabel);
		inputPanel.add(headerPanel, c);

		c.gridy++;
		c.insets = new Insets(0, 5, 8, 5);
		JLabel headerLabel = new JLabel("Your Preferences");
		headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
		headerLabel.setForeground(new Color(88, 86, 214));
		headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
		inputPanel.add(headerLabel, c);

		c.gridwidth = 1;
		c.gridy++;
		c.insets = new Insets(5, 5, 2, 5);

		// Salary field with dollar symbol
		addInputRow(inputPanel, c, "Minimum Salary (SAR):", salaryField,
				"Enter your desired minimum salary");
		salaryField.setText("5000");
		salaryField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		salaryField.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(206, 212, 218), 1, true),
				BorderFactory.createEmptyBorder(4, 8, 4, 8)));

		// GPA spinner
		c.gridy++;
		c.insets = new Insets(5, 5, 2, 5);
		addInputRow(inputPanel, c, "Previous GPA:", gpaSpinner,
				"Your current or previous GPA");
		gpaSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		((JSpinner.DefaultEditor) gpaSpinner.getEditor()).getTextField().setHorizontalAlignment(JTextField.CENTER);
		((JSpinner.DefaultEditor) gpaSpinner.getEditor()).getTextField().setFont(new Font("Segoe UI", Font.PLAIN, 13));
		((JSpinner.DefaultEditor) gpaSpinner.getEditor()).getTextField().setBorder(
				BorderFactory.createEmptyBorder(4, 8, 4, 8));

		// Interest combo box
		c.gridy++;
		c.insets = new Insets(5, 5, 2, 5);
		addInputRow(inputPanel, c, "Analytical Interest:", interestCombo,
				"How much do you enjoy analytical work?");
		interestCombo.setSelectedIndex(1);
		interestCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		interestCombo.setBackground(Color.WHITE);
		interestCombo.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(206, 212, 218), 1, true),
				BorderFactory.createEmptyBorder(2, 4, 2, 4)));

		// Button panel
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 2;
		c.insets = new Insets(12, 5, 5, 5);
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
		buttonPanel.setBackground(Color.WHITE);

		recommendBtn.setPreferredSize(new Dimension(140, 42));
		recommendBtn.setBackground(new Color(88, 86, 214));
		recommendBtn.setForeground(Color.WHITE);
		recommendBtn.setFocusPainted(false);
		recommendBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
		recommendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		recommendBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
		recommendBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				recommendBtn.setBackground(new Color(108, 106, 224));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				recommendBtn.setBackground(new Color(88, 86, 214));
			}
		});

		clearBtn.setPreferredSize(new Dimension(110, 42));
		clearBtn.setBackground(new Color(233, 236, 239));
		clearBtn.setForeground(new Color(73, 80, 87));
		clearBtn.setFocusPainted(false);
		clearBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		clearBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		clearBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
		clearBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				clearBtn.setBackground(new Color(222, 226, 230));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				clearBtn.setBackground(new Color(233, 236, 239));
			}
		});

		buttonPanel.add(recommendBtn);
		buttonPanel.add(clearBtn);
		inputPanel.add(buttonPanel, c);

		// Info note with lightbulb symbol
		c.gridy++;
		c.insets = new Insets(8, 8, 3, 8);
		JLabel noteLabel = new JLabel("<html><div style='text-align: center; font-size: 9px; color: #868e96;'>"
				+ "<b>Tip:</b> Extra study hours = max(0, Required GPA - Your GPA)"
				+ "</div></html>");
		noteLabel.setHorizontalAlignment(SwingConstants.CENTER);
		inputPanel.add(noteLabel, c);

		container.add(inputPanel, BorderLayout.NORTH);
		return container;
	}

	private void addInputRow(JPanel panel, GridBagConstraints c, String labelText,
			java.awt.Component component, String tooltip) {
		c.gridx = 0;
		c.weightx = 0;
		c.anchor = GridBagConstraints.WEST;
		c.gridwidth = 2;
		JLabel label = new JLabel(labelText);
		label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		label.setForeground(new Color(73, 80, 87));
		panel.add(label, c);

		c.gridy++;
		c.gridx = 0;
		c.weightx = 1;
		((JComponent) component).setToolTipText(tooltip);
		panel.add(component, c);
		c.gridy++;
	}

	private JPanel createResultsPanel() {
		JPanel panel = new JPanel(new BorderLayout(0, 10));
		panel.setBackground(new Color(250, 251, 252));

		JLabel resultsLabel = new JLabel("Recommended Programs");
		resultsLabel.setFont(new Font("Segoe UI Semibold", Font.BOLD, 14));
		resultsLabel.setForeground(new Color(52, 58, 64));
		resultsLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
		panel.add(resultsLabel, BorderLayout.NORTH);

		// Configure table
		resultTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		resultTable.setRowHeight(32);
		resultTable.setShowGrid(true);
		resultTable.setGridColor(new Color(233, 236, 239));
		resultTable.setSelectionBackground(new Color(232, 232, 255));
		resultTable.setSelectionForeground(new Color(33, 37, 41));
		resultTable.setFillsViewportHeight(true);
		resultTable.setBackground(Color.WHITE);

		// Header styling
		JTableHeader header = resultTable.getTableHeader();
		header.setFont(new Font("Segoe UI Semibold", Font.BOLD, 11));
		header.setBackground(new Color(248, 249, 250));
		header.setForeground(new Color(73, 80, 87));
		header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
		header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(222, 226, 230)));

		// Center align numeric columns
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
		for (int i = 1; i < resultTable.getColumnCount(); i++) {
			resultTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
		}
		// Set column widths
		resultTable.getColumnModel().getColumn(0).setPreferredWidth(200); // Program
		resultTable.getColumnModel().getColumn(1).setPreferredWidth(80); // Category (narrower)
		resultTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Min Salary
		resultTable.getColumnModel().getColumn(3).setPreferredWidth(100); // GPA Required
		resultTable.getColumnModel().getColumn(4).setPreferredWidth(120); // Analytical Level
		resultTable.getColumnModel().getColumn(5).setPreferredWidth(120); // Post-Degree GPA
		resultTable.getColumnModel().getColumn(6).setPreferredWidth(150); // Extra Study Hours

		JScrollPane scrollPane = new JScrollPane(resultTable);
		scrollPane.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(222, 226, 230), 2, true),
				BorderFactory.createEmptyBorder(0, 0, 0, 0)));
		scrollPane.getViewport().setBackground(Color.WHITE);

		panel.add(scrollPane, BorderLayout.CENTER);

		return panel;
	}

	private JPanel createStatusPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
		panel.setBackground(new Color(248, 249, 250));
		panel.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(222, 226, 230)));

		statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		statusLabel.setForeground(new Color(108, 117, 125));
		panel.add(statusLabel);
		configureLogoutButton();
		panel.add(Box.createHorizontalStrut(15));
		panel.add(logoutButton);

		return panel;
	}

	private void configureLogoutButton() {
		logoutButton.setBackground(new Color(88, 86, 214));
		logoutButton.setForeground(Color.WHITE);
		logoutButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
		logoutButton.setFocusPainted(false);
		logoutButton.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));
		for (var listener : logoutButton.getActionListeners()) {
			logoutButton.removeActionListener(listener);
		}
		logoutButton.addActionListener(e -> performLogout());
		logoutButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	private void onRecommend() {
		try {
			double salary = Double.parseDouble(salaryField.getText().trim());
			if (salary <= 0) {
				throw new IllegalArgumentException("Salary must be greater than zero.");
			}
			double prevGpa = ((Double) ((SpinnerNumberModel) gpaSpinner.getModel()).getNumber()).doubleValue();
			if (prevGpa < 0.0 || prevGpa > 4.0) {
				throw new IllegalArgumentException("GPA must be between 0.0 and 4.0.");
			}
			String interest = (String) interestCombo.getSelectedItem();
			Program.InterestLevel level = Program.InterestLevel.fromString(interest);

			RecommendationEngine engine = new RecommendationEngine(repository.getPrograms());
			RecommendationEngine.Input input = new RecommendationEngine.Input(salary, prevGpa, level);
			List<RecommendationEngine.Recommendation> recs = engine.recommend(input);
			refreshTable(recs);

			if (recs.isEmpty()) {
				statusLabel.setText("No programs matched your criteria. Try adjusting your preferences.");
				statusLabel.setForeground(new Color(220, 53, 69));
				JOptionPane.showMessageDialog(this,
						"No programs matched the provided criteria.\nTry lowering your salary requirement or adjusting other preferences.",
						"No Matches Found", JOptionPane.INFORMATION_MESSAGE);
			} else {
				statusLabel.setText(String.format("Found %d matching program(s)", recs.size()));
				statusLabel.setForeground(new Color(40, 167, 69));
			}
		} catch (NumberFormatException ex) {
			statusLabel.setText("Error: Invalid salary value");
			statusLabel.setForeground(new Color(220, 53, 69));
			JOptionPane.showMessageDialog(this,
					"Please enter a valid numeric salary value (e.g., 5000).",
					"Input Error", JOptionPane.ERROR_MESSAGE);
		} catch (IllegalArgumentException ex) {
			statusLabel.setText("Error: " + ex.getMessage());
			statusLabel.setForeground(new Color(220, 53, 69));
			JOptionPane.showMessageDialog(this,
					ex.getMessage(),
					"Input Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void onClear() {
		salaryField.setText("5000");
		gpaSpinner.setValue(3.0);
		interestCombo.setSelectedIndex(1);
		tableModel.setRowCount(0);
		statusLabel.setText(" ");
	}

	private void refreshTable(List<RecommendationEngine.Recommendation> recs) {
		tableModel.setRowCount(0);
		for (var r : recs) {
			Program p = r.program;
			tableModel.addRow(new Object[] {
					p.getName(),
					p.getCategory(),
					String.format("SAR %.0f", p.getMinIndustrySalary()),
					String.format("%.2f", p.getMinRequiredPreviousGPA()),
					p.getAnalyticalInterestRequired(),
					String.format("%.2f", p.getRequiredAcceptableGPAAfterDegree()),
					String.format("%.2f hrs", r.suggestedExtraStudyHours)
			});
		}
	}

	private void setupSessionMonitoring() {
		inactivityTimer = new Timer(SESSION_TIMEOUT_MS, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				performLogout();
			}
		});
		inactivityTimer.setRepeats(false);
		inactivityTimer.start();

		long mask = AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK;
		activityListener = new AWTEventListener() {
			@Override
			public void eventDispatched(AWTEvent event) {
				Object source = event.getSource();
				if (source instanceof Component) {
					Component component = (Component) source;
					if (SwingUtilities.isDescendingFrom(component, getRootPane())) {
						resetInactivityTimer();
					}
				}
			}
		};
		Toolkit.getDefaultToolkit().addAWTEventListener(activityListener, mask);
	}

	private void resetInactivityTimer() {
		if (inactivityTimer != null) {
			inactivityTimer.restart();
		}
	}

	private void performLogout() {
		if (!isDisplayable()) {
			return;
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (!isDisplayable()) {
					return;
				}
				dispose();
				onLogout.run();
			}
		});
	}

	private void cleanupSessionMonitoring() {
		if (inactivityTimer != null) {
			inactivityTimer.stop();
			inactivityTimer = null;
		}
		if (activityListener != null) {
			Toolkit.getDefaultToolkit().removeAWTEventListener(activityListener);
			activityListener = null;
		}
	}

	@Override
	public void dispose() {
		cleanupSessionMonitoring();
		super.dispose();
	}
}