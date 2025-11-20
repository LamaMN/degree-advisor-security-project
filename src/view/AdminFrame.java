package view;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import controllers.ProgramAdminService;
import model.Program;
import model.ProgramCategory;
import security.User;

public class AdminFrame extends JFrame {
	private static final Color BACKGROUND = new Color(250, 251, 252);
	private static final Color PRIMARY = new Color(92, 90, 208);
	private static final Color SECONDARY = new Color(110, 115, 201);
	private static final Color CARD_BORDER = new Color(224, 229, 236);
	private static final Color TEXT_PRIMARY = new Color(52, 58, 64);
	private static final Color TEXT_MUTED = new Color(108, 117, 125);
	private static final int SESSION_TIMEOUT_MS = 15_000; // the maximum should be 3 min to run timeout for the user page

	private final ProgramAdminService adminService;
	private final User adminUser;
	private final Runnable onLogout;

	private final DefaultTableModel programTableModel = new DefaultTableModel(
			new String[] { "Program", "Category", "Min Salary", "Min Required GPA", "Interest", "Post-Degree GPA" },
			0) {
		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
	};
	private final JTable programTable = new JTable(programTableModel);

	private final JTextField programNameField = new JTextField(16);
	private final JComboBox<Object> programCategoryCombo = new JComboBox<>();
	private final JTextField programSalaryField = new JTextField(8);
	private final JSpinner programMinGpaSpinner = new JSpinner(new SpinnerNumberModel(3.0, 0.0, 4.0, 0.1));
	private final JComboBox<Program.InterestLevel> programInterestCombo = new JComboBox<>(
			Program.InterestLevel.values());
	private final JSpinner programPostGpaSpinner = new JSpinner(new SpinnerNumberModel(3.5, 0.0, 4.0, 0.1));

	private final JButton addProgramBtn = new JButton("Add");
	private final JButton editProgramBtn = new JButton("Edit Selected");
	private final JButton deleteProgramBtn = new JButton("Delete");
	private final JButton addCategoryBtn = new JButton("Add Category");
	private final JButton logoutButton = new JButton("Log Out");

	private final JLabel statusLabel = new JLabel(" ");
	private JPanel statsPanel;
	private final JLabel totalProgramsValue = createStatValueLabel("0");
	private final JLabel totalCategoriesValue = createStatValueLabel("0");
	private final JLabel avgSalaryValue = createStatValueLabel("SAR 0");
	private List<ProgramCategory> categoryCache = List.of();
	private List<Program> programCache = List.of();
	private Point mouseDownCompCoords;
	private Timer inactivityTimer;
	private AWTEventListener activityListener;

	public AdminFrame(ProgramAdminService adminService, Runnable onLogout) {
		super("Degree Advisor Admin Console");
		this.adminService = Objects.requireNonNull(adminService, "adminService");
		this.adminUser = Objects.requireNonNull(adminService.getActor(), "adminUser");
		this.onLogout = Objects.requireNonNull(onLogout, "onLogout");
		setUndecorated(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1200, 720);
		setLocationRelativeTo(null);
		setResizable(true);
		initUI();
		refreshAllData();
		setupSessionMonitoring();
	}

	private void initUI() {
		JPanel mainContainer = new JPanel(new BorderLayout());
		mainContainer.setBackground(BACKGROUND);
		mainContainer.add(createCustomTitleBar(), BorderLayout.NORTH);
		mainContainer.add(createContentArea(), BorderLayout.CENTER);
		setContentPane(mainContainer);
	}

	private JPanel createContentArea() {
		JPanel container = new JPanel(new BorderLayout(20, 15));
		container.setBackground(BACKGROUND);
		container.setBorder(BorderFactory.createEmptyBorder(20, 25, 15, 25)); // Reduced bottom padding from 25 to 15

		container.add(createHeroPanel(), BorderLayout.NORTH);

		JPanel mainContent = new JPanel(new BorderLayout(0, 15));
		mainContent.setOpaque(false);
		statsPanel = buildStatsPanel();
		mainContent.add(statsPanel, BorderLayout.NORTH);

		JPanel body = new JPanel(new BorderLayout(20, 0));
		body.setOpaque(false);
		body.add(buildFormCard(), BorderLayout.WEST);
		body.add(buildTableCard(), BorderLayout.CENTER);
		mainContent.add(body, BorderLayout.CENTER);

		container.add(mainContent, BorderLayout.CENTER);

		statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		statusLabel.setForeground(TEXT_MUTED);
		JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		statusPanel.setOpaque(false);
		statusPanel.add(statusLabel);
		configureLogoutButton();
		statusPanel.add(Box.createHorizontalStrut(10));
		statusPanel.add(logoutButton);
		container.add(statusPanel, BorderLayout.SOUTH);

		return container;
	}

	private JPanel createCustomTitleBar() {
		JPanel titleBar = new JPanel(new BorderLayout());
		titleBar.setBackground(PRIMARY);
		titleBar.setPreferredSize(new Dimension(0, 38));

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

		JLabel titleLabel = new JLabel("  Admin – Program Management");
		titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
		titleLabel.setForeground(Color.WHITE);

		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		controlPanel.setOpaque(false);
		JButton minimizeBtn = createWindowButton("-");
		JButton closeBtn = createWindowButton("×");
		closeBtn.setBackground(new Color(220, 53, 69));
		minimizeBtn.addActionListener(e -> setState(JFrame.ICONIFIED));
		closeBtn.addActionListener(e -> System.exit(0));
		controlPanel.add(minimizeBtn);
		controlPanel.add(closeBtn);

		titleBar.add(titleLabel, BorderLayout.WEST);
		titleBar.add(controlPanel, BorderLayout.EAST);
		return titleBar;
	}

	private JButton createWindowButton(String text) {
		JButton btn = new JButton(text);
		btn.setPreferredSize(new Dimension(45, 38));
		btn.setFont(new Font("Arial", Font.PLAIN, 22));
		btn.setForeground(Color.WHITE);
		btn.setBackground(PRIMARY);
		btn.setBorderPainted(false);
		btn.setFocusPainted(false);
		btn.setContentAreaFilled(true);
		btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btn.setMargin(new Insets(0, 0, 0, 0));
		btn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				if ("×".equals(text)) {
					btn.setBackground(new Color(200, 35, 51));
				} else {
					btn.setBackground(new Color(108, 106, 224));
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if ("×".equals(text)) {
					btn.setBackground(new Color(220, 53, 69));
				} else {
					btn.setBackground(PRIMARY);
				}
			}
		});
		return btn;
	}

	private JPanel buildFormCard() {
		JPanel card = createCardPanel(new GridBagLayout());
		card.setPreferredSize(new Dimension(470, 0));
		GridBagConstraints c = defaultConstraints();
		int row = 0;

		JLabel heading = new JLabel("Manage Programs", SwingConstants.LEFT);
		heading.setFont(new Font("Segoe UI", Font.BOLD, 20));
		heading.setForeground(TEXT_PRIMARY);
		c.gridx = 0;
		c.gridy = row++;
		c.gridwidth = 2;
		c.insets = new Insets(0, 0, 10, 0);
		card.add(heading, c);

		programNameField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		programNameField.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(206, 212, 218), 1, true),
				BorderFactory.createEmptyBorder(4, 8, 4, 8)));


		programCategoryCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		programCategoryCombo.setBackground(Color.WHITE);
		programCategoryCombo.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(206, 212, 218), 1, true),
				BorderFactory.createEmptyBorder(2, 4, 2, 4)));

		addFormRow(card, c, row++, "Program Name", programNameField);
		addFormRow(card, c, row, "Category", programCategoryCombo);

		addCategoryBtn.setText("Manage Categories");
		styleLinkButton(addCategoryBtn);
		addCategoryBtn.addActionListener(e -> handleAddCategory());
		GridBagConstraints linkC = new GridBagConstraints();
		linkC.gridx = 1;
		linkC.gridy = row++;
		linkC.anchor = GridBagConstraints.EAST;
		linkC.insets = new Insets(0, 0, 5, 0);
		card.add(addCategoryBtn, linkC);

		programSalaryField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		programSalaryField.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(206, 212, 218), 1, true),
				BorderFactory.createEmptyBorder(4, 8, 4, 8)));

		programMinGpaSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		((JSpinner.DefaultEditor) programMinGpaSpinner.getEditor()).getTextField()
				.setHorizontalAlignment(JTextField.CENTER);
		((JSpinner.DefaultEditor) programMinGpaSpinner.getEditor()).getTextField()
				.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		((JSpinner.DefaultEditor) programMinGpaSpinner.getEditor()).getTextField().setBorder(
				BorderFactory.createEmptyBorder(4, 8, 4, 8));

		programInterestCombo.setSelectedIndex(1);
		programInterestCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		programInterestCombo.setBackground(Color.WHITE);
		programInterestCombo.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(206, 212, 218), 1, true),
				BorderFactory.createEmptyBorder(2, 4, 2, 4)));

				programPostGpaSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		((JSpinner.DefaultEditor) programPostGpaSpinner.getEditor()).getTextField()
				.setHorizontalAlignment(JTextField.CENTER);
		((JSpinner.DefaultEditor) programPostGpaSpinner.getEditor()).getTextField()
				.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		((JSpinner.DefaultEditor) programPostGpaSpinner.getEditor()).getTextField().setBorder(
				BorderFactory.createEmptyBorder(4, 8, 4, 8));

		addFormRow(card, c, row++, "Min Salary (SAR)", programSalaryField);
		addFormRow(card, c, row++, "Min Required GPA", programMinGpaSpinner);
		addFormRow(card, c, row++, "Required Interest", programInterestCombo);
		addFormRow(card, c, row++, "Post-Degree GPA", programPostGpaSpinner);

		c.gridy = row++;
		c.insets = new Insets(10, 5, 25, 5);
		JPanel buttonRow = new JPanel(new GridBagLayout());
		buttonRow.setOpaque(false);
		GridBagConstraints btnC = new GridBagConstraints();
		btnC.insets = new Insets(0, 5, 0, 5);
		btnC.fill = GridBagConstraints.HORIZONTAL;

		stylePrimaryButton(addProgramBtn);
		addProgramBtn.addActionListener(e -> onAddProgram());
		buttonRow.add(addProgramBtn, btnC);

		btnC.gridx = 1;
		styleSecondaryButton(editProgramBtn);
		editProgramBtn.addActionListener(e -> onEditProgram());
		buttonRow.add(editProgramBtn, btnC);

		btnC.gridx = 2;
		styleDangerButton(deleteProgramBtn);
		deleteProgramBtn.addActionListener(e -> onDeleteProgram());
		buttonRow.add(deleteProgramBtn, btnC);

		card.add(buttonRow, c);

		return card;
	}

	private JPanel buildTableCard() {
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(Color.WHITE);
		configureTable(programTable);
		programTable.getSelectionModel().addListSelectionListener(new ProgramSelectionListener());

		JScrollPane scrollPane = new JScrollPane(programTable);
		scrollPane.getViewport().setBackground(Color.WHITE);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

		card.add(scrollPane, BorderLayout.CENTER);
		return card;
	}

	private JPanel createCardPanel(java.awt.LayoutManager layout) {
		RoundedPanel card = new RoundedPanel(22, CARD_BORDER);
		card.setBackground(Color.WHITE);
		card.setLayout(layout);
		card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
		return card;
	}

	private GridBagConstraints defaultConstraints() {
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(8, 10, 8, 10);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		return c;
	}

	private void addFormRow(JPanel panel, GridBagConstraints template, int row, String label,
			java.awt.Component component) {
		GridBagConstraints labelConstraints = (GridBagConstraints) template.clone();
		labelConstraints.gridx = 0;
		labelConstraints.gridy = row;
		labelConstraints.gridwidth = 1;
		labelConstraints.anchor = GridBagConstraints.WEST;
		JLabel lbl = new JLabel(label);
		lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		lbl.setForeground(TEXT_PRIMARY);
		lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
		panel.add(lbl, labelConstraints);

		GridBagConstraints fieldConstraints = (GridBagConstraints) template.clone();
		fieldConstraints.gridx = 1;
		fieldConstraints.gridy = row;
		fieldConstraints.weightx = 1;
		panel.add(component, fieldConstraints);
	}

	private void configureTable(JTable table) {
		table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		table.setRowHeight(32);
		table.setShowGrid(true);
		table.setGridColor(new Color(233, 236, 239));
		table.setSelectionBackground(new Color(232, 232, 255));
		table.setSelectionForeground(new Color(33, 37, 41));
		table.setFillsViewportHeight(true);
		table.setBackground(Color.WHITE);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
		for (int i = 1; i < table.getColumnCount(); i++) {
			table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
		}

		JTableHeader header = table.getTableHeader();
		header.setFont(new Font("Segoe UI Semibold", Font.BOLD, 11));
		header.setBackground(new Color(248, 249, 250));
		header.setForeground(new Color(73, 80, 87));
		header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
		header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(222, 226, 230)));
	}

	private void stylePrimaryButton(JButton button) {
		button.setBackground(PRIMARY);
		button.setForeground(Color.WHITE);
		button.setFont(new Font("Segoe UI", Font.BOLD, 13));
		button.setFocusPainted(false);
		button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	private void styleSecondaryButton(JButton button) {
		button.setBackground(SECONDARY);
		button.setForeground(Color.WHITE);
		button.setFont(new Font("Segoe UI", Font.BOLD, 13));
		button.setFocusPainted(false);
		button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	private void styleDangerButton(JButton button) {
		button.setBackground(new Color(220, 53, 69));
		button.setForeground(Color.WHITE);
		button.setFont(new Font("Segoe UI", Font.BOLD, 13));
		button.setFocusPainted(false);
		button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	private void configureLogoutButton() {
		styleSecondaryButton(logoutButton);
		logoutButton.setText("Log Out");
		logoutButton.setPreferredSize(new Dimension(110, 32));
		for (var listener : logoutButton.getActionListeners()) {
			logoutButton.removeActionListener(listener);
		}
		logoutButton.addActionListener(e -> performLogout());
	}

	private void styleLinkButton(JButton button) {
		button.setForeground(PRIMARY.darker());
		button.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		button.setContentAreaFilled(false);
		button.setBorderPainted(false);
		button.setFocusPainted(false);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	private void handleAddCategory() {
		String name = JOptionPane.showInputDialog(this, "Enter new category name:", "Add Category",
				JOptionPane.PLAIN_MESSAGE);
		if (name == null || name.trim().isEmpty()) {
			return;
		}
		String description = JOptionPane.showInputDialog(this, "Optional description for " + name + ":", "");
		try {
			var created = adminService.createCategory(name, description);
			refreshAllData();
			setStatus("Category \"" + created.getName() + "\" added.");
		} catch (SecurityException ex) {
			showError("Access denied: " + ex.getMessage());
		} catch (IllegalArgumentException ex) {
			showError(ex.getMessage());
		} catch (SQLException ex) {
			showError("Unable to add category: " + ex.getMessage());
		}
	}

	private void onAddProgram() {
		String name = programNameField.getText().trim();
		String salaryText = programSalaryField.getText().trim();
		if (name.isEmpty() || salaryText.isEmpty()) {
			showError("Program name and salary are required.");
			return;
		}
		try {
			ProgramCategory category = resolveCategorySelection();
			double salary = Double.parseDouble(salaryText);
			double minGpa = ((Double) programMinGpaSpinner.getValue()).doubleValue();
			double postGpa = ((Double) programPostGpaSpinner.getValue()).doubleValue();
			Program.InterestLevel interest = (Program.InterestLevel) programInterestCombo.getSelectedItem();

			adminService.addProgram(name, category, salary, minGpa, interest, postGpa);
			clearProgramForm();
			refreshAllData();
			setStatus("Program \"" + name + "\" added.");
		} catch (NumberFormatException ex) {
			showError("Salary must be a numeric value.");
		} catch (IllegalArgumentException ex) {
			showError(ex.getMessage());
		} catch (SecurityException ex) {
			showError("Access denied: " + ex.getMessage());
		} catch (SQLException ex) {
			showError("Unable to add program: " + ex.getMessage());
		}
	}

	private void onEditProgram() {
		int row = programTable.getSelectedRow();
		if (row < 0) {
			showError("Select a program to edit.");
			return;
		}
		Program program = programCache.get(row);
		try {
			ProgramCategory category = resolveCategorySelection();
			double salary = Double.parseDouble(programSalaryField.getText().trim());
			double minGpa = ((Double) programMinGpaSpinner.getValue()).doubleValue();
			double postGpa = ((Double) programPostGpaSpinner.getValue()).doubleValue();
			Program.InterestLevel interest = (Program.InterestLevel) programInterestCombo.getSelectedItem();

			adminService.updateProgram(program.getId(), programNameField.getText().trim(), category, salary, minGpa,
					interest, postGpa);
			refreshAllData();
			setStatus("Program updated.");
		} catch (NumberFormatException ex) {
			showError("Salary must be numeric.");
		} catch (IllegalArgumentException ex) {
			showError(ex.getMessage());
		} catch (SecurityException ex) {
			showError("Access denied: " + ex.getMessage());
		} catch (SQLException ex) {
			showError("Unable to update program: " + ex.getMessage());
		}
	}

	private void onDeleteProgram() {
		int row = programTable.getSelectedRow();
		if (row < 0) {
			showError("Select a program to delete.");
			return;
		}
		Program program = programCache.get(row);
		int confirm = JOptionPane.showConfirmDialog(this,
				"Delete program \"" + program.getName() + "\"?",
				"Confirm Delete", JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION) {
			return;
		}
		try {
			adminService.deleteProgram(program.getId());
			refreshAllData();
			setStatus("Program deleted.");
		} catch (SecurityException ex) {
			showError("Access denied: " + ex.getMessage());
		} catch (SQLException ex) {
			showError("Unable to delete program: " + ex.getMessage());
		}
	}

	private void refreshAllData() {
		categoryCache = adminService.listCategories();
		programCache = adminService.listPrograms();
		updateProgramCategoryCombo();
		refreshProgramTable();
		updateStatsCards();
		clearProgramForm();
	}

	private void updateProgramCategoryCombo() {
		DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
		for (ProgramCategory c : categoryCache) {
			model.addElement(c);
		}
		programCategoryCombo.setModel(model);
		programCategoryCombo.setEditable(true);
		if (model.getSize() > 0) {
			programCategoryCombo.setSelectedIndex(0);
		}
	}

	private void refreshProgramTable() {
		programTableModel.setRowCount(0);
		for (Program p : programCache) {
			programTableModel.addRow(new Object[] {
					p.getName(),
					p.getCategory(),
					String.format("%.0f", p.getMinIndustrySalary()),
					String.format("%.2f", p.getMinRequiredPreviousGPA()),
					p.getAnalyticalInterestRequired().name(),
					String.format("%.2f", p.getRequiredAcceptableGPAAfterDegree())
			});
		}
	}

	private void clearProgramForm() {
		programNameField.setText("");
		programSalaryField.setText("");
		programMinGpaSpinner.setValue(3.0);
		programPostGpaSpinner.setValue(3.5);
		programInterestCombo.setSelectedIndex(0);
		if (programCategoryCombo.getItemCount() > 0) {
			programCategoryCombo.setSelectedIndex(0);
		}
	}

	private void showError(String message) {
		JOptionPane.showMessageDialog(this, message, "Admin Console", JOptionPane.ERROR_MESSAGE);
	}

	private void setStatus(String message) {
		statusLabel.setText(message);
	}

	private JPanel createHeroPanel() {
		GradientPanel hero = new GradientPanel(new Color(92, 90, 208), new Color(126, 124, 233), 26);
		hero.setLayout(new BorderLayout());
		hero.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));

		JPanel textStack = new JPanel();
		textStack.setOpaque(false);
		textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));

		JLabel heading = new JLabel("Welcome back, " + adminUser.getUsername() + "!");
		heading.setFont(new Font("Segoe UI", Font.BOLD, 24));
		heading.setForeground(Color.WHITE);
		textStack.add(heading);
		textStack.add(Box.createVerticalStrut(6));

		JLabel subheading = new JLabel("Manage academic programs and categories with confidence.");
		subheading.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		subheading.setForeground(new Color(255, 255, 255, 220));
		textStack.add(subheading);

		hero.add(textStack, BorderLayout.CENTER);

		RoundedPanel badge = new RoundedPanel(30, new Color(255, 255, 255, 80));
		badge.setBackground(new Color(255, 255, 255, 40));
		badge.setBorder(BorderFactory.createEmptyBorder(15, 18, 10, 24));
		badge.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));

		JLabel badgeIcon = new JLabel("🛡");
		badgeIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
		badgeIcon.setForeground(Color.WHITE);

		JLabel badgeText = new JLabel("ADMIN DASHBOARD");
		badgeText.setFont(new Font("Segoe UI", Font.BOLD, 13));
		badgeText.setForeground(Color.WHITE);

		badge.add(badgeIcon);
		badge.add(badgeText);
		hero.add(badge, BorderLayout.EAST);

		return hero;
	}

	private JPanel buildStatsPanel() {
		JPanel panel = new JPanel(new GridLayout(1, 3, 15, 0));
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));

		panel.add(createStatCard("Programs", totalProgramsValue, "Active degree offerings",
				new Color(92, 90, 208)));
		panel.add(createStatCard("Categories", totalCategoriesValue, "Distinct specialization tracks",
				new Color(56, 193, 114)));
		panel.add(createStatCard("Avg. Min Salary", avgSalaryValue, "Across all programs",
				new Color(255, 153, 51)));

		return panel;
	}

	private JPanel createStatCard(String title, JLabel valueLabel, String helperText, Color accent) {
		RoundedPanel card = new RoundedPanel(20, new Color(232, 236, 244));
		card.setBackground(Color.WHITE);
		card.setLayout(new BorderLayout());
		card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

		JLabel titleLabel = new JLabel(title.toUpperCase());
		titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
		titleLabel.setForeground(accent.darker());

		JLabel accentDot = new JLabel("●");
		accentDot.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		accentDot.setForeground(accent);

		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		header.add(titleLabel, BorderLayout.WEST);
		header.add(accentDot, BorderLayout.EAST);

		card.add(header, BorderLayout.NORTH);

		valueLabel.setForeground(TEXT_PRIMARY);
		card.add(valueLabel, BorderLayout.CENTER);

		JLabel helper = new JLabel(helperText);
		helper.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		helper.setForeground(TEXT_MUTED);
		helper.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
		card.add(helper, BorderLayout.SOUTH);

		return card;
	}

	private JLabel createStatValueLabel(String initialValue) {
		JLabel label = new JLabel(initialValue);
		label.setFont(new Font("Segoe UI", Font.BOLD, 26));
		return label;
	}

	private void updateStatsCards() {
		totalProgramsValue.setText(String.valueOf(programCache.size()));
		totalCategoriesValue.setText(String.valueOf(categoryCache.size()));
		double avgSalary = programCache.stream()
				.mapToDouble(Program::getMinIndustrySalary)
				.average()
				.orElse(0);
		avgSalaryValue.setText(String.format("SAR %.0f", avgSalary));
	}

	private class ProgramSelectionListener implements ListSelectionListener {
		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting()) {
				return;
			}
			int row = programTable.getSelectedRow();
			if (row < 0 || row >= programCache.size()) {
				return;
			}
			Program program = programCache.get(row);
			programNameField.setText(program.getName());
			programSalaryField.setText(String.format("%.0f", program.getMinIndustrySalary()));
			programMinGpaSpinner.setValue(program.getMinRequiredPreviousGPA());
			programPostGpaSpinner.setValue(program.getRequiredAcceptableGPAAfterDegree());
			programInterestCombo.setSelectedItem(program.getAnalyticalInterestRequired());
			for (int i = 0; i < programCategoryCombo.getItemCount(); i++) {
				Object item = programCategoryCombo.getItemAt(i);
				if (item instanceof ProgramCategory cat && cat.getName().equalsIgnoreCase(program.getCategory())) {
					programCategoryCombo.setSelectedIndex(i);
					break;
				}
			}
		}
	}

	private ProgramCategory resolveCategorySelection() throws SQLException {
		Object selected = programCategoryCombo.getSelectedItem();
		if (selected instanceof ProgramCategory) {
			return (ProgramCategory) selected;
		}
		String name = selected == null ? "" : selected.toString().trim();
		if (name.isEmpty()) {
			throw new IllegalArgumentException("Category is required.");
		}
		for (ProgramCategory c : categoryCache) {
			if (c.getName().equalsIgnoreCase(name)) {
				programCategoryCombo.setSelectedItem(c);
				return c;
			}
		}
		ProgramCategory created = adminService.createCategory(name, "");
		setStatus("Category \"" + created.getName() + "\" created automatically.");
		return created;
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

	private static final class RoundedPanel extends JPanel {
		private final int arc;
		private final Color borderColor;

		private RoundedPanel(int arc, Color borderColor) {
			this.arc = arc;
			this.borderColor = borderColor;
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(getBackground());
			g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
			if (borderColor != null) {
				g2.setColor(borderColor);
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
			}
			g2.dispose();
			super.paintComponent(g);
		}
	}

	private static final class GradientPanel extends JPanel {
		private final Color start;
		private final Color end;
		private final int arc;

		private GradientPanel(Color start, Color end, int arc) {
			this.start = start;
			this.end = end;
			this.arc = arc;
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			GradientPaint gradient = new GradientPaint(0, 0, start, getWidth(), getHeight(), end);
			g2.setPaint(gradient);
			g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
			g2.dispose();
			super.paintComponent(g);
		}
	}
}
