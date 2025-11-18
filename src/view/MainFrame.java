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
import java.util.List;

import javax.swing.BorderFactory;
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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import logic.RecommendationEngine;
import model.Program;
import security.User;

public class MainFrame extends JFrame {
	private final JTextField salaryField = new JTextField(15);
	private final JSpinner gpaSpinner = new JSpinner(new SpinnerNumberModel(3.0, 0.0, 4.0, 0.1));
	private final JComboBox<String> interestCombo = new JComboBox<>(
			new String[] { "Low", "Medium", "High", "Very High" });
	private final JButton recommendBtn = new JButton("Find Programs");
	private final JButton clearBtn = new JButton("Clear");
	private final DefaultTableModel tableModel = new DefaultTableModel(
			new String[] { "Program", "Category", "Min Salary", "GPA Required", 
					"Analytical Level", "Post-Degree GPA", "Extra Study Hours/Day" }, 0) {
		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
	};
	private final JTable resultTable = new JTable(tableModel);
	private final JLabel statusLabel = new JLabel(" ");
	private final RecommendationEngine engine;
	private final User user;

	public MainFrame(RecommendationEngine engine, User user) {
		super("Degree Program Recommender");
		this.engine = engine;
		this.user = user;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1300, 400);
		setLocationRelativeTo(null);
		initUI();
		statusLabel.setText("Signed in as " + user.getUsername());
	}

	private void initUI() {
		// Main container with padding
		JPanel mainContainer = new JPanel(new BorderLayout(10, 10));
		mainContainer.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		mainContainer.setBackground(new Color(245, 245, 245));

		// Title panel
		JPanel titlePanel = createTitlePanel();
		
		// Input panel with improved styling
		JPanel inputPanel = createInputPanel();
		
		// Results panel with better formatting
		JPanel resultsPanel = createResultsPanel();
		
		// Status bar at bottom
		JPanel statusPanel = createStatusPanel();

		// Add components
		mainContainer.add(titlePanel, BorderLayout.NORTH);
		mainContainer.add(inputPanel, BorderLayout.WEST);
		mainContainer.add(resultsPanel, BorderLayout.CENTER);
		mainContainer.add(statusPanel, BorderLayout.SOUTH);

		setContentPane(mainContainer);
		
		// Event listeners
		recommendBtn.addActionListener(e -> onRecommend());
		clearBtn.addActionListener(e -> onClear());
	}

	private JPanel createTitlePanel() {
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setHgap(15);
		layout.setVgap(5);
		JPanel panel = new JPanel(layout);
		panel.setBackground(new Color(245, 245, 245));
		
		JLabel titleLabel = new JLabel("Find Your Ideal Degree Program");
		titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
		titleLabel.setForeground(new Color(33, 37, 41));
		
		panel.add(titleLabel);
		
		if (user != null) {
			JLabel userLabel = new JLabel("Welcome, " + user.getUsername());
			userLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
			userLabel.setForeground(new Color(52, 58, 64));
			panel.add(userLabel);
		}
		return panel;
	}

	private JPanel createInputPanel() {
		JPanel container = new JPanel(new BorderLayout());
		container.setPreferredSize(new Dimension(320, 0));
		container.setBackground(Color.WHITE);
		container.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(220, 220, 220)),
			BorderFactory.createEmptyBorder(20, 20, 20, 20)
		));

		JPanel inputPanel = new JPanel(new GridBagLayout());
		inputPanel.setBackground(Color.WHITE);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(8, 5, 8, 5);

		// Header
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		JLabel headerLabel = new JLabel("Your Preferences");
		headerLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
		inputPanel.add(headerLabel, c);

		c.gridwidth = 1;
		c.gridy++;

		// Salary field
		addInputRow(inputPanel, c, "Minimum Salary (SAR):", salaryField, 
				"Enter your desired minimum salary");
		salaryField.setText("5000");
		salaryField.setFont(new Font("SansSerif", Font.PLAIN, 13));

		// GPA spinner
		c.gridy++;
		addInputRow(inputPanel, c, "Previous GPA:", gpaSpinner, 
				"Your current or previous GPA");
		gpaSpinner.setFont(new Font("SansSerif", Font.PLAIN, 13));
		((JSpinner.DefaultEditor) gpaSpinner.getEditor()).getTextField().setHorizontalAlignment(JTextField.CENTER);

		// Interest combo
		c.gridy++;
		addInputRow(inputPanel, c, "Analytical Interest:", interestCombo, 
				"How much do you enjoy analytical work?");
		interestCombo.setSelectedIndex(1);
		interestCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));

		// Button panel
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 2;
		c.insets = new Insets(20, 5, 8, 5);
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
		buttonPanel.setBackground(Color.WHITE);
		
		recommendBtn.setPreferredSize(new Dimension(130, 35));
		recommendBtn.setBackground(new Color(0, 123, 255));
		recommendBtn.setForeground(Color.WHITE);
		recommendBtn.setFocusPainted(false);
		recommendBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
		recommendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		recommendBtn.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(0, 123, 255), 1, true),
			BorderFactory.createEmptyBorder(5, 15, 5, 15)
		));
		
		clearBtn.setPreferredSize(new Dimension(100, 35));
		clearBtn.setBackground(new Color(108, 117, 125));
		clearBtn.setForeground(Color.WHITE);
		clearBtn.setFocusPainted(false);
		clearBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
		clearBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		clearBtn.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(108, 117, 125), 1, true),
			BorderFactory.createEmptyBorder(5, 15, 5, 15)
		));
		
		buttonPanel.add(recommendBtn);
		buttonPanel.add(clearBtn);
		inputPanel.add(buttonPanel, c);

		// Info note
		c.gridy++;
		c.insets = new Insets(20, 5, 5, 5);
		JLabel noteLabel = new JLabel("<html><div style='text-align: center; font-size: 8px; color: #6c757d;'>"
				+ "<b>Note:</b> Extra study hours = max(0, Required GPA - Your GPA)"
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
		JLabel label = new JLabel(labelText);
		label.setFont(new Font("SansSerif", Font.PLAIN, 12));
		label.setForeground(new Color(73, 80, 87));
		panel.add(label, c);

		c.gridx = 1;
		c.weightx = 1;
		((JComponent) component).setToolTipText(tooltip);
		panel.add(component, c);
	}

	private JPanel createResultsPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(new Color(245, 245, 245));
		
		// Configure table
		resultTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
		resultTable.setRowHeight(28);
		resultTable.setShowGrid(true);
		resultTable.setGridColor(new Color(230, 230, 230));
		resultTable.setSelectionBackground(new Color(184, 218, 255));
		resultTable.setFillsViewportHeight(true);
		
		// Header styling
		JTableHeader header = resultTable.getTableHeader();
		header.setFont(new Font("SansSerif", Font.BOLD, 12));
		header.setBackground(new Color(248, 249, 250));
		header.setForeground(new Color(33, 37, 41));
		header.setPreferredSize(new Dimension(header.getPreferredSize().width, 35));
		
		// Center align numeric columns
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
		for (int i = 2; i < resultTable.getColumnCount(); i++) {
			resultTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
		}
		
		JScrollPane scrollPane = new JScrollPane(resultTable);
		scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
		
		panel.add(scrollPane, BorderLayout.CENTER);
		
		return panel;
	}

	private JPanel createStatusPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.setBackground(new Color(248, 249, 250));
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)),
			BorderFactory.createEmptyBorder(5, 10, 5, 10)
		));
		
		statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
		statusLabel.setForeground(new Color(108, 117, 125));
		panel.add(statusLabel);
		
		return panel;
	}

	private void onRecommend() {
		try {
			double salary = Double.parseDouble(salaryField.getText().trim());
			double prevGpa = ((Double) ((SpinnerNumberModel) gpaSpinner.getModel()).getNumber()).doubleValue();
			String interest = (String) interestCombo.getSelectedItem();
			Program.InterestLevel level = Program.InterestLevel.fromString(interest);

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
}