package UI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import javax.swing.event.*;
import Model.Role;
import Model.User;
import Service.UserService;
import Service.PatientService;
import Service.DoctorServiceImpl;
import Model.Doctor;
import java.time.LocalDate;
import Controller.AdminController;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.Arrays;
import java.util.function.BiConsumer;
import UI.DeactivatedAccountsPanel;

public class AdminDashboardPanel extends JPanel implements GlobalSearchable {
    private static final long serialVersionUID = 1L;

    public static boolean validateDateFormat(String date) {

        if (date == null)
            return false;
        if (date.length() != 10)
            return false;

        // Check fixed positions for dashes
        if (date.charAt(4) != '-' || date.charAt(7) != '-')
            return false;

        // Extract year, month, day
        String yearStr = date.substring(0, 4);
        String monthStr = date.substring(5, 7);
        String dayStr = date.substring(8, 10);

        // Ensure all characters are digits
        if (!isNumeric(yearStr) || !isNumeric(monthStr) || !isNumeric(dayStr))
            return false;

        int year = toInt(yearStr);
        int month = toInt(monthStr);
        int day = toInt(dayStr);

        // Basic bounds
        if (year < 1900 || year > 2100)
            return false;
        if (month < 1 || month > 12)
            return false;
        if (day < 1 || day > 31)
            return false;

        // Days in each month
        int[] daysInMonth = {
                31, // Jan
                28, // Feb (handle leap-year separately)
                31, // Mar
                30, // Apr
                31, // May
                30, // Jun
                31, // Jul
                31, // Aug
                30, // Sep
                31, // Oct
                30, // Nov
                31 // Dec
        };

        // Handle leap year for February
        boolean leap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
        if (month == 2) {
            if (leap && day > 29)
                return false;
            if (!leap && day > 28)
                return false;
        } else {
            if (day > daysInMonth[month - 1])
                return false;
        }

        return true;
    }

    // Helper: check numeric manually
    private static boolean isNumeric(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i)))
                return false;
        }
        return true;
    }

    // Helper: convert to int manually (no imports)
    private static int toInt(String s) {
        int value = 0;
        for (int i = 0; i < s.length(); i++) {
            value = value * 10 + (s.charAt(i) - '0');
        }
        return value;
    }

    // controller reference (optional)
    private final AdminController adminController;

    // THEME CONSTANTS
    private static final Color COLOR_BG = Color.WHITE;
    private static final Color COLOR_SIDEBAR_BG = new Color(245, 247, 250);
    private static final Color COLOR_PRIMARY = new Color(60, 120, 200);
    private static final Color COLOR_PRIMARY_HOVER = new Color(80, 140, 220);
    private static final Color COLOR_ACTIVE = new Color(100, 160, 240);
    private static final Color COLOR_BORDER = new Color(210, 215, 220);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONT_NORMAL = new Font("Segoe UI", Font.PLAIN, 16);

    // Title -> Department mapping for doctor titles
    // This mapping is intentionally a LinkedHashMap to preserve insertion order for
    // UI dropdowns.
    private static final Map<String, String> TITLE_TO_DEPARTMENT = buildTitleToDepartmentMap();

    private static Map<String, String> buildTitleToDepartmentMap() {
        Map<String, String> m = new LinkedHashMap<>();

        // Emergency group
        m.put("Emergency Medicine Physician", "Emergency Medicine");
        m.put("ER Doctor", "Emergency Medicine");
        m.put("Trauma Specialist", "Trauma");

        // ICU / Critical care group
        m.put("Intensive Care Unit (ICU)", "Intensive Care");
        m.put("Intensivist", "Intensive Care");
        m.put("Critical Care Specialist", "Intensive Care");

        // Cardiology
        m.put("Cardiologist", "Cardiology");
        m.put("Interventional Cardiologist", "Cardiology");

        // Neurology
        m.put("Neurologist", "Neurology");
        m.put("Neurophysician", "Neurology");

        // Pediatrics
        m.put("Pediatrician", "Pediatrics");
        m.put("Pediatric Specialist", "Pediatrics");

        // Obstetrics & Gynecology
        m.put("Obstetrician-Gynecologist (OB-GYN)", "Obstetrics & Gynecology");
        m.put("Maternal-Fetal Medicine Specialist", "Obstetrics & Gynecology");

        // Orthopedics
        m.put("Orthopedic Surgeon", "Orthopedics");
        m.put("Pediatric Orthopedist", "Orthopedics");
        m.put("Orthopedist", "Orthopedics");
        m.put("Orthopedic Trauma Surgeon", "Orthopedics");

        // Nephrology / Dialysis
        m.put("Nephrologist", "Nephrology");
        m.put("Renal Specialist", "Nephrology");
        m.put("Kidney Disease Specialist", "Nephrology");
        m.put("Transplant Nephrologist", "Nephrology");

        m.put("Pulmonologist", "Pulmonology");
        m.put("Respiratory Medicine Specialist", "Pulmonology");

        return m;
    }

    // Layout + navigation
    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private JPanel sideNavPanel;
    private JButton btnDashboard;
    private JButton btnUsers;
    private JButton btnPayments;
    private JButton btnSummary;
    private JButton btnDeactivated; // new nav for deactivated accounts
    private JButton activeButton;
    private JButton btnAdmissionDischarge;
    private JButton btnHospitalService;
    // Management panel references so we can trigger reloads after create/update
    private DoctorManagementPanel doctorPanel;
    private StaffManagementPanel staffPanel;
    private PatientManagementPanel patientPanel;

    // Dashboard dynamic labels
    private JLabel lblPatientsValue;
    private JLabel lblDoctorsValue;
    private JLabel lblStaffValue;
    // revenue stat removed per request
    // private JLabel lblRevenueValue;

    // Tables (exposed for future data binding)
    private JTable userTable;
    private JTable paymentTable;
    // Global search/filter state
    private String globalSearchQuery;
    private final Map<String, Map<String, String>> columnFilters = new HashMap<>();
    private final UserService userService = UserService.getInstance();
    private final String currentUsername;
    // NEW: username label reference to control visibility
    private JLabel userTagLabel;

    public AdminDashboardPanel() {
        this(null, null);
    }

    public AdminDashboardPanel(String username) {
        this(null, username);
    }

    public AdminDashboardPanel(AdminController controller, String username) {
        this.adminController = controller;
        this.currentUsername = username;
        setBackground(COLOR_BG);
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setLayout(new BorderLayout(8, 8));

        add(createHeader(), BorderLayout.NORTH);
        add(createSideBar(), BorderLayout.WEST);
        add(createMainContent(), BorderLayout.CENTER);

        // Default view
        setActiveButton(btnDashboard, "OVERVIEW");
    }

    // HEADER BAR -------------------------------------------------------
    private JComponent createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new LineBorder(COLOR_BORDER));
        header.setBackground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 60));

        // Left container: title only
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        left.setOpaque(false);
        userTagLabel = null;

        JLabel title = new JLabel("Admin Overview");
        title.setFont(FONT_TITLE);
        title.setForeground(COLOR_PRIMARY.darker());
        left.add(title);

        // Right: dynamic actions
        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        topRight.setOpaque(false);
        // persistent refresh button
        JButton btnRefresh = new JButton("Refresh");
        styleSecondaryButton(btnRefresh);
        btnRefresh.addActionListener(e -> JOptionPane.showMessageDialog(this, "Data refreshed (placeholder)", "Info",
                JOptionPane.INFORMATION_MESSAGE));
        topRight.add(btnRefresh);

        right.add(topRight, BorderLayout.EAST);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    // SIDEBAR ----------------------------------------------------------
    private JComponent createSideBar() {
        sideNavPanel = new JPanel();
        sideNavPanel.setLayout(new BoxLayout(sideNavPanel, BoxLayout.Y_AXIS));
        sideNavPanel.setBackground(COLOR_SIDEBAR_BG);
        sideNavPanel.setBorder(new LineBorder(COLOR_BORDER));
        sideNavPanel.setPreferredSize(new Dimension(260, 0));

        // Dito magaadd ng buttons para sa side bar
        btnDashboard = createNavButton("Overview", "OVERVIEW");
        btnUsers = createNavButton("User Management", "USERS");
        btnPayments = createNavButton("Payments", "PAYMENTS");
        btnSummary = createNavButton("Summary", "SUMMARY");
        btnDeactivated = createNavButton("Deactivated Accounts", "DEACTIVATED");
        // hospital logs removed
        JButton btnDoctors = createNavButton("Doctor Management", "DOCTORS");
        JButton btnStaffMgmt = createNavButton("Staff Management", "STAFF_MGMT");
        JButton btnPatientMgmt = createNavButton("Patient Management", "PATIENT_MGMT");
        btnPayments = createNavButton("Billing History", "PAYMENTS");
        btnAdmissionDischarge = createNavButton("Admission & Discharge", "ADMISSION_DISCHARGE");
        btnHospitalService = createNavButton("Hospital Services", "HOSPITAL_SERVICE");
        // btnSummary = createNavButton("Summary", "SUMMARY");
        btnDeactivated = createNavButton("Deactivated Accounts", "DEACTIVATED");
        // hospital logs removed
        int gap = 12;

        sideNavPanel.add(Box.createVerticalStrut(gap));
        sideNavPanel.add(btnDashboard);
        sideNavPanel.add(Box.createVerticalStrut(gap));
        sideNavPanel.add(btnUsers);
        sideNavPanel.add(Box.createVerticalStrut(gap));
        sideNavPanel.add(btnDoctors);
        sideNavPanel.add(Box.createVerticalStrut(gap));
        sideNavPanel.add(btnStaffMgmt);
        sideNavPanel.add(Box.createVerticalStrut(gap));
        sideNavPanel.add(btnPatientMgmt);
        sideNavPanel.add(Box.createVerticalStrut(gap));
        sideNavPanel.add(btnPayments);
        sideNavPanel.add(Box.createVerticalStrut(gap));
        sideNavPanel.add(btnAdmissionDischarge);
        sideNavPanel.add(Box.createVerticalStrut(gap));
        sideNavPanel.add(btnHospitalService);
        sideNavPanel.add(Box.createVerticalStrut(gap));
        sideNavPanel.add(btnDeactivated);
        sideNavPanel.add(Box.createVerticalStrut(gap));
        // sideNavPanel.add(btnSummary); sideNavPanel.add(Box.createVerticalStrut(gap));
        return sideNavPanel;
    }

    private JButton createNavButton(String text, String card) {
        JButton b = new JButton(text);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        b.setPreferredSize(new Dimension(Integer.MAX_VALUE, 64));
        b.setFont(FONT_NORMAL);
        b.setBackground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(new LineBorder(COLOR_BORDER));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (b != activeButton)
                    b.setBackground(COLOR_PRIMARY_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (b != activeButton)
                    b.setBackground(Color.WHITE);
            }
        });
        b.addActionListener(e -> setActiveButton(b, card));
        return b;
    }

    private void setActiveButton(JButton button, String card) {
        if (activeButton != null) {
            activeButton.setBackground(Color.WHITE);
            activeButton.setForeground(Color.BLACK);
        }
        activeButton = button;
        activeButton.setBackground(COLOR_ACTIVE);
        activeButton.setForeground(Color.WHITE);
        // Username no longer shown
        cardLayout.show(mainContentPanel, card);
    }

    // MAIN CONTENT -----------------------------------------------------
    private JComponent createMainContent() {
        mainContentPanel = new JPanel();
        cardLayout = new CardLayout();
        mainContentPanel.setLayout(cardLayout);
        mainContentPanel.setBorder(new LineBorder(COLOR_BORDER));

        mainContentPanel.add(buildDashboardPanel(), "OVERVIEW");
        mainContentPanel.add(buildUserPanel(), "USERS");
        mainContentPanel.add(buildPaymentPanel(), "PAYMENTS");
        mainContentPanel.add(buildAdmissionDischargePanel(), "ADMISSION_DISCHARGE");
        mainContentPanel.add(buildHospitalServicePanel(), "HOSPITAL_SERVICE");
        // Provide a callback so when an account is reactivated the users table
        // refreshes
        mainContentPanel.add(new DeactivatedAccountsPanel(this::reloadUsersTable), "DEACTIVATED");
        // hospital logs panel removed
        // Doctor management
        doctorPanel = new DoctorManagementPanel();
        mainContentPanel.add(doctorPanel, "DOCTORS");
        // Staff and Patient management panels
        staffPanel = new StaffManagementPanel();
        patientPanel = new PatientManagementPanel();
        mainContentPanel.add(staffPanel, "STAFF_MGMT");
        mainContentPanel.add(patientPanel, "PATIENT_MGMT");
        // Hospital Services panel (Department Management)
        mainContentPanel.add(new HospitalServicesPanel(), "HOSPITAL_SERVICE");
        return mainContentPanel;
    }

    // DASHBOARD PANEL --------------------------------------------------
    private JPanel buildDashboardPanel() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(COLOR_BG);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel header = new JLabel("Overview", SwingConstants.LEFT);
        header.setFont(FONT_SECTION);
        header.setForeground(COLOR_PRIMARY.darker());
        header.setBorder(new EmptyBorder(0, 0, 8, 0));
        root.add(header, BorderLayout.NORTH);

        // show three summary stat cards (patients, doctors, staff)
        JPanel statsGrid = new JPanel(new GridLayout(1, 3, 12, 12));
        statsGrid.setOpaque(false);

        lblPatientsValue = new JLabel("0", SwingConstants.CENTER);
        lblDoctorsValue = new JLabel("0", SwingConstants.CENTER);
        lblStaffValue = new JLabel("0", SwingConstants.CENTER);

        statsGrid.add(createStatCard("Patients", lblPatientsValue));
        statsGrid.add(createStatCard("Doctors", lblDoctorsValue));
        statsGrid.add(createStatCard("Staff", lblStaffValue));

        root.add(statsGrid, BorderLayout.CENTER);

        JTextArea info = new JTextArea(
                "Welcome to the Overview.\nUse sidebar navigation to manage users, review payments, and view summaries.");
        info.setFont(FONT_NORMAL);
        info.setEditable(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setBorder(new EmptyBorder(8, 12, 8, 12));
        root.add(new JScrollPane(info), BorderLayout.SOUTH);
        return root;
    }

    private JPanel createStatCard(String label, JLabel valueLabel) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new LineBorder(COLOR_BORDER));
        wrapper.setPreferredSize(new Dimension(160, 120));

        JLabel title = new JLabel(label, SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        title.setForeground(COLOR_PRIMARY.darker());
        title.setBorder(new EmptyBorder(6, 6, 0, 6));
        wrapper.add(title, BorderLayout.NORTH);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(COLOR_PRIMARY);
        wrapper.add(valueLabel, BorderLayout.CENTER);

        return wrapper;
    }

    // USER MANAGEMENT PANEL --------------------------------------------
    private JPanel buildUserPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(COLOR_BG);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Top area: header (left), search (center), actions (right)
        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.setOpaque(false);
        JLabel header = new JLabel("User Management", SwingConstants.LEFT);
        header.setFont(FONT_SECTION);
        header.setForeground(COLOR_PRIMARY.darker());
        header.setBorder(new EmptyBorder(0, 0, 0, 0));
        topPanel.add(header, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setOpaque(false);
        searchPanel.add(new JLabel("Search Users:"));
        JTextField searchField = new JTextField(20);
        searchPanel.add(searchField);
        topPanel.add(searchPanel, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setOpaque(false);
        JButton bAdd = new JButton("Add User");
        styleSecondaryButton(bAdd);
        bAdd.addActionListener(e -> openAddUserDialog());
        JButton bDeactivate = new JButton("Deactivate");
        styleSecondaryButton(bDeactivate);
        bDeactivate.addActionListener(e -> openDeactivateUserDialog());
        JButton bExport = new JButton("Export");
        styleSecondaryButton(bExport);
        bExport.addActionListener(e -> openExportDialog());
        actionPanel.add(bAdd);
        actionPanel.add(bDeactivate);
        actionPanel.add(bExport);
        topPanel.add(actionPanel, BorderLayout.EAST);

        root.add(topPanel, BorderLayout.NORTH);

        String[] cols = { "Username", "Role" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        userTable = new JTable(model);
        // load users from service
        reloadUsersTable();

        // Add search listener
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filterUserTable(searchField.getText());
            }

            public void removeUpdate(DocumentEvent e) {
                filterUserTable(searchField.getText());
            }

            public void changedUpdate(DocumentEvent e) {
                filterUserTable(searchField.getText());
            }
        });

        root.add(new JScrollPane(userTable), BorderLayout.CENTER);
        return root;
    }

    private void reloadUsersTable() {
        DefaultTableModel model = (DefaultTableModel) userTable.getModel();
        model.setRowCount(0);
        for (User u : userService.getAllUsers()) {
            model.addRow(new Object[] { u.getUsername(), u.getRole().name() });
        }
    }

    // PAYMENT PANEL ----------------------------------------------------
    private JPanel buildPaymentPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(COLOR_BG);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.setOpaque(false);
        JLabel header = new JLabel("Payment History", SwingConstants.LEFT);
        header.setFont(FONT_SECTION);
        header.setForeground(COLOR_PRIMARY.darker());
        topPanel.add(header, BorderLayout.WEST);
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setOpaque(false);
        searchPanel.add(new JLabel("Search Payments:"));
        JTextField searchField = new JTextField(20);
        searchPanel.add(searchField);
        topPanel.add(searchPanel, BorderLayout.CENTER);
        JPanel actionPanelPay = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanelPay.setOpaque(false);
        JButton btnExport = new JButton("Export CSV");
        styleSecondaryButton(btnExport);
        btnExport.addActionListener(e -> openExportPaymentDialog());
        actionPanelPay.add(btnExport);
        topPanel.add(actionPanelPay, BorderLayout.EAST);
        root.add(topPanel, BorderLayout.NORTH);

        String[] cols = { "Date", "Name", "Amount", "Description" };
        Object[][] data = { { "2025-01-10", "Patient A", "$120.00", "Consultation" },
                { "2025-01-11", "Patient B", "$450.00", "Procedure" } };
        paymentTable = new JTable(new DefaultTableModel(data, cols));

        // Add search listener
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filterPaymentTable(searchField.getText());
            }

            public void removeUpdate(DocumentEvent e) {
                filterPaymentTable(searchField.getText());
            }

            public void changedUpdate(DocumentEvent e) {
                filterPaymentTable(searchField.getText());
            }
        });

        root.add(new JScrollPane(paymentTable), BorderLayout.CENTER);
        return root;
    }

    // ADMISSION & DISCHARGE PANEL ---------------------------------------

    private JPanel buildAdmissionDischargePanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(COLOR_BG);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.setOpaque(false);
        JLabel header = new JLabel("Admission & Discharge Management", SwingConstants.LEFT);
        header.setFont(FONT_SECTION);
        header.setForeground(COLOR_PRIMARY.darker());
        topPanel.add(header, BorderLayout.WEST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setOpaque(false);
        JButton btnAddAdmission = new JButton("Add Admission");
        styleSecondaryButton(btnAddAdmission);
        btnAddAdmission.addActionListener(e -> JOptionPane.showMessageDialog(this, "Add Admission feature coming soon!",
                "Info", JOptionPane.INFORMATION_MESSAGE));
        // add admission details

        JButton btnExport = new JButton("Export Records");
        styleSecondaryButton(btnExport);

        btnExport.addActionListener(e -> JOptionPane.showMessageDialog(this, "Export feature coming soon!", "Info",
                JOptionPane.INFORMATION_MESSAGE));

        actionPanel.add(btnAddAdmission);
        actionPanel.add(btnExport);

        topPanel.add(actionPanel, BorderLayout.EAST);

        root.add(topPanel, BorderLayout.NORTH);

        JTextArea info = new JTextArea(
                "Manage patient admissions and discharges here.\n\nPlaceholder content:\n- Track admission dates, reasons, and assigned rooms.\n- Record discharge summaries and follow-ups.\n- Integrate with patient records for seamless workflow.\n\nAdd tables, search, and data binding as needed.");

        info.setFont(FONT_NORMAL);

        info.setEditable(false);

        info.setLineWrap(true);

        info.setWrapStyleWord(true);

        root.add(new JScrollPane(info), BorderLayout.CENTER);

        return root;

    }

    // HOSPITAL SERVICE PANEL -------------------------------------------

    private JPanel buildHospitalServicePanel() {

        JPanel root = new JPanel(new BorderLayout(8, 8));

        root.setBackground(COLOR_BG);

        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.setOpaque(false);

        JLabel header = new JLabel("Hospital Services", SwingConstants.LEFT);

        header.setFont(FONT_SECTION);

        header.setForeground(COLOR_PRIMARY.darker());

        topPanel.add(header, BorderLayout.WEST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setOpaque(false);

        JButton btnManageRooms = new JButton("Manage Rooms");
        styleSecondaryButton(btnManageRooms);

        btnManageRooms.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Room management feature coming soon!", "Info", JOptionPane.INFORMATION_MESSAGE));

        JButton btnExport = new JButton("Export Services");
        styleSecondaryButton(btnExport);

        btnExport.addActionListener(e -> JOptionPane.showMessageDialog(this, "Export feature coming soon!", "Info",
                JOptionPane.INFORMATION_MESSAGE));

        actionPanel.add(btnManageRooms);
        actionPanel.add(btnExport);

        topPanel.add(actionPanel, BorderLayout.EAST);

        root.add(topPanel, BorderLayout.NORTH);

        JTextArea info = new JTextArea(
                "Oversee hospital services and resources.\n\nPlaceholder content:\n- Manage room availability and assignments.\n- Track equipment, maintenance, and supplies.\n- Monitor service requests and schedules.\n\nAdd tables, search, and data binding as needed.");

        info.setFont(FONT_NORMAL);

        info.setEditable(false);

        info.setLineWrap(true);

        info.setWrapStyleWord(true);

        root.add(new JScrollPane(info), BorderLayout.CENTER);

        return root;

    }

    // PUBLIC API -------------------------------------------------------
    public void updateStats(int patients, int doctors, int staff) {
        if (lblPatientsValue != null)
            lblPatientsValue.setText(String.valueOf(patients));
        if (lblDoctorsValue != null)
            lblDoctorsValue.setText(String.valueOf(doctors));
        if (lblStaffValue != null)
            lblStaffValue.setText(String.valueOf(staff));
    }

    public JTable getUserTable() {
        return userTable;
    }

    public JTable getPaymentTable() {
        return paymentTable;
    }

    // GLOBAL SEARCH IMPLEMENTATION ------------------------------------
    @Override
    public Map<String, JTable> getSearchableTables() {
        Map<String, JTable> map = new LinkedHashMap<>();
        if (userTable != null)
            map.put("users", userTable);
        if (paymentTable != null)
            map.put("payments", paymentTable);
        return map;
    }

    @Override
    public void applyGlobalSearch(String query) {
        globalSearchQuery = (query == null || query.isBlank()) ? null : query.trim();
        refreshAllFilters();
    }

    @Override
    public void clearGlobalSearch() {
        globalSearchQuery = null;
        refreshAllFilters();
    }

    @Override
    public void applyGlobalFilter(String tableName, String columnName, String value) {
        if (tableName == null || columnName == null)
            return;
        Map<String, String> map = columnFilters.computeIfAbsent(tableName, k -> new HashMap<>());
        if (value == null || value.isBlank()) {
            map.remove(columnName);
            if (map.isEmpty())
                columnFilters.remove(tableName);
        } else {
            map.put(columnName, value.trim());
        }
        JTable table = getSearchableTables().get(tableName);
        if (table != null)
            applyFiltersToTable(tableName, table);
    }

    @Override
    public void clearGlobalFilter() {
        columnFilters.clear();
        refreshAllFilters();
    }

    private void refreshAllFilters() {
        getSearchableTables().forEach(this::applyFiltersToTable);
    }

    @SuppressWarnings("unchecked")
    private void applyFiltersToTable(String logicalName, JTable table) {
        if (table.getRowSorter() == null) {
            table.setRowSorter(new TableRowSorter<>(table.getModel()));
        }
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) table.getRowSorter();
        List<RowFilter<TableModel, Object>> filters = new ArrayList<>();
        if (globalSearchQuery != null) {
            final String q = globalSearchQuery.toLowerCase();
            filters.add(new RowFilter<TableModel, Object>() {
                @Override
                public boolean include(Entry<? extends TableModel, ? extends Object> entry) {
                    int cols = entry.getValueCount();
                    for (int i = 0; i < cols; i++) {
                        Object v = entry.getValue(i);
                        if (v != null && v.toString().toLowerCase().contains(q))
                            return true;
                    }
                    return false;
                }
            });
        }
        Map<String, String> colMap = columnFilters.get(logicalName);
        if (colMap != null) {
            for (Map.Entry<String, String> e : colMap.entrySet()) {
                String colName = e.getKey();
                String val = e.getValue();
                if (val == null || val.isBlank())
                    continue;
                int colIndex;
                try {
                    colIndex = table.getColumnModel().getColumnIndex(colName);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                final String qv = val.toLowerCase();
                filters.add(new RowFilter<TableModel, Object>() {
                    @Override
                    public boolean include(Entry<? extends TableModel, ? extends Object> entry) {
                        Object v = entry.getValue(colIndex);
                        return v != null && v.toString().toLowerCase().contains(qv);
                    }
                });
            }
        }
        if (filters.isEmpty()) {
            sorter.setRowFilter(null);
        } else if (filters.size() == 1) {
            sorter.setRowFilter(filters.get(0));
        } else {
            sorter.setRowFilter(RowFilter.andFilter(filters));
        }
    }

    // HELPERS & PLACEHOLDERS -----------------------------------------
    private int showDialog(JPanel panel, String title) {
        return JOptionPane.showConfirmDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
    }

    private void styleSecondaryButton(JButton b) {
        b.setFont(FONT_NORMAL);
        b.setBackground(Color.WHITE);
        b.setBorder(new LineBorder(COLOR_BORDER));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(COLOR_PRIMARY_HOVER);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(Color.WHITE);
            }
        });
    }

    private void filterUserTable(String query) {
        if (userTable == null)
            return;
        if (userTable.getRowSorter() == null)
            userTable.setRowSorter(new TableRowSorter<>(userTable.getModel()));
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) userTable.getRowSorter();
        if (query == null || query.isBlank()) {
            sorter.setRowFilter(null);
            return;
        }
        final String q = "(?i).*" + java.util.regex.Pattern.quote(query.trim()) + ".*";
        sorter.setRowFilter(RowFilter.regexFilter(q));
    }

    private void filterPaymentTable(String query) {
        if (paymentTable == null)
            return;
        if (paymentTable.getRowSorter() == null)
            paymentTable.setRowSorter(new TableRowSorter<>(paymentTable.getModel()));
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) paymentTable.getRowSorter();
        if (query == null || query.isBlank()) {
            sorter.setRowFilter(null);
            return;
        }
        final String q = "(?i).*" + java.util.regex.Pattern.quote(query.trim()) + ".*";
        sorter.setRowFilter(RowFilter.regexFilter(q));
    }

    private void openEditUserDialog() {
        int r = userTable.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a user to edit.", "Edit User", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String uname = userTable.getValueAt(userTable.convertRowIndexToModel(r), 0).toString();
        JOptionPane.showMessageDialog(this, "Edit user feature not implemented yet for: " + uname, "Edit User",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void openResetPasswordDialog() {
        int r = userTable.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a user to reset password.", "Reset Password",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String uname = userTable.getValueAt(userTable.convertRowIndexToModel(r), 0).toString();
        userService.findByUsername(uname).ifPresentOrElse(u -> {
            int ok = JOptionPane.showConfirmDialog(this, "Reset password for " + uname + "?", "Reset Password",
                    JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                String newPw = userService.generateNextPlainPassword();
                boolean changed = userService.resetPasswordById(u.getId(), newPw.toCharArray());
                if (changed)
                    JOptionPane.showMessageDialog(this, "Password reset. New password: " + newPw, "Password Reset",
                            JOptionPane.INFORMATION_MESSAGE);
                else
                    JOptionPane.showMessageDialog(this, "Failed to reset password.", "Error",
                            JOptionPane.ERROR_MESSAGE);
            }
        }, () -> JOptionPane.showMessageDialog(this, "User not found: " + uname, "Error", JOptionPane.ERROR_MESSAGE));
    }

    private void openDeactivateUserDialog() {
        int r = userTable.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a user to deactivate.", "Deactivate User",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String uname = userTable.getValueAt(userTable.convertRowIndexToModel(r), 0).toString();
        userService.findByUsername(uname).ifPresentOrElse(u -> {
            int ok = JOptionPane.showConfirmDialog(this, "Deactivate user " + uname + "?", "Deactivate",
                    JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                boolean deact = userService.deactivateById(u.getId());
                if (deact) {
                    JOptionPane.showMessageDialog(this, "User deactivated.", "Deactivated",
                            JOptionPane.INFORMATION_MESSAGE);
                    reloadUsersTable();
                } else
                    JOptionPane.showMessageDialog(this, "Could not deactivate user (protected role?).", "Error",
                            JOptionPane.ERROR_MESSAGE);
            }
        }, () -> JOptionPane.showMessageDialog(this, "User not found: " + uname, "Error", JOptionPane.ERROR_MESSAGE));
    }

    private void openExportDialog() {
        JOptionPane.showMessageDialog(this, "Export feature not implemented.", "Export",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void openExportPaymentDialog() {
        JOptionPane.showMessageDialog(this, "Export payments feature not implemented.", "Export",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void openGenerateSummaryDialog() {
        JOptionPane.showMessageDialog(this, "Generate summary feature not implemented.", "Summary",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private boolean requireValue(Component parent, JTextField field, String fieldName) {
        if (field.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "Please enter a value for: " + fieldName,
                    "Missing Required Field",
                    JOptionPane.WARNING_MESSAGE);
            field.requestFocus();
            return false;
        }
        return true;
    }

    // DIALOG METHODS ---------------------------------------------------
    private void openAddUserDialog() {
        // Only allow creating Doctor, Staff, or Patient accounts from this dialog
        Role[] rolesForAdd = new Role[] { Role.DOCTOR, Role.STAFF, Role.PATIENT };
        JComboBox<Role> roleSelector = new JComboBox<>(rolesForAdd);

        // Shared Personal fields
        JTextField surnameField = new JTextField();
        JTextField givenNameField = new JTextField();
        JTextField middleField = new JTextField();
        JTextField dobField = new JTextField();
        JComboBox<String> genderBox = new JComboBox<>(new String[] { "-Select-", "Male", "Female" });
        JSpinner ageSpinner = new JSpinner(new SpinnerNumberModel(30, 0, 150, 1));
        JTextField nationalityField = new JTextField();

        // Shared Contact fields
        Dimension contactPref = new Dimension(480, 32);
        JTextField emailField = new JTextField();
        emailField.setPreferredSize(contactPref);
        JTextField contactNumField = new JTextField();
        contactNumField.setPreferredSize(contactPref);
        JTextField addressField = new JTextField();
        addressField.setPreferredSize(contactPref);
        JTextField emergencyNameField = new JTextField();
        emergencyNameField.setPreferredSize(contactPref);
        JTextField emergencyContactField = new JTextField();
        emergencyContactField.setPreferredSize(contactPref);

        // Professional-only fields
        JComboBox<String> titleCombo = new JComboBox<>(TITLE_TO_DEPARTMENT.keySet().toArray(new String[0]));
        titleCombo.setEditable(false);
        JTextField titleField = new JTextField();
        JTextField specialityField = new JTextField();
        JSpinner yearsField = new JSpinner(new SpinnerNumberModel(1, 0, 80, 1));
        JTextField licenseField = new JTextField();
        JTextField prcExpiryField = new JTextField();
        JTextField hospitalAffilField = new JTextField();
        titleCombo.addActionListener(ae -> {
            Object sel = titleCombo.getSelectedItem();
            if (sel instanceof String)
                specialityField.setText(TITLE_TO_DEPARTMENT.getOrDefault((String) sel, ""));
        });

        // Identification / documents
        JTextField idNumberField = new JTextField();
        idNumberField.setPreferredSize(contactPref);
        JCheckBox minorCheck = new JCheckBox("Minor (under 18)");
        JTextField studentIdField = new JTextField();
        studentIdField.setPreferredSize(contactPref);
        studentIdField.setEnabled(false);
        minorCheck.addActionListener(e -> studentIdField.setEnabled(minorCheck.isSelected()));

        // System fields: username (derived), password (generated), picture/id paths
        JTextField usernameField = new JTextField();
        usernameField.setEditable(false);
        JTextField passwordField = new JPasswordField();
        passwordField.setEditable(false);
        passwordField.setToolTipText("Password will be auto-generated on Save (e.g. PW000001)");
        JTextField picField = new JTextField();
        picField.setEditable(false);
        JTextField idFrontField = new JTextField();
        idFrontField.setEditable(false);
        JTextField idBackField = new JTextField();
        idBackField.setEditable(false);

        // Build panels
        JPanel container = new JPanel(new BorderLayout(12, 12));
        container.setBorder(new EmptyBorder(8, 8, 8, 8));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        top.add(new JLabel("Select Role:"));
        top.add(roleSelector);
        container.add(top, BorderLayout.NORTH);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(Color.WHITE);
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBackground(Color.WHITE);

        // Personal panel
        JPanel personal = new JPanel(new GridBagLayout());
        personal.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_BORDER), "Personal"));
        personal.setBackground(Color.WHITE);
        GridBagConstraints pgbc = new GridBagConstraints();
        pgbc.insets = new Insets(8, 8, 8, 8);
        pgbc.fill = GridBagConstraints.HORIZONTAL;
        pgbc.gridx = 0;
        pgbc.gridy = 0;
        BiConsumer<String, Component> addPersonal = (lbl, comp) -> {
            pgbc.gridx = 0;
            pgbc.weightx = 0;
            personal.add(new JLabel(lbl), pgbc);
            pgbc.gridx = 1;
            pgbc.weightx = 1;
            personal.add(comp, pgbc);
            pgbc.gridy++;
        };
        addPersonal.accept("Surname:", surnameField);
        addPersonal.accept("Given Name:", givenNameField);
        addPersonal.accept("Middle Name:", middleField);
        addPersonal.accept("Date Of Birth (YYYY-MM-DD):", dobField);
        addPersonal.accept("Sex/Gender:", genderBox);
        addPersonal.accept("Age:", ageSpinner);
        addPersonal.accept("Nationality:", nationalityField);

        // Contact panel
        JPanel contact = new JPanel(new GridBagLayout());
        contact.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_BORDER), "Contact"));
        contact.setBackground(Color.WHITE);
        GridBagConstraints cgbc = new GridBagConstraints();
        cgbc.insets = new Insets(8, 8, 8, 8);
        cgbc.fill = GridBagConstraints.HORIZONTAL;
        cgbc.gridx = 0;
        cgbc.gridy = 0;
        BiConsumer<String, Component> addContact = (lbl, comp) -> {
            cgbc.gridx = 0;
            cgbc.weightx = 0;
            contact.add(new JLabel(lbl), cgbc);
            cgbc.gridx = 1;
            cgbc.weightx = 1;
            contact.add(comp, cgbc);
            cgbc.gridy++;
        };
        addContact.accept("Email:", emailField);
        addContact.accept("Contact Number:", contactNumField);
        addContact.accept("Address:", addressField);
        addContact.accept("Emergency Contact Name:", emergencyNameField);
        addContact.accept("Emergency Contact Number:", emergencyContactField);

        left.add(personal);
        left.add(Box.createVerticalStrut(10));
        left.add(contact);

        // Professional panel
        JPanel prof = new JPanel(new GridBagLayout());
        prof.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_BORDER), "Professional"));
        prof.setBackground(Color.WHITE);
        GridBagConstraints rgbc = new GridBagConstraints();
        rgbc.insets = new Insets(8, 8, 8, 8);
        rgbc.fill = GridBagConstraints.HORIZONTAL;
        rgbc.gridx = 0;
        rgbc.gridy = 0;
        BiConsumer<String, Component> addProf = (lbl, comp) -> {
            rgbc.gridx = 0;
            rgbc.weightx = 0;
            prof.add(new JLabel(lbl), rgbc);
            rgbc.gridx = 1;
            rgbc.weightx = 1;
            prof.add(comp, rgbc);
            rgbc.gridy++;
        };
        addProf.accept("Title/Position:", titleCombo);
        addProf.accept("Speciality/Department:", specialityField);
        addProf.accept("Years in Field:", yearsField);
        addProf.accept("License / PRC ID:", licenseField);
        addProf.accept("PRC Expiry Date(YYYY-MM-DD):", prcExpiryField);
        left.add(prof);

        // Identification block
        JPanel idBlock = new JPanel(new GridBagLayout());
        idBlock.setBackground(Color.WHITE);
        idBlock.setBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_BORDER), "Identification"));
        GridBagConstraints idgbc = new GridBagConstraints();
        idgbc.insets = new Insets(8, 8, 8, 8);
        idgbc.fill = GridBagConstraints.HORIZONTAL;
        idgbc.gridx = 0;
        idgbc.gridy = 0;
        idgbc.gridx = 0;
        idBlock.add(new JLabel("ID Type:"), idgbc);
        idgbc.gridx = 1;
        idBlock.add(
                new JComboBox<>(new String[] { "National ID", "PhilHealth", "Driver's License", "Passport", "Other" }),
                idgbc);
        idgbc.gridy++;
        idgbc.gridx = 0;
        idBlock.add(new JLabel("ID Number:"), idgbc);
        idgbc.gridx = 1;
        idBlock.add(idNumberField, idgbc);
        idgbc.gridy++;
        idgbc.gridx = 0;
        idBlock.add(minorCheck, idgbc);
        idgbc.gridy++;
        idgbc.gridx = 0;
        idBlock.add(new JLabel("Student ID (if minor):"), idgbc);
        idgbc.gridx = 1;
        idBlock.add(studentIdField, idgbc);
        idgbc.gridy++;

        // Schedule panel for doctors: day/time dropdowns + add/remove list
        JPanel schedulePanel = new JPanel(new GridBagLayout());
        schedulePanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory
                .createTitledBorder(BorderFactory.createLineBorder(COLOR_BORDER), "Weekly Schedule (Doctor)"),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        schedulePanel.setBackground(Color.WHITE);
        schedulePanel.setPreferredSize(new Dimension(620, 360));
        GridBagConstraints sgbc2 = new GridBagConstraints();
        sgbc2.insets = new Insets(12, 12, 12, 12); // increase spacing
        sgbc2.fill = GridBagConstraints.HORIZONTAL;
        sgbc2.gridx = 0;
        sgbc2.gridy = 0;

        // day selector + build time options (LocalTime values) but render them using
        // 12-hour AM/PM format
        JComboBox<java.time.DayOfWeek> dayBox = new JComboBox<>(java.time.DayOfWeek.values());
        dayBox.setFont(FONT_NORMAL);
        java.util.List<java.time.LocalTime> timeOptions = new java.util.ArrayList<>();
        for (int hh = 6; hh <= 22; hh++) {
            timeOptions.add(java.time.LocalTime.of(hh, 0));
            timeOptions.add(java.time.LocalTime.of(hh, 30));
        }
        JComboBox<java.time.LocalTime> startBox = new JComboBox<>(timeOptions.toArray(new java.time.LocalTime[0]));
        startBox.setFont(FONT_NORMAL);
        startBox.setPreferredSize(new Dimension(160, 28));
        JComboBox<java.time.LocalTime> endBox = new JComboBox<>(timeOptions.toArray(new java.time.LocalTime[0]));
        endBox.setFont(FONT_NORMAL);
        endBox.setPreferredSize(new Dimension(160, 28));
        // formatter for display (12-hour with AM/PM)
        final java.time.format.DateTimeFormatter TIME_FMT = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
        // Renderer that shows LocalTime in hh:mm AM/PM but keeps the model value as
        // LocalTime
        ListCellRenderer<Object> timeRenderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof java.time.LocalTime)
                    setText(((java.time.LocalTime) value).format(TIME_FMT));
                return this;
            }
        };
        startBox.setRenderer(timeRenderer);
        endBox.setRenderer(timeRenderer);

        DefaultListModel<String> scheduleListModel = new DefaultListModel<>();
        JList<String> scheduleList = new JList<>(scheduleListModel);
        scheduleList.setVisibleRowCount(8);
        scheduleList.setFixedCellHeight(30);
        scheduleList.setFont(FONT_NORMAL);
        scheduleList.setPreferredSize(new Dimension(560, 220));

        JButton addSlotBtn = new JButton("Add Slot");
        addSlotBtn.setFont(FONT_NORMAL);
        JButton removeSlotBtn = new JButton("Remove Selected");
        removeSlotBtn.setFont(FONT_NORMAL);
        java.util.List<Object[]> pendingSlots = new java.util.ArrayList<>();

        // place controls in a two-column layout (label, control)
        sgbc2.gridx = 0;
        sgbc2.weightx = 0;
        schedulePanel.add(new JLabel("Day:"), sgbc2);
        sgbc2.gridx = 1;
        sgbc2.weightx = 1;
        schedulePanel.add(dayBox, sgbc2);
        sgbc2.gridy++;

        sgbc2.gridx = 0;
        sgbc2.weightx = 0;
        schedulePanel.add(new JLabel("Start:"), sgbc2);
        sgbc2.gridx = 1;
        sgbc2.weightx = 1;
        schedulePanel.add(startBox, sgbc2);
        sgbc2.gridy++;

        sgbc2.gridx = 0;
        sgbc2.weightx = 0;
        schedulePanel.add(new JLabel("End:"), sgbc2);
        sgbc2.gridx = 1;
        sgbc2.weightx = 1;
        schedulePanel.add(endBox, sgbc2);
        sgbc2.gridy++;

        // buttons row (spacious)
        JPanel smallBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        smallBtns.setOpaque(false);
        smallBtns.add(addSlotBtn);
        smallBtns.add(removeSlotBtn);
        sgbc2.gridx = 0;
        sgbc2.gridwidth = 2;
        schedulePanel.add(smallBtns, sgbc2);
        sgbc2.gridy++;
        sgbc2.gridwidth = 1;

        // schedule list
        sgbc2.gridx = 0;
        sgbc2.gridwidth = 2;
        sgbc2.fill = GridBagConstraints.BOTH;
        sgbc2.weighty = 1.0;
        schedulePanel.add(new JScrollPane(scheduleList), sgbc2);
        sgbc2.gridy++;
        sgbc2.weighty = 0;
        sgbc2.fill = GridBagConstraints.HORIZONTAL;
        sgbc2.gridwidth = 1;

        addSlotBtn.addActionListener(ae -> {
            java.time.DayOfWeek day = (java.time.DayOfWeek) dayBox.getSelectedItem();
            java.time.LocalTime st = (java.time.LocalTime) startBox.getSelectedItem();
            java.time.LocalTime en = (java.time.LocalTime) endBox.getSelectedItem();

            if (day == null || st == null || en == null) {
                JOptionPane.showMessageDialog(this, "Select day, start and end times.");
                return;
            }
            if (!en.isAfter(st)) {
                JOptionPane.showMessageDialog(this, "End time must be after start time.");
                return;
            }

            // Check for duplicates
            boolean duplicate = false;
            for (Object[] slot : pendingSlots) {
                java.time.DayOfWeek existingDay = (java.time.DayOfWeek) slot[0];
                java.time.LocalTime existingStart = (java.time.LocalTime) slot[1];
                java.time.LocalTime existingEnd = (java.time.LocalTime) slot[2];

                if (existingDay == day &&
                        ((st.equals(existingStart) && en.equals(existingEnd)) || // exact match
                                (st.isBefore(existingEnd) && en.isAfter(existingStart)))) { // overlapping
                    duplicate = true;
                    break;
                }
            }

            if (duplicate) {
                JOptionPane.showMessageDialog(this, "This schedule conflicts with an existing slot.");
                return;
            }

            // Add new slot
            String entry = day.name() + " " + st.format(TIME_FMT) + " - " + en.format(TIME_FMT);
            scheduleListModel.addElement(entry);
            pendingSlots.add(new Object[] { day, st, en });
        });

        removeSlotBtn.addActionListener(ae -> {
            int i = scheduleList.getSelectedIndex();
            if (i < 0)
                return;
            scheduleListModel.remove(i);
            pendingSlots.remove(i);
        });

        // System / documents panel
        JPanel sysdocs = new JPanel(new GridBagLayout());
        sysdocs.setBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_BORDER), "System / Documents"));
        sysdocs.setBackground(Color.WHITE);
        GridBagConstraints sgbc = new GridBagConstraints();
        sgbc.insets = new Insets(8, 8, 8, 8);
        sgbc.fill = GridBagConstraints.HORIZONTAL;
        sgbc.gridx = 0;
        sgbc.gridy = 0;
        JButton picBtn = new JButton("Choose 2x2");
        picBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Image files", "jpg", "jpeg", "png"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                picField.setText(fc.getSelectedFile().getAbsolutePath());
        });
        JButton idFrontBtn = new JButton("ID Front");
        idFrontBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Image/PDF", "jpg", "jpeg", "png", "pdf"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                idFrontField.setText(fc.getSelectedFile().getAbsolutePath());
        });
        JButton idBackBtn = new JButton("ID Back");
        idBackBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Image/PDF", "jpg", "jpeg", "png", "pdf"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                idBackField.setText(fc.getSelectedFile().getAbsolutePath());
        });
        sgbc.gridx = 0;
        sgbc.weightx = 0;
        sysdocs.add(new JLabel("2x2 Picture:"), sgbc);
        sgbc.gridx = 1;
        sgbc.weightx = 1;
        sysdocs.add(picField, sgbc);
        sgbc.gridx = 2;
        sysdocs.add(picBtn, sgbc);
        sgbc.gridy++;
        sgbc.gridx = 0;
        sysdocs.add(new JLabel("ID Front:"), sgbc);
        sgbc.gridx = 1;
        sysdocs.add(idFrontField, sgbc);
        sgbc.gridx = 2;
        sysdocs.add(idFrontBtn, sgbc);
        sgbc.gridy++;
        sgbc.gridx = 0;
        sysdocs.add(new JLabel("ID Back:"), sgbc);
        sgbc.gridx = 1;
        sysdocs.add(idBackField, sgbc);
        sgbc.gridx = 2;
        sysdocs.add(idBackBtn, sgbc);
        sgbc.gridy++;

        // Account panel
        JPanel accountPanel = new JPanel(new GridBagLayout());
        accountPanel.setOpaque(false);
        GridBagConstraints acgbc = new GridBagConstraints();
        acgbc.insets = new Insets(6, 6, 6, 6);
        acgbc.fill = GridBagConstraints.HORIZONTAL;
        acgbc.gridx = 0;
        acgbc.gridy = 0;
        acgbc.weightx = 0;
        accountPanel.add(new JLabel("Account Username:"), acgbc);
        acgbc.gridx = 1;
        acgbc.weightx = 1;
        usernameField.setPreferredSize(new Dimension(320, 28));
        accountPanel.add(usernameField, acgbc);
        acgbc.gridy++;
        acgbc.gridx = 0;
        acgbc.weightx = 0;
        accountPanel.add(new JLabel("Password: (Auto-Generated Password)"), acgbc);
        acgbc.gridx = 1;
        acgbc.weightx = 1;
        passwordField.setPreferredSize(new Dimension(320, 28));
        accountPanel.add(passwordField, acgbc);

        // assemble right column
        right.add(idBlock);
        right.add(Box.createVerticalStrut(10));
        right.add(schedulePanel);
        right.add(Box.createVerticalStrut(10));
        right.add(sysdocs);
        right.add(Box.createVerticalStrut(10));
        right.add(accountPanel);

        container.add(left, BorderLayout.WEST);
        container.add(right, BorderLayout.CENTER);

        // Dialog and footer
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Add New User",
                Dialog.ModalityType.APPLICATION_MODAL);
        dlg.getContentPane().setLayout(new BorderLayout());
        dlg.getContentPane().add(new JScrollPane(container), BorderLayout.CENTER);
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okB = new JButton("Save");
        JButton cancelB = new JButton("Cancel");
        foot.add(cancelB);
        foot.add(okB);
        dlg.getContentPane().add(foot, BorderLayout.SOUTH);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        dlg.setSize(screen.width, screen.height);
        dlg.setLocation(0, 0);
        dlg.setResizable(true);

        // visibility helper
        Runnable updateVisibility = () -> {
            Role r = (Role) roleSelector.getSelectedItem();
            boolean isPatient = r == Role.PATIENT;
            boolean isDoctor = r == Role.DOCTOR;
            boolean isStaff = r == Role.STAFF;
            prof.setVisible(isDoctor);
            schedulePanel.setVisible(isDoctor);
            titleCombo.setVisible(isDoctor);
            titleField.setVisible(!isDoctor);
            idBlock.setVisible(isPatient);
            // account panel always visible
            container.revalidate();
            container.repaint();
        };
        roleSelector.addActionListener(e -> updateVisibility.run());
        updateVisibility.run();

        // username follows email
        emailField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                usernameField.setText(emailField.getText().trim());
            }

            public void removeUpdate(DocumentEvent e) {
                usernameField.setText(emailField.getText().trim());
            }

            public void changedUpdate(DocumentEvent e) {
                usernameField.setText(emailField.getText().trim());
            }
        });

        okB.addActionListener(e -> {
            // === PERSONAL REQUIRED FIELDS ===
            if (!requireValue(dlg, surnameField, "Surname"))
                return;
            if (!requireValue(dlg, givenNameField, "Given Name"))
                return;
            if (!requireValue(dlg, dobField, "Date of Birth"))
                return;
            if (!validateDateFormat(dobField.getText().trim())) {
                JOptionPane.showMessageDialog(this, "Invalid date format (Date Of Birth. Use YYYY-MM-DD.");
                return;
            }
            if (genderBox.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(dlg, "Please select a Gender.", "Missing Required Field",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!requireValue(dlg, nationalityField, "Nationality"))
                return;

            // === CONTACT REQUIRED FIELDS ===
            if (!requireValue(dlg, emailField, "Email Address"))
                return;
            if (!requireValue(dlg, contactNumField, "Contact Number"))
                return;
            if (!requireValue(dlg, addressField, "Address"))
                return;
            if (!requireValue(dlg, emergencyNameField, "Emergency Contact Name"))
                return;
            if (!requireValue(dlg, emergencyContactField, "Emergency Contact Number"))
                return;

            // === ROLE-BASED VALIDATION ===
            Role chosenRole = (Role) roleSelector.getSelectedItem();

            // ---- FOR DOCTOR ----
            if (chosenRole == Role.DOCTOR) {
                if (!requireValue(dlg, specialityField, "Speciality / Department"))
                    return;
                if (!requireValue(dlg, licenseField, "License Number"))
                    return;
                if (!requireValue(dlg, prcExpiryField, "PRC Expiry Date"))
                    return;
                if (!validateDateFormat(prcExpiryField.getText().trim())) {
                    JOptionPane.showMessageDialog(this, "Invalid date format (PRC Expiry Date). Use YYYY-MM-DD.");
                    return;
                }
                if (genderBox.getSelectedItem() == null) {
                    JOptionPane.showMessageDialog(dlg, "Please select a Gender.", "Missing Required Field",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

            }

            // ---- FOR STAFF ----
            if (chosenRole == Role.STAFF) {
                if (!requireValue(dlg, titleField, "Staff Title / Position"))
                    return;
            }

            // ---- FOR PATIENT ----
            if (chosenRole == Role.PATIENT) {
                if (!minorCheck.isSelected()) {
                    // If NOT a minor, require National/PhilHealth ID
                    if (!requireValue(dlg, idNumberField, "ID Number"))
                        return;
                } else {
                    // If minor, require student ID
                    if (!requireValue(dlg, studentIdField, "Student ID"))
                        return;
                }
            }
            // if (!requireValue(dlg, picField, "2x2 Picture")) return;
            Role chosen = (Role) roleSelector.getSelectedItem();
            String email = emailField.getText().trim();
            if (email.isEmpty()) {
                JOptionPane.showMessageDialog(dlg,
                        "Please provide an email address for the account (used as username).", "Validation",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                JOptionPane.showMessageDialog(dlg, "Please provide a valid email address.", "Validation",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String uname = email;
            String plainPw = userService.generateNextPlainPassword();
            passwordField.setText(plainPw);
            char[] pw = plainPw.toCharArray();
            try {
                userService.createUser(uname, pw, chosen);
                Arrays.fill(pw, '\0');
                userService.findByUsername(uname).ifPresent(u -> {
                    // basic user info
                    String full = surnameField.getText().trim();
                    if (!givenNameField.getText().trim().isBlank())
                        full += (full.isEmpty() ? "" : " ") + givenNameField.getText().trim();
                    if (!middleField.getText().trim().isBlank())
                        full += (full.isEmpty() ? "" : " ") + middleField.getText().trim();
                    if (!full.isBlank())
                        u.setFullName(full);
                    if (!emailField.getText().trim().isBlank())
                        u.setEmail(emailField.getText().trim());
                    if (!picField.getText().trim().isBlank())
                        u.setProfilePictureUrl(picField.getText().trim());

                    // save patient profile common fields
                    PatientService.PatientProfile profile = PatientService.getInstance()
                            .getProfileByUsername(u.getUsername());
                    profile.surname = surnameField.getText().trim();
                    profile.firstName = givenNameField.getText().trim();
                    profile.middleName = middleField.getText().trim();
                    profile.dateOfBirth = dobField.getText().trim();
                    profile.gender = genderBox.getSelectedItem() == null ? "" : genderBox.getSelectedItem().toString();
                    profile.nationality = nationalityField.getText().trim();
                    profile.phone = contactNumField.getText().trim();
                    profile.email = emailField.getText().trim();
                    profile.address = addressField.getText().trim();
                    profile.emergencyContactName = emergencyNameField.getText().trim();
                    profile.emergencyContactNumber = emergencyContactField.getText().trim();
                    profile.idNumber = idNumberField.getText().trim();
                    profile.idFrontPath = idFrontField.getText().trim();
                    profile.idBackPath = idBackField.getText().trim();
                    profile.twoByTwoPath = picField.getText().trim();
                    PatientService.getInstance().saveProfile(u.getUsername(), profile);

                    // role specific
                    if (chosen == Role.DOCTOR) {
                        Doctor d = new Doctor(u);
                        d.setSpecialization(specialityField.getText().trim());
                        d.setLicenseNumber(licenseField.getText().trim());
                        try {
                            if (!prcExpiryField.getText().trim().isEmpty())
                                d.setLicenseExpiry(LocalDate.parse(prcExpiryField.getText().trim()));
                        } catch (Exception ignored) {
                        }
                        try {
                            d.setYearsOfExperience(((Number) yearsField.getValue()).intValue());
                        } catch (Exception ignored) {
                        }
                        d.setContactNumber(contactNumField.getText().trim());
                        DoctorServiceImpl.getInstance().save(d);
                        // persist schedule slots
                        Service.DoctorScheduleService sched = Service.DoctorScheduleService.getInstance();
                        for (Object[] os : pendingSlots) {
                            java.time.DayOfWeek day = (java.time.DayOfWeek) os[0];
                            java.time.LocalTime st = (java.time.LocalTime) os[1];
                            java.time.LocalTime en = (java.time.LocalTime) os[2];
                            try {
                                sched.addSlot(d.getDoctorId(), day, st, en);
                            } catch (Exception ignored) {
                            }
                        }
                        if (doctorPanel != null)
                            doctorPanel.reload();
                    }
                    if (chosen == Role.STAFF) {
                        // Create Staff record in database
                        Model.Staff staff = new Model.Staff(
                                u,
                                givenNameField.getText().trim(),
                                surnameField.getText().trim(),
                                titleField.getText().trim(), // role_type
                                "DEPT-001", // default department, you can add a department selector
                                contactNumField.getText().trim(),
                                java.time.LocalDate.now(), // hire_date
                                Model.UserStatus.ACTIVE);
                        Repository.StaffRepository.getInstance().save(staff);
                        if (staffPanel != null)
                            staffPanel.reloadPanel();
                    }
                    if (chosen == Role.PATIENT) {
                        PatientService.getInstance().createPatientForUser(u, givenNameField.getText().trim(),
                                surnameField.getText().trim(), null, profile.gender, profile.phone, profile.address);
                        if (patientPanel != null)
                            patientPanel.reloadPanel();
                    }
                });

                reloadUsersTable();
                JOptionPane.showMessageDialog(dlg,
                        "User created (username: " + uname + ")\nGenerated password: " + plainPw, "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                dlg.dispose();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(dlg, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Cancel
        cancelB.addActionListener(e -> dlg.dispose());
        dlg.setVisible(true);
    }
}
