package UI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.*;
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
import Service.PatientService;
import Service.AppointmentService;
import Service.UserService;
import Model.Patient;
import Model.User;
import Model.Role;
import Model.Appointment;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Period;
import Controller.StaffController;

import java.util.function.BiConsumer;
import java.util.Arrays;
import javax.swing.filechooser.FileNameExtensionFilter;

public class StaffDashboardPanel extends JPanel implements GlobalSearchable {
    private static final long serialVersionUID = 1L;
    // THEME (aligned with Admin/Doctor/Patient dashboards)
    private static final Color COLOR_BG = Color.WHITE;
    private static final Color COLOR_SIDEBAR_BG = new Color(245, 247, 250);
    private static final Color COLOR_PRIMARY = new Color(60, 120, 200);
    private static final Color COLOR_PRIMARY_HOVER = new Color(80, 140, 220);
    private static final Color COLOR_ACTIVE = new Color(100, 160, 240);
    private static final Color COLOR_BORDER = new Color(210, 215, 220);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONT_NORMAL = new Font("Segoe UI", Font.PLAIN, 16);

    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private JPanel sideNavPanel;
    private JButton btnSummary;
    private JButton btnPatientReg;
    private JButton btnMedical;
    private JButton btnBilling;
    private JButton btnLab;
    private JButton btnAdmission;
    private JButton activeButton;
    // headerActions removed to avoid duplicate toolbar gap; per-panel action panels are used

    private String currentUsername;
    private String subRole; // REGISTRATION, BILLING, LAB (optional)

    // controller reference
    private final StaffController staffController;

    // NEW: Keep a reference to the username label to toggle visibility
    private JLabel userTagLabel;

    // Tables
    private JTable patientRegTable;
    private JTable medicalRecordTable;
    private JTable billingTable;
    private JTable labTable;
    private JTable admissionTable;
    // Global search/filter state
    private String globalSearchQuery;
    private final Map<String, Map<String,String>> columnFilters = new HashMap<>();

    // Summary dynamic labels
    private JLabel lblTotalPatients;
    private JLabel lblPendingBills;
    private JLabel lblLabPending;

    // Constructors (controller-first to avoid ambiguous String overloads)
    public StaffDashboardPanel(StaffController controller, String username) { this(controller, null, username); }
    public StaffDashboardPanel(StaffController controller, String subRole, String username) {
        this.staffController = controller;
        this.currentUsername = username;
        this.subRole = subRole != null ? subRole.toUpperCase() : null;
        setBackground(COLOR_BG);
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setLayout(new BorderLayout(8, 8));

        add(createHeader(), BorderLayout.NORTH);
        add(createSideBar(), BorderLayout.WEST);
        add(createMainContent(), BorderLayout.CENTER);

        // Default card selection based on subRole
        if (this.subRole == null) {
            setActiveButton(btnSummary, "SUMMARY");
        } else {
            switch (this.subRole) {
                case "REGISTRATION": setActiveButton(btnPatientReg, "PATIENT_REG"); break;
                case "BILLING": setActiveButton(btnBilling, "BILLING"); break;
                case "LAB": setActiveButton(btnLab, "LAB"); break;
                default: setActiveButton(btnSummary, "SUMMARY");
            }
        }

        // Show any queued notifications for this staff user
        SwingUtilities.invokeLater(this::checkNotifications);
     }

    private void checkNotifications() {
        if (this.currentUsername == null || this.currentUsername.isBlank()) return;
        java.util.List<String> notes = Service.NotificationService.getInstance().getAndClearNotifications(this.currentUsername);
        if (notes != null && !notes.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String n : notes) sb.append("- ").append(n).append("\n");
            JOptionPane.showMessageDialog(this, sb.toString(), "Notifications", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private JComponent createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new LineBorder(COLOR_BORDER));
        header.setBackground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 60));

        // Left side: title (with subRole if any)
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        left.setOpaque(false);
        userTagLabel = null;

        String titleText = (subRole != null && !subRole.isBlank()) ? "Staff Dashboard - " + subRole : "Staff Dashboard";
        JLabel title = new JLabel(titleText);
        title.setFont(FONT_TITLE);
        title.setForeground(COLOR_PRIMARY.darker());
        left.add(title);

        // Right: persistent refresh and dynamic actions area
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12)); right.setOpaque(false);
        JButton btnRefresh = new JButton("Refresh"); styleSecondaryButton(btnRefresh); btnRefresh.addActionListener(e -> JOptionPane.showMessageDialog(this, "Data refreshed (placeholder)", "Info", JOptionPane.INFORMATION_MESSAGE));
        right.add(btnRefresh);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JComponent createSideBar() {
        sideNavPanel = new JPanel();
        sideNavPanel.setLayout(new BoxLayout(sideNavPanel, BoxLayout.Y_AXIS));
        sideNavPanel.setBackground(COLOR_SIDEBAR_BG);
        sideNavPanel.setBorder(new LineBorder(COLOR_BORDER));
        sideNavPanel.setPreferredSize(new Dimension(260, 0));

        btnSummary = createNavButton("Summary", "SUMMARY");
        btnPatientReg = createNavButton("Patient Registration", "PATIENT_REG");
        btnMedical = createNavButton("Medical Records", "MEDICAL");
        btnBilling = createNavButton("Billing History", "BILLING");
        btnLab = createNavButton("Lab Tests", "LAB");
        btnAdmission = createNavButton("Admission & Discharge", "ADMISSION");
       // JButton btnGuide = createNavButton("User Guide", "GUIDE");

        // consistent spacing
        int gap = 12;
        sideNavPanel.add(Box.createVerticalStrut(gap));
        sideNavPanel.add(btnSummary); sideNavPanel.add(Box.createVerticalStrut(gap));
        sideNavPanel.add(btnPatientReg); sideNavPanel.add(Box.createVerticalStrut(gap));
        sideNavPanel.add(btnAdmission); sideNavPanel.add(Box.createVerticalStrut(gap));
        sideNavPanel.add(btnMedical); sideNavPanel.add(Box.createVerticalStrut(gap));
        sideNavPanel.add(btnLab); sideNavPanel.add(Box.createVerticalStrut(gap));
        sideNavPanel.add(btnBilling); sideNavPanel.add(Box.createVerticalStrut(gap));
       // sideNavPanel.add(btnGuide); sideNavPanel.add(Box.createVerticalStrut(8));
        sideNavPanel.add(Box.createVerticalGlue());
        return sideNavPanel;
    }

    private JButton createNavButton(String text, String card) {
        JButton b = new JButton(text);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        // taller buttons for consistent spacing with patient UI
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        b.setPreferredSize(new Dimension(Integer.MAX_VALUE, 64));
        b.setFont(FONT_NORMAL);
        b.setBackground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(new LineBorder(COLOR_BORDER));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (b != activeButton) b.setBackground(COLOR_PRIMARY_HOVER); }
            @Override public void mouseExited(MouseEvent e) { if (b != activeButton) b.setBackground(Color.WHITE); }
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
        // Username is no longer shown anywhere
        cardLayout.show(mainContentPanel, card);
        // per-panel action panels are used; no global headerActions to update
    }

    private JComponent createMainContent() {
        mainContentPanel = new JPanel();
        cardLayout = new CardLayout();
        mainContentPanel.setLayout(cardLayout);
        mainContentPanel.setBorder(new LineBorder(COLOR_BORDER));

        mainContentPanel.add(buildSummaryPanel(), "SUMMARY");
        mainContentPanel.add(buildPatientRegPanel(), "PATIENT_REG");
        mainContentPanel.add(buildMedicalPanel(), "MEDICAL");
        mainContentPanel.add(buildBillingPanel(), "BILLING");
        mainContentPanel.add(buildLabPanel(), "LAB");
        mainContentPanel.add(buildAdmissionPanel(), "ADMISSION");
        mainContentPanel.add(buildGuidePanel(), "GUIDE");
        return mainContentPanel;
    }

    // SUMMARY ---------------------------------------------------------
    private JPanel buildSummaryPanel() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(COLOR_BG);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        root.add(sectionHeader("Operational Summary"), BorderLayout.NORTH);

        JPanel statsGrid = new JPanel(new GridLayout(1, 3, 12, 12));
        statsGrid.setOpaque(false);
        lblTotalPatients = createStatValueLabel("0");
        lblPendingBills = createStatValueLabel("0");
        lblLabPending = createStatValueLabel("0");
        statsGrid.add(wrapStat("Registered Patients", lblTotalPatients));
        statsGrid.add(wrapStat("Pending Bills", lblPendingBills));
        statsGrid.add(wrapStat("Pending Lab Tests", lblLabPending));
        root.add(statsGrid, BorderLayout.CENTER);

        JTextArea info = new JTextArea("Welcome staff! Navigate using the left menu to manage patients, records, billing, labs, and admissions.");
        info.setEditable(false); info.setLineWrap(true); info.setWrapStyleWord(true); info.setFont(FONT_NORMAL);
        info.setBorder(new EmptyBorder(8, 12, 8, 12));
        root.add(new JScrollPane(info), BorderLayout.SOUTH);
        return root;
    }

    private JLabel createStatValueLabel(String value) {
        JLabel l = new JLabel(value, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 22));
        l.setForeground(COLOR_PRIMARY);
        return l;
    }
    private JPanel wrapStat(String title, JLabel value) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new LineBorder(COLOR_BORDER));
        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.setForeground(COLOR_PRIMARY.darker());
        t.setBorder(new EmptyBorder(6, 6, 0, 6));
        p.add(t, BorderLayout.NORTH);
        p.add(value, BorderLayout.CENTER);
        return p;
    }

    // PATIENT REGISTRATION --------------------------------------------
    private JPanel buildPatientRegPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(COLOR_BG);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Top area: title (left), search (center), actions (right) - action buttons visible next to search
        JPanel topPanel = new JPanel(new BorderLayout(12, 12));
        topPanel.setOpaque(false);
        // add a small empty border for breathing room
        topPanel.setBorder(new EmptyBorder(6, 6, 6, 6));

        JLabel header = new JLabel("Patient Registration & Records", SwingConstants.LEFT);
        header.setFont(FONT_SECTION);
        header.setForeground(COLOR_PRIMARY.darker());
        topPanel.add(header, BorderLayout.WEST);

        // Center: search area (use a panel to left-align and provide padding)
        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchWrap.setOpaque(false);
        searchWrap.add(new JLabel("Search Patients:"));
        JTextField searchField = new JTextField(24);
        searchField.setPreferredSize(new Dimension(300, 28));
        searchWrap.add(searchField);
        topPanel.add(searchWrap, BorderLayout.CENTER);

        // Right: actions — put buttons inside a horizontal box for consistent spacing
        JPanel actionPanel = new JPanel();
        actionPanel.setOpaque(false);
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.X_AXIS));
        actionPanel.setBorder(new EmptyBorder(0, 0, 0, 4));

        JButton btnAdd = new JButton("Add"); styleSecondaryButton(btnAdd); btnAdd.addActionListener(e -> openAddPatientDialog());
        JButton btnView = new JButton("View"); styleSecondaryButton(btnView); btnView.addActionListener(e -> openViewPatientDialog());
        JButton btnDeactivate = new JButton("Deactivate"); styleSecondaryButton(btnDeactivate); btnDeactivate.addActionListener(e -> openDeactivatePatientDialog());
        JButton btnAssign = new JButton("Assign Appointment"); styleSecondaryButton(btnAssign); btnAssign.addActionListener(e -> openAssignAppointmentDialogForStaff());

        // Standardize sizes
        Dimension small = new Dimension(84, 34);
        Dimension medium = new Dimension(120, 34);
        Dimension large = new Dimension(180, 34);
        btnAdd.setPreferredSize(small); btnAdd.setMaximumSize(small);
        btnView.setPreferredSize(small); btnView.setMaximumSize(small);
        btnDeactivate.setPreferredSize(medium); btnDeactivate.setMaximumSize(medium);
        btnAssign.setPreferredSize(large); btnAssign.setMaximumSize(large);

        actionPanel.add(Box.createHorizontalGlue()); // push buttons to the right within the Box
        actionPanel.add(btnAdd); actionPanel.add(Box.createRigidArea(new Dimension(8,0)));
        actionPanel.add(btnView); actionPanel.add(Box.createRigidArea(new Dimension(8,0)));
        actionPanel.add(btnDeactivate); actionPanel.add(Box.createRigidArea(new Dimension(8,0)));
        actionPanel.add(btnAssign);

        // Wrap actionPanel so it behaves in BorderLayout.EAST
        JPanel actionWrap = new JPanel(new BorderLayout()); actionWrap.setOpaque(false); actionWrap.add(actionPanel, BorderLayout.CENTER);
        topPanel.add(actionWrap, BorderLayout.EAST);

        root.add(topPanel, BorderLayout.NORTH);

        // Now include hidden ID column at index 0: {ID, Name, Age, Gender, Status}
        String[] cols = {"ID", "Name", "Age", "Gender", "Status"};
        Object[][] data = {{"p1", "John Doe", 45, "M", "Active"}, {"p2", "Jane Smith", 29, "F", "Inactive"}};
        DefaultTableModel ptModel = new DefaultTableModel(data, cols) { @Override public boolean isCellEditable(int r,int c){ return false; } };
        patientRegTable = new JTable(ptModel);

        // Improve table appearance and sizing
        patientRegTable.setRowHeight(28);
        patientRegTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        patientRegTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        patientRegTable.setFillsViewportHeight(true);

        // Hide ID column visually but keep it in the model. Set width to 0 and mark not resizable.
        try {
            patientRegTable.getColumnModel().getColumn(0).setMinWidth(0);
            patientRegTable.getColumnModel().getColumn(0).setMaxWidth(0);
            patientRegTable.getColumnModel().getColumn(0).setPreferredWidth(0);
            patientRegTable.getColumnModel().getColumn(0).setResizable(false);
        } catch (Exception ignored) {}

        // Put table in a scroll pane with a preferred viewport size
        JScrollPane tableScroll = new JScrollPane(patientRegTable);
        tableScroll.setBorder(new LineBorder(COLOR_BORDER));
        tableScroll.setPreferredSize(new Dimension(760, 360));

        // Add search listener
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterPatientRegTable(searchField.getText()); }
            public void removeUpdate(DocumentEvent e) { filterPatientRegTable(searchField.getText()); }
            public void changedUpdate(DocumentEvent e) { filterPatientRegTable(searchField.getText()); }
        });

        root.add(tableScroll, BorderLayout.CENTER);
        return root;
    }

    // Assign appointment flow used by staff panel
    private void openAssignAppointmentDialogForStaff() {
        int row = patientRegTable.getSelectedRow();
        if (row == -1) { warn("Select a patient first"); return; }
        DefaultTableModel m = (DefaultTableModel) patientRegTable.getModel();
        String patientId = (String) m.getValueAt(row, 0);
        String patientName = (String) m.getValueAt(row, 1);

        JPanel p = new JPanel(new GridLayout(5, 2, 8, 8));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        p.add(new JLabel("Patient:")); JTextField patientField = new JTextField(patientName); patientField.setEditable(false); p.add(patientField);

        // SPECIALTY -> doctor selection UI
        p.add(new JLabel("Specialty:"));
        String[] specialties = new String[]{"Any","Orthopedics","Cardiology","Pediatrics","General"};
        JComboBox<String> specialtyCombo = new JComboBox<>(specialties);
        p.add(specialtyCombo);

        p.add(new JLabel("Doctor:"));
        JComboBox<String> doctorCombo = new JComboBox<>();
        doctorCombo.setEditable(false);
        p.add(doctorCombo);

        // Instead of free-form datetime + text reason, we present a dropdown of available slot datetimes
        p.add(new JLabel("Available Slot (Date / Time):"));
        JComboBox<String> dateTimeCombo = new JComboBox<>(); dateTimeCombo.setEnabled(false);
        p.add(dateTimeCombo);
        p.add(new JLabel("Reason:"));
        JComboBox<String> reasonCombo = new JComboBox<>(); reasonCombo.setEnabled(false);
        p.add(reasonCombo);

        // Build doctor label -> Model.Doctor map using DoctorServiceImpl if available; fall back to usernames
        java.util.Map<String, Model.Doctor> doctorByLabel = new java.util.LinkedHashMap<>();
        try {
            for (Model.Doctor d : Service.DoctorServiceImpl.getInstance().listActive()) {
                String uname = (d.getUser()!=null) ? d.getUser().getUsername() : (d.getDoctorId()==null?"":d.getDoctorId());
                String spec = d.getSpecialization()==null?"General":d.getSpecialization();
                String label = uname + " (" + spec + ")";
                doctorByLabel.put(label, d);
            }
        } catch (Throwable ignored) {
            // Fallback: use usernames from UserService if DoctorServiceImpl not available
            for (User u : UserService.getInstance().getAllUsers()) {
                if (u.getRole() == Role.DOCTOR) {
                    String label = u.getUsername() + " (General)";
                    doctorByLabel.putIfAbsent(label, null);
                }
            }
        }
        // Populate doctorCombo filtered by specialty selection
        Runnable refreshDoctorCombo = () -> {
            String selSpec = (String) specialtyCombo.getSelectedItem();
            doctorCombo.removeAllItems();
            for (java.util.Map.Entry<String, Model.Doctor> e : doctorByLabel.entrySet()) {
                String label = e.getKey(); Model.Doctor d = e.getValue();
                if (selSpec == null || selSpec.equalsIgnoreCase("Any") || label.toLowerCase().contains(selSpec.toLowerCase())) {
                    doctorCombo.addItem(label);
                }
            }
            if (doctorCombo.getItemCount() == 0) { doctorCombo.addItem("(No doctors available)"); doctorCombo.setEnabled(false); }
            else doctorCombo.setEnabled(true);
        };
        specialtyCombo.addActionListener(e -> refreshDoctorCombo.run());
        refreshDoctorCombo.run();

        // Helper: build upcoming datetimes for a doctor's available schedule slots
        java.util.function.BiConsumer<Model.Doctor, JComboBox<String>> populateSlotsForDoctor = (doc, targetCombo) -> {
            targetCombo.removeAllItems();
            if (doc == null) { targetCombo.addItem("(No schedule available)"); targetCombo.setEnabled(false); return; }
            java.util.List<Model.DoctorSchedule> slots = Service.DoctorScheduleService.getInstance().listByDoctorId(doc.getDoctorId());
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            java.util.List<String> items = new java.util.ArrayList<>();
            for (Model.DoctorSchedule s : slots) {
                if (!s.isAvailable()) continue;
                // for next 14 days, find dates matching the slot day-of-week
                for (int d = 0; d < 14; d++) {
                    java.time.LocalDate candidate = today.plusDays(d);
                    if (candidate.getDayOfWeek() != s.getDayOfWeek()) continue;
                    java.time.LocalTime t = s.getTimeStart();
                    while (!t.isAfter(s.getTimeEnd().minusMinutes(1))) {
                        java.time.LocalDateTime dt = java.time.LocalDateTime.of(candidate, t);
                        if (!dt.isBefore(java.time.LocalDateTime.now())) {
                            items.add(dt.format(dtf));
                        }
                        t = t.plusMinutes(30);
                    }
                }
            }
            if (items.isEmpty()) { targetCombo.addItem("(no available times in next 14 days)"); targetCombo.setEnabled(false); }
            else { for (String it : items) targetCombo.addItem(it); targetCombo.setEnabled(true); }
        };

        // Auto-derive reason options from patient profile (existingConditions/symptoms) and populate reasonCombo
        java.util.function.Function<String, java.util.List<String>> deriveReasons = (pid) -> {
            java.util.List<String> list = new java.util.ArrayList<>();
            // Attempt to obtain profile by provisioned username
            PatientService ps = PatientService.getInstance();
            String username = ps.getProvisionedAccountForPatient(pid).map(acc -> acc.username).orElse(null);
            Service.PatientService.PatientProfile prof = null;
            if (username != null) prof = ps.getProfileByUsername(username);
            // Use a safe accessor that tolerates different profile shapes (some builds may not have these fields)
            String existing = safeReadProfileField(prof, "existingConditions", "medicalHistory", "conditions").toLowerCase();
            String symptoms = safeReadProfileField(prof, "symptoms", "presentingSymptoms", "chiefComplaint", "complaint").toLowerCase();
            if (existing.contains("surgery") || existing.contains("pre-op") || existing.contains("post-op")) { list.add("Surgery Consult"); list.add("Pre-op Evaluation"); }
            if (existing.contains("follow")) { list.add("Follow-up"); }
            if (existing.contains("lab") || existing.contains("blood")) { list.add("Lab Review"); }
            if (symptoms.contains("fever") || symptoms.contains("cough") || symptoms.contains("pain") || symptoms.contains("bleed")) { list.add("Acute Care"); }
            // canonical defaults
            if (!list.contains("Consultation")) list.add("Consultation");
            if (!list.contains("Follow-up")) list.add("Follow-up");
            if (!list.contains("Lab Review")) list.add("Lab Review");
            return list;
        };

        // When doctor selection changes, populate slots and compute reason options
        doctorCombo.addActionListener(e -> {
            String sel = (String) doctorCombo.getSelectedItem();
            if (sel == null || sel.startsWith("(No doctors")) {
                dateTimeCombo.removeAllItems(); dateTimeCombo.addItem("(No doctors available)"); dateTimeCombo.setEnabled(false);
                reasonCombo.removeAllItems(); reasonCombo.addItem("Consultation"); reasonCombo.setEnabled(false);
                return;
            }
            Model.Doctor doc = doctorByLabel.get(sel);
            if (doc == null) {
                // fallback: try to resolve by username
                try { for (Model.Doctor d : Service.DoctorServiceImpl.getInstance().listActive()) { if (d.getUser()!=null && (d.getUser().getUsername()).equalsIgnoreCase(sel.split(" ")[0])) { doc = d; break; } } } catch (Throwable ignored) {}
            }
            // populate available datetimes
            try { populateSlotsForDoctor.accept(doc, dateTimeCombo); } catch (Throwable ignored) { dateTimeCombo.removeAllItems(); dateTimeCombo.addItem("(no schedule service)"); dateTimeCombo.setEnabled(false); }
            // compute reasons
            try { java.util.List<String> reasons = deriveReasons.apply(patientId); reasonCombo.removeAllItems(); for (String r : reasons) reasonCombo.addItem(r); reasonCombo.setEnabled(true); } catch (Throwable ignored) { reasonCombo.removeAllItems(); reasonCombo.addItem("Consultation"); reasonCombo.setEnabled(false); }
        });

        int res = JOptionPane.showConfirmDialog(this, p, "Assign Appointment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String doctor = (doctorCombo.getItemCount() > 0 && doctorCombo.isEnabled()) ? (String) doctorCombo.getSelectedItem() : null;
            if (doctor != null && doctor.startsWith("(No doctors")) doctor = null;
            if (doctor == null) { warn("No doctor selected or available for that specialty"); return; }
            String whenStr = (String) dateTimeCombo.getSelectedItem();
            String reason = (String) reasonCombo.getSelectedItem();
            if (doctor.isEmpty() || whenStr.isEmpty()) { warn("Doctor and date/time are required"); return; }
            LocalDateTime when;
            try { when = LocalDateTime.parse(whenStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")); }
            catch (Exception ex) { warn("Invalid date/time format. Use YYYY-MM-DDTHH:MM"); return; }

            // Schedule using selected doctor's username as staffId
            try {
                Appointment appt = AppointmentService.getInstance().schedule(patientId, doctor, when, reason);
                // Register the request origin so staff can be notified if it is cancelled/accepted
                Service.AppointmentRequestRegistry.getInstance().registerRequest(appt.getId(), this.currentUsername==null?"staff":this.currentUsername);
                info("Appointment scheduled for " + patientName + " with " + doctor + " on " + when);
            } catch (Exception ex) {
                warn("Failed to schedule appointment: " + ex.getMessage());
            }
        }
    }

    // MEDICAL RECORDS -------------------------------------------------
    private JPanel buildMedicalPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(COLOR_BG);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Top: header, search, actions
        JPanel topPanel = new JPanel(new BorderLayout(8,8)); topPanel.setOpaque(false);
        JLabel header = new JLabel("Medical & Treatment Records", SwingConstants.LEFT); header.setFont(FONT_SECTION); header.setForeground(COLOR_PRIMARY.darker()); topPanel.add(header, BorderLayout.WEST);
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); searchPanel.setOpaque(false); searchPanel.add(new JLabel("Search Records:")); JTextField searchField = new JTextField(20); searchPanel.add(searchField); topPanel.add(searchPanel, BorderLayout.CENTER);

        // Toolbar: only Request and Refresh
        JPanel actionPanelMed = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); actionPanelMed.setOpaque(false);
        JButton btnRequest = new JButton("Request"); styleSecondaryButton(btnRequest); btnRequest.addActionListener(e -> openRequestMedicalRecordDialog());
        JButton btnRefresh = new JButton("Refresh"); styleSecondaryButton(btnRefresh); btnRefresh.addActionListener(e -> {
            // Placeholder refresh of medical records table
            try { ((DefaultTableModel) medicalRecordTable.getModel()).fireTableDataChanged(); info("Records refreshed."); } catch (Exception ignored) { info("Refresh complete."); }
        });
        actionPanelMed.add(btnRequest); actionPanelMed.add(btnRefresh);
        topPanel.add(actionPanelMed, BorderLayout.EAST);
        root.add(topPanel, BorderLayout.NORTH);

        String[] cols = {"Record ID", "Patient", "Type", "Notes"};
        Object[][] data = {{101, "John Doe", "Consultation", "Blood pressure stable"}, {102, "Jane Smith", "Follow-up", "Recommend lab test"}};
        medicalRecordTable = new JTable(new DefaultTableModel(data, cols));

        // Add search listener
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterMedicalTable(searchField.getText()); }
            public void removeUpdate(DocumentEvent e) { filterMedicalTable(searchField.getText()); }
            public void changedUpdate(DocumentEvent e) { filterMedicalTable(searchField.getText()); }
        });

        root.add(new JScrollPane(medicalRecordTable), BorderLayout.CENTER);
        return root;
    }

    // NEW: Medical Record Request dialog (Staff -> Doctor)
    private void openRequestMedicalRecordDialog() {
        // Form layout
        JPanel form = new JPanel(new GridBagLayout()); form.setBorder(new EmptyBorder(12,12,12,12));
        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(8,8,8,8); gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridx=0; gbc.gridy=0; gbc.weightx=0;

        // Fields
        JTextField patientIdField = new JTextField();
        JTextField patientNameField = new JTextField();
        patientNameField.setToolTipText("Optional; will be validated against Patient ID if resolvable");

        JComboBox<String> doctorCombo = new JComboBox<>(); doctorCombo.setEditable(false);
        JLabel doctorHint = new JLabel("(Assigned doctors only)"); doctorHint.setForeground(Color.GRAY);

        // Scope multi-select
        JList<String> scopeList = new JList<>(new String[]{
            "Lab Results",
            "Medications & Allergies",
            "Discharge Summary",
            "Orders & Referrals",
            "Full Medical Record",
            "Other"
        });
        scopeList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JTextField otherScopeDetail = new JTextField(); otherScopeDetail.setEnabled(false);

        // Time range
        JTextField fromDateField = new JTextField(); fromDateField.setToolTipText("YYYY-MM-DD (optional)");
        JTextField toDateField = new JTextField(); toDateField.setToolTipText("YYYY-MM-DD (optional)");

        // Urgency
        JComboBox<String> urgencyCombo = new JComboBox<>(new String[]{"Normal","High"});
        JTextField urgencyJustification = new JTextField(); urgencyJustification.setEnabled(false);

        // Consent handling
        JCheckBox consentOnFile = new JCheckBox("Consent on file");
        JCheckBox emergencyException = new JCheckBox("Emergency exception");
        JTextField emergencyReason = new JTextField(); emergencyReason.setEnabled(false);

        // Visibility (fixed per confirmation)
        JLabel visibilityLabel = new JLabel("Visibility: Doctor and Staff");

        // Notifications (system inbox only)
        JCheckBox notifyInbox = new JCheckBox("Notify via System Inbox"); notifyInbox.setSelected(true);

        // Wire interactions
        scopeList.addListSelectionListener(e -> {
            boolean selectedOther = Arrays.stream(scopeList.getSelectedIndices()).anyMatch(i -> i == scopeList.getModel().getSize()-1);
            otherScopeDetail.setEnabled(selectedOther);
        });
        urgencyCombo.addActionListener(e -> urgencyJustification.setEnabled("High".equalsIgnoreCase(String.valueOf(urgencyCombo.getSelectedItem()))));
        emergencyException.addActionListener(e -> emergencyReason.setEnabled(emergencyException.isSelected()));

        // Helper to add rows
        java.util.function.BiConsumer<String, Component> addRow = (label, comp) -> { gbc.gridx=0; gbc.weightx=0; form.add(new JLabel(label), gbc); gbc.gridx=1; gbc.weightx=1; form.add(comp, gbc); gbc.gridy++; };

        addRow.accept("Patient ID:", patientIdField);
        addRow.accept("Patient Name (optional):", patientNameField);
        addRow.accept("Assigned Doctor:", doctorCombo);
        addRow.accept("", doctorHint);
        JScrollPane scopeScroll = new JScrollPane(scopeList); scopeScroll.setPreferredSize(new Dimension(280, 120));
        addRow.accept("Record Scope:", scopeScroll);
        addRow.accept("Other details:", otherScopeDetail);
        addRow.accept("From Date:", fromDateField);
        addRow.accept("To Date:", toDateField);
        addRow.accept("Urgency:", urgencyCombo);
        addRow.accept("High-urgency reason:", urgencyJustification);
        addRow.accept("Consent:", consentOnFile);
        addRow.accept("Emergency Exception:", emergencyException);
        addRow.accept("Emergency Reason:", emergencyReason);
        addRow.accept("Visibility:", visibilityLabel);
        addRow.accept("Notifications:", notifyInbox);

        // Populate assigned doctors when patient ID changes
        patientIdField.getDocument().addDocumentListener(new DocumentListener(){
            private void refresh(){
                String pid = patientIdField.getText().trim();
                doctorCombo.removeAllItems();
                if (pid.isEmpty()) { doctorCombo.addItem("(Enter Patient ID)"); doctorCombo.setEnabled(false); return; }
                // Validate patient exists via local table (hidden ID column index 0)
                boolean existsLocal = false;
                try {
                    DefaultTableModel pm = (DefaultTableModel) patientRegTable.getModel();
                    for (int i=0;i<pm.getRowCount();i++){ Object v = pm.getValueAt(i,0); if (v!=null && pid.equals(String.valueOf(v))) { existsLocal = true; break; }}
                } catch (Throwable ignored) {}
                if (!existsLocal) { doctorCombo.addItem("(Unknown Patient ID)"); doctorCombo.setEnabled(false); return; }
                // Assigned-only: if no service to fetch assignments, show placeholder
                doctorCombo.addItem("(No assigned doctors)"); doctorCombo.setEnabled(false);
            }
            public void insertUpdate(DocumentEvent e){ refresh(); }
            public void removeUpdate(DocumentEvent e){ refresh(); }
            public void changedUpdate(DocumentEvent e){ refresh(); }
        });

        // Show large dialog
        int res = showLargeDialog(form, "Request Medical Record");
        if (res != JOptionPane.OK_OPTION) return;

        // Validate
        String patientId = patientIdField.getText().trim();
        if (patientId.isEmpty()) { warn("Patient ID is required"); return; }
        String patientName = patientNameField.getText().trim();
        // validate patient exists via local table
        boolean existsLocal = false; String expectedName = null;
        try {
            DefaultTableModel pm = (DefaultTableModel) patientRegTable.getModel();
            for (int i=0;i<pm.getRowCount();i++){ Object idv = pm.getValueAt(i,0); if (idv!=null && patientId.equals(String.valueOf(idv))) { existsLocal = true; expectedName = String.valueOf(pm.getValueAt(i,1)); break; }}
        } catch (Throwable ignored) {}
        if (!existsLocal) { warn("Patient ID not found"); return; }
        if (!patientName.isEmpty() && expectedName != null && !expectedName.equalsIgnoreCase(patientName)) {
            int c = confirm("Patient name does not match records ("+expectedName+"). Continue?"); if (c != JOptionPane.YES_OPTION) return;
        }

        String doctorSel = (doctorCombo.getItemCount()>0 && doctorCombo.isEnabled()) ? String.valueOf(doctorCombo.getSelectedItem()) : null;
        if (doctorSel == null || doctorSel.startsWith("(No doctors")) { warn("Select an assigned doctor"); return; }

        java.util.List<String> scopes = scopeList.getSelectedValuesList();
        if (scopes == null || scopes.isEmpty()) { warn("Select at least one scope"); return; }
        if (scopes.contains("Other") && otherScopeDetail.getText().trim().isEmpty()) { warn("Provide details for 'Other' scope"); return; }

        String fromStr = fromDateField.getText().trim(); String toStr = toDateField.getText().trim();
        LocalDate from = null, to = null; try { if (!fromStr.isEmpty()) from = LocalDate.parse(fromStr); } catch (Exception ex) { warn("From Date must be YYYY-MM-DD"); return; }
        try { if (!toStr.isEmpty()) to = LocalDate.parse(toStr); } catch (Exception ex) { warn("To Date must be YYYY-MM-DD"); return; }
        if (from != null && to != null && to.isBefore(from)) { warn("To Date must be on or after From Date"); return; }

        String urgency = String.valueOf(urgencyCombo.getSelectedItem());
        if ("High".equalsIgnoreCase(urgency) && urgencyJustification.getText().trim().length() < 10) {
            warn("Please provide a brief justification (at least 10 characters) for High urgency"); return;
        }

        // Consent/emergency rules: allow either consentOnFile or emergencyException; at least one must be checked
        if (!consentOnFile.isSelected() && !emergencyException.isSelected()) { warn("Select 'Consent on file' or 'Emergency exception'"); return; }
        if (emergencyException.isSelected() && emergencyReason.getText().trim().isEmpty()) { warn("Provide an emergency reason"); return; }

        // Visibility fixed per confirmation: Doctor and Staff; no action needed
        boolean notify = notifyInbox.isSelected();

        // Submit: for now, record a notification to the doctor via system inbox and show a confirmation
        try {
            if (notify) {
                Service.NotificationService.getInstance().notifyUser(doctorSel,
                    "Medical Record Request: Patient=" + patientId + (patientName.isEmpty()?"":" ("+patientName+")") +
                    ", Scope=" + String.join(", ", scopes) +
                    (scopes.contains("Other")? "; Other=" + otherScopeDetail.getText().trim() : "") +
                    (from!=null? "; From="+from.toString():"") + (to!=null? "; To="+to.toString():"") +
                    "; Urgency=" + urgency +
                    ("High".equalsIgnoreCase(urgency)? "; Reason=" + urgencyJustification.getText().trim() : "") +
                    (consentOnFile.isSelected()? "; Consent on file" : "; Emergency: " + emergencyReason.getText().trim())
                );
            }
        } catch (Throwable ignored) { /* if NotificationService unavailable, continue */ }

        info("Request submitted to " + doctorSel + " via System Inbox.");
    }

    // BILLING ---------------------------------------------------------
    private JPanel buildBillingPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(COLOR_BG);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        JLabel header = new JLabel("Billing & Payment History", SwingConstants.LEFT);
        header.setFont(FONT_SECTION);
        header.setForeground(COLOR_PRIMARY.darker());
        header.setBorder(new EmptyBorder(0, 0, 8, 0));
        topPanel.add(header, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setOpaque(false);
        searchPanel.add(new JLabel("Search Billing:"));
        JTextField searchField = new JTextField(20);
        searchPanel.add(searchField);
        topPanel.add(searchPanel, BorderLayout.SOUTH);

        JPanel actionPanelBill = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); actionPanelBill.setOpaque(false); JButton billExport = new JButton("Export"); styleSecondaryButton(billExport); billExport.addActionListener(e -> openExportBillingDialog()); JButton billMark = new JButton("Mark Paid"); styleSecondaryButton(billMark); billMark.addActionListener(e -> openMarkPaidDialog());
        JButton billAdd = new JButton("Add"); styleSecondaryButton(billAdd); billAdd.addActionListener(e -> openAddPaymentDialog());
        billAdd.setPreferredSize(new Dimension(80,34));
        actionPanelBill.add(billExport); actionPanelBill.add(billMark); actionPanelBill.add(billAdd); topPanel.add(actionPanelBill, BorderLayout.EAST);
        root.add(topPanel, BorderLayout.NORTH);

        String[] cols = {"Date", "Patient", "Amount", "Description", "Status"};
        Object[][] data = {{"2025-01-10", "John Doe", "$120", "Consultation", "Unpaid"}, {"2025-01-11", "Jane Smith", "$200", "Lab Test", "Paid"}};
        billingTable = new JTable(new DefaultTableModel(data, cols));

        // Add search listener
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterBillingTable(searchField.getText()); }
            public void removeUpdate(DocumentEvent e) { filterBillingTable(searchField.getText()); }
            public void changedUpdate(DocumentEvent e) { filterBillingTable(searchField.getText()); }
        });

        root.add(new JScrollPane(billingTable), BorderLayout.CENTER);

        // actions moved to headerActions (no bottom footer)
        return root;
    }

    // LAB -------------------------------------------------------------
    private JPanel buildLabPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(COLOR_BG);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        JLabel header = new JLabel("Laboratory & Test Management", SwingConstants.LEFT);
        header.setFont(FONT_SECTION);
        header.setForeground(COLOR_PRIMARY.darker());
        header.setBorder(new EmptyBorder(0, 0, 8, 0));
        topPanel.add(header, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setOpaque(false);
        searchPanel.add(new JLabel("Search Lab Tests:"));
        JTextField searchField = new JTextField(20);
        searchPanel.add(searchField);
        topPanel.add(searchPanel, BorderLayout.SOUTH);

        // Toolbar: only Request and Refresh
        JPanel actionPanelLab = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); actionPanelLab.setOpaque(false);
        JButton labRequest = new JButton("Request"); styleSecondaryButton(labRequest); labRequest.addActionListener(e -> openRequestLabTestDialog());
        JButton labRefresh = new JButton("Refresh"); styleSecondaryButton(labRefresh); labRefresh.addActionListener(e -> { try { ((DefaultTableModel) labTable.getModel()).fireTableDataChanged(); info("Lab list refreshed."); } catch (Exception ignored) { info("Refresh complete."); } });
        actionPanelLab.add(labRequest); actionPanelLab.add(labRefresh);
        topPanel.add(actionPanelLab, BorderLayout.EAST);
        root.add(topPanel, BorderLayout.NORTH);

        String[] cols = {"Test ID", "Patient", "Test", "Status"};
        Object[][] data = {{501, "John Doe", "CBC", "Pending"}, {502, "Jane Smith", "X-Ray", "Completed"}};
        labTable = new JTable(new DefaultTableModel(data, cols));

        // Add search listener
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterLabTable(searchField.getText()); }
            public void removeUpdate(DocumentEvent e) { filterLabTable(searchField.getText()); }
            public void changedUpdate(DocumentEvent e) { filterLabTable(searchField.getText()); }
        });

        root.add(new JScrollPane(labTable), BorderLayout.CENTER);

        // actions moved to headerActions (no bottom toolbar)
        return root;
    }

    // NEW: Lab Test Request dialog (Staff -> Doctor)
    private void openRequestLabTestDialog() {
        JPanel form = new JPanel(new GridBagLayout()); form.setBorder(new EmptyBorder(12,12,12,12));
        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(8,8,8,8); gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridx=0; gbc.gridy=0; gbc.weightx=0;

        // Patient and doctor
        JTextField patientIdField = new JTextField();
        JTextField patientNameField = new JTextField(); patientNameField.setToolTipText("Optional; validated against ID if known");
        JComboBox<String> doctorCombo = new JComboBox<>(); doctorCombo.setEditable(false);
        JLabel doctorHint = new JLabel("(Assigned doctors only)"); doctorHint.setForeground(Color.GRAY);

        // Test selection (multi-select)
        String[] tests = new String[]{
            // Serology/Immunology
            "Hepatitis Panel", "HIV Ag/Ab",
            // Microbiology
            "Urine Culture", "Blood Culture",
            // Urinalysis
            "Urinalysis (Complete)",
            // Imaging (optional under lab)
            "X-Ray", "Ultrasound", "ECG",
            // Other
            "Other Test"
        };
        JList<String> testList = new JList<>(tests); testList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane testScroll = new JScrollPane(testList); testScroll.setPreferredSize(new Dimension(320, 140));
        JTextField otherTestDetail = new JTextField(); otherTestDetail.setEnabled(false);

        // Imaging details if any imaging selected
        JTextField imagingRegionField = new JTextField(); imagingRegionField.setEnabled(false);
        JLabel imagingHint = new JLabel("(Required if Imaging selected)"); imagingHint.setForeground(Color.GRAY);

        // Specimen/Collection
        JComboBox<String> specimenType = new JComboBox<>(new String[]{"Blood","Urine","Stool","Sputum","Swab","Other"});
        JCheckBox fastingRequired = new JCheckBox("Fasting required");
        JTextField collectionLocation = new JTextField();
        JTextField preferredCollectionDT = new JTextField(); preferredCollectionDT.setToolTipText("YYYY-MM-DDTHH:mm (optional)");

        // Clinical notes and urgency
        JTextField clinicalNotes = new JTextField();
        JComboBox<String> urgencyCombo = new JComboBox<>(new String[]{"Routine","STAT"});
        JTextField statReason = new JTextField(); statReason.setEnabled(false);

        // Consent (for sensitive tests like HIV)
        JCheckBox consentOnFile = new JCheckBox("Consent on file"); consentOnFile.setEnabled(false);
        JCheckBox emergencyException = new JCheckBox("Emergency exception"); emergencyException.setEnabled(false);
        JTextField emergencyReason = new JTextField(); emergencyReason.setEnabled(false);

        // Visibility/Notifications (fixed)
        JLabel visibility = new JLabel("Visibility: Doctor and Staff");
        JCheckBox notifyInbox = new JCheckBox("Notify via System Inbox"); notifyInbox.setSelected(true);

        // Wire interactions
        testList.addListSelectionListener(e -> {
            java.util.List<String> sel = testList.getSelectedValuesList();
            boolean hasOther = sel.contains("Other Test");
            boolean hasImaging = sel.contains("X-Ray") || sel.contains("Ultrasound") || sel.contains("ECG");
            boolean hasSensitive = sel.contains("HIV Ag/Ab");
            otherTestDetail.setEnabled(hasOther);
            imagingRegionField.setEnabled(hasImaging);
            consentOnFile.setEnabled(hasSensitive);
            emergencyException.setEnabled(hasSensitive);
            emergencyReason.setEnabled(hasSensitive && emergencyException.isSelected());
        });
        emergencyException.addActionListener(e -> {
            java.util.List<String> sel = testList.getSelectedValuesList();
            boolean hasSensitive = sel.contains("HIV Ag/Ab");
            emergencyReason.setEnabled(hasSensitive && emergencyException.isSelected());
        });
        urgencyCombo.addActionListener(e -> statReason.setEnabled("STAT".equalsIgnoreCase(String.valueOf(urgencyCombo.getSelectedItem()))));

        // Helper to add rows
        java.util.function.BiConsumer<String, Component> addRow = (label, comp) -> { gbc.gridx=0; gbc.weightx=0; form.add(new JLabel(label), gbc); gbc.gridx=1; gbc.weightx=1; form.add(comp, gbc); gbc.gridy++; };

        addRow.accept("Patient ID:", patientIdField);
        addRow.accept("Patient Name (optional):", patientNameField);
        addRow.accept("Assigned Doctor:", doctorCombo);
        addRow.accept("", doctorHint);
        addRow.accept("Tests:", testScroll);
        addRow.accept("Other test details:", otherTestDetail);
        addRow.accept("Imaging Body Part/Region:", imagingRegionField);
        addRow.accept("", imagingHint);
        addRow.accept("Specimen Type:", specimenType);
        addRow.accept("Fasting:", fastingRequired);
        addRow.accept("Collection Location:", collectionLocation);
        addRow.accept("Preferred Collection (YYYY-MM-DDTHH:mm):", preferredCollectionDT);
        addRow.accept("Clinical Notes / Justification:", clinicalNotes);
        addRow.accept("Urgency:", urgencyCombo);
        addRow.accept("STAT reason:", statReason);
        addRow.accept("Consent:", consentOnFile);
        addRow.accept("Emergency Exception:", emergencyException);
        addRow.accept("Emergency Reason:", emergencyReason);
        addRow.accept("Visibility:", visibility);
        addRow.accept("Notifications:", notifyInbox);

        // Populate assigned doctors when patient ID changes
        patientIdField.getDocument().addDocumentListener(new DocumentListener(){
            private void refresh(){
                String pid = patientIdField.getText().trim();
                doctorCombo.removeAllItems();
                if (pid.isEmpty()) { doctorCombo.addItem("(Enter Patient ID)"); doctorCombo.setEnabled(false); return; }
                // Validate patient exists via local table (hidden ID column index 0)
                boolean existsLocal = false;
                try {
                    DefaultTableModel pm = (DefaultTableModel) patientRegTable.getModel();
                    for (int i=0;i<pm.getRowCount();i++){ Object v = pm.getValueAt(i,0); if (v!=null && pid.equals(String.valueOf(v))) { existsLocal = true; break; }}
                } catch (Throwable ignored) {}
                if (!existsLocal) { doctorCombo.addItem("(Unknown Patient ID)"); doctorCombo.setEnabled(false); return; }
                // Assigned-only: if no service to fetch assignments, show placeholder
                doctorCombo.addItem("(No assigned doctors)"); doctorCombo.setEnabled(false);
            }
            public void insertUpdate(DocumentEvent e){ refresh(); }
            public void removeUpdate(DocumentEvent e){ refresh(); }
            public void changedUpdate(DocumentEvent e){ refresh(); }
        });

        int res = showLargeDialog(form, "Request Lab Test");
        if (res != JOptionPane.OK_OPTION) return;

        // Validate
        String patientId = patientIdField.getText().trim();
        if (patientId.isEmpty()) { warn("Patient ID is required"); return; }
        // check patient exists in table
        boolean existsLocal = false; String expectedName = null;
        try { DefaultTableModel pm = (DefaultTableModel) patientRegTable.getModel(); for (int i=0;i<pm.getRowCount();i++){ Object v0 = pm.getValueAt(i,0); if (v0!=null && patientId.equals(String.valueOf(v0))) { existsLocal = true; expectedName = String.valueOf(pm.getValueAt(i,1)); break; } } } catch (Throwable ignored) {}
        if (!existsLocal) { warn("Patient ID not found"); return; }
        String givenName = patientNameField.getText().trim(); if (!givenName.isEmpty() && expectedName != null && !expectedName.equalsIgnoreCase(givenName)) { int c = confirm("Patient name does not match records ("+expectedName+"). Continue?"); if (c != JOptionPane.YES_OPTION) return; }

        String doctorSel = (doctorCombo.getItemCount()>0 && doctorCombo.isEnabled()) ? String.valueOf(doctorCombo.getSelectedItem()) : null;
        if (doctorSel == null || doctorSel.startsWith("(No doctors")) { warn("Select an assigned doctor"); return; }

        java.util.List<String> selectedTests = testList.getSelectedValuesList();
        if (selectedTests == null || selectedTests.isEmpty()) { warn("Select at least one test"); return; }
        if (selectedTests.contains("Other Test") && otherTestDetail.getText().trim().isEmpty()) { warn("Provide details for 'Other Test'"); return; }
        boolean imagingSelected = selectedTests.contains("X-Ray") || selectedTests.contains("Ultrasound") || selectedTests.contains("ECG");
        if (imagingSelected && imagingRegionField.getText().trim().isEmpty()) { warn("Please specify Imaging Body Part/Region"); return; }
        // Specimen required
        if (specimenType.getSelectedItem()==null || String.valueOf(specimenType.getSelectedItem()).isBlank()) { warn("Select a specimen type"); return; }

        String dtStr = preferredCollectionDT.getText().trim();
        LocalDateTime when = null; if (!dtStr.isEmpty()) {
            try { when = LocalDateTime.parse(dtStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")); if (when.isBefore(LocalDateTime.now())) { warn("Preferred collection time cannot be in the past"); return; } }
            catch (Exception ex) { warn("Preferred collection must be in format YYYY-MM-DDTHH:MM"); return; }
        }

        if (clinicalNotes.getText().trim().length() < 10) { warn("Please provide a short clinical justification (min 10 characters)"); return; }
        String urgency = String.valueOf(urgencyCombo.getSelectedItem());
        if ("STAT".equalsIgnoreCase(urgency) && statReason.getText().trim().length() < 10) { warn("Please provide a STAT reason (min 10 characters)"); return; }

        boolean sensitive = selectedTests.contains("HIV Ag/Ab");
        if (sensitive) {
            if (!consentOnFile.isSelected() && !emergencyException.isSelected()) { warn("Select 'Consent on file' or 'Emergency exception' for sensitive tests"); return; }
            if (emergencyException.isSelected() && emergencyReason.getText().trim().isEmpty()) { warn("Provide an emergency reason"); return; }
        }

        boolean notify = notifyInbox.isSelected();

        try {
            if (notify) {
                String summary = "Lab Test Request: Patient=" + patientId + (givenName.isEmpty()?"":" ("+givenName+")") +
                    ", Tests=" + String.join(", ", selectedTests) +
                    (selectedTests.contains("Other Test")? "; Other=" + otherTestDetail.getText().trim() : "") +
                    (imagingSelected? "; ImagingRegion=" + imagingRegionField.getText().trim() : "") +
                    "; Specimen=" + String.valueOf(specimenType.getSelectedItem()) +
                    (fastingRequired.isSelected()? "; Fasting" : "") +
                    (!collectionLocation.getText().trim().isEmpty()? "; Location=" + collectionLocation.getText().trim() : "") +
                    (when!=null? "; Preferred=" + when.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "") +
                    "; Urgency=" + urgency + ("STAT".equalsIgnoreCase(urgency)? "; Reason=" + statReason.getText().trim() : "") +
                    (sensitive? (consentOnFile.isSelected()? "; Consent on file" : "; Emergency: " + emergencyReason.getText().trim()) : "");
                Service.NotificationService.getInstance().notifyUser(doctorSel, summary);
            }
        } catch (Throwable ignored) {}

        info("Lab test request submitted to " + doctorSel + " via System Inbox.");
    }

    // ADMISSION -------------------------------------------------------
    private JPanel buildAdmissionPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(COLOR_BG);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        JLabel header = new JLabel("Admission & Discharge Management", SwingConstants.LEFT);
        header.setFont(FONT_SECTION);
        header.setForeground(COLOR_PRIMARY.darker());
        header.setBorder(new EmptyBorder(0, 0, 8, 0));
        topPanel.add(header, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setOpaque(false);
        searchPanel.add(new JLabel("Search Admissions:"));
        JTextField searchField = new JTextField(20);
        searchPanel.add(searchField);
        topPanel.add(searchPanel, BorderLayout.SOUTH);

        JPanel actionPanelAdm = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); actionPanelAdm.setOpaque(false); JButton admAdmit = new JButton("Admit"); styleSecondaryButton(admAdmit); admAdmit.addActionListener(e -> StaffDashboardPanel.this.openAdmitPatientDialog()); JButton admDis = new JButton("Discharge"); styleSecondaryButton(admDis); admDis.addActionListener(e -> StaffDashboardPanel.this.openDischargePatientDialog()); JButton admTrans = new JButton("Transfer"); styleSecondaryButton(admTrans); admTrans.addActionListener(e -> StaffDashboardPanel.this.openTransferPatientDialog()); actionPanelAdm.add(admAdmit); actionPanelAdm.add(admDis); actionPanelAdm.add(admTrans); topPanel.add(actionPanelAdm, BorderLayout.EAST);
        root.add(topPanel, BorderLayout.NORTH);

        String[] cols = {"Admission ID", "Patient", "Room", "Status"};
        Object[][] data = {{801, "John Doe", "101A", "Admitted"}, {802, "Jane Smith", "102B", "Discharged"}};
        admissionTable = new JTable(new DefaultTableModel(data, cols));

        // Add search listener
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterAdmissionTable(searchField.getText()); }
            public void removeUpdate(DocumentEvent e) { filterAdmissionTable(searchField.getText()); }
            public void changedUpdate(DocumentEvent e) { filterAdmissionTable(searchField.getText()); }
        });

        root.add(new JScrollPane(admissionTable), BorderLayout.CENTER);

        // actions moved to headerActions (no bottom toolbar)
        return root;
    }

    // USER GUIDE ------------------------------------------------------
    private JPanel buildGuidePanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(COLOR_BG);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        root.add(sectionHeader("User Guide"), BorderLayout.NORTH);

        JTextArea area = new JTextArea(
            "Welcome to the Staff User Guide.\n\n" +
            "Navigation:\n- Use the left menu to manage Summary, Patient Registration, Medical Records, Billing, Lab, Admission, and this Guide.\n\n" +
            "Patient Registration:\n- Add, view, deactivate patients; use the search box to filter.\n\n" +
            "Medical Records:\n- Add, view, remove treatment records.\n\n" +
            "Billing:\n- Search bills, mark as paid, and export history.\n\n" +
            "Lab:\n- Add tests, update status, and complete tests.\n\n" +
            "Admission:\n- Admit, discharge, and transfer patients.\n\n" +
            "Tips:\n- Use global search/filter when available to narrow results.\n- Toolbar buttons at the bottom provide common actions.");
        area.setEditable(false);
        area.setFont(FONT_NORMAL);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        root.add(new JScrollPane(area), BorderLayout.CENTER);
        return root;
    }

    // HELPERS ---------------------------------------------------------
    private JLabel sectionHeader(String text) {
        JLabel l = new JLabel(text, SwingConstants.LEFT);
        l.setFont(FONT_SECTION);
        l.setForeground(COLOR_PRIMARY.darker());
        l.setBorder(new EmptyBorder(0, 0, 8, 0));
        return l;
    }
    private void styleToolbarButton(JToolBar bar, String text, Runnable action) {
        JButton b = new JButton(text); b.setFont(FONT_NORMAL); b.addActionListener(e -> action.run()); bar.add(b);
    }
    private void styleSecondaryButton(JButton b) {
        b.setFont(FONT_NORMAL); b.setBackground(Color.WHITE); b.setBorder(new LineBorder(COLOR_BORDER)); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() { @Override public void mouseEntered(MouseEvent e){ b.setBackground(COLOR_PRIMARY_HOVER);} @Override public void mouseExited(MouseEvent e){ b.setBackground(Color.WHITE);} });
    }

    // DIALOG METHODS (Patient Registration)
    private void openAddPatientDialog() {
        // Patient-only admin-style dialog: collects personal/contact info + emergency contact
        UserService userService = UserService.getInstance();
        JPanel container = new JPanel(new BorderLayout(16, 16));
        container.setBorder(new EmptyBorder(12,12,12,12));

        // Personal/contact fields
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(14,14,14,14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12,12,12,12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;

        // Use wider text fields (consistent preferred size)
        Dimension wideField = new Dimension(360, 30);

        JTextField surnameField = new JTextField(); surnameField.setColumns(20); surnameField.setPreferredSize(wideField);
        JTextField givenField = new JTextField(); givenField.setColumns(20); givenField.setPreferredSize(wideField);
        JTextField middleField = new JTextField(); middleField.setColumns(20); middleField.setPreferredSize(wideField);
        JTextField dobField = new JTextField(); dobField.setColumns(12); dobField.setPreferredSize(new Dimension(180, 30));
        JComboBox<String> genderBox = new JComboBox<>(new String[]{"Male","Female","Other"}); genderBox.setPreferredSize(new Dimension(160, 30));
        JTextField phoneField = new JTextField(); phoneField.setColumns(14); phoneField.setPreferredSize(new Dimension(200, 30));
        JTextField emailField = new JTextField(); emailField.setColumns(20); emailField.setPreferredSize(wideField);
        JTextField addressField = new JTextField(); addressField.setColumns(30); addressField.setPreferredSize(wideField);
        JTextField emergencyNameField = new JTextField(); emergencyNameField.setColumns(20); emergencyNameField.setPreferredSize(wideField);
        JTextField emergencyContactField = new JTextField(); emergencyContactField.setColumns(14); emergencyContactField.setPreferredSize(new Dimension(200,30));

        // add helper
        BiConsumer<String, Component> add = (lbl, comp) -> {
            gbc.gridx = 0; gbc.weightx = 0; form.add(new JLabel(lbl), gbc);
            gbc.gridx = 1; gbc.weightx = 1; form.add(comp, gbc);
            gbc.gridy++;
        };
        add.accept("Surname:", surnameField); add.accept("Given Name:", givenField); add.accept("Middle Name:", middleField);
        add.accept("DOB (YYYY-MM-DD):", dobField); add.accept("Sex/Gender:", genderBox);
        add.accept("Phone:", phoneField); add.accept("Email (username):", emailField); add.accept("Address:", addressField);
        add.accept("Emergency Contact Name:", emergencyNameField); add.accept("Emergency Contact Number:", emergencyContactField);

        // Account preview (username = email, password auto-generated shown)
        JPanel accountPanel = new JPanel(new GridBagLayout());
        GridBagConstraints ag = new GridBagConstraints(); ag.insets = new Insets(8,8,8,8); ag.fill = GridBagConstraints.HORIZONTAL; ag.gridx=0; ag.gridy=0;
        JTextField usernameField = new JTextField(); usernameField.setEditable(false); usernameField.setPreferredSize(wideField);
        JPasswordField passwordField = new JPasswordField(); passwordField.setEditable(false); passwordField.setPreferredSize(new Dimension(240,30));
        ag.gridx=0; accountPanel.add(new JLabel("Account Username:"), ag); ag.gridx=1; accountPanel.add(usernameField, ag); ag.gridy++; ag.gridx=0; accountPanel.add(new JLabel("Password (generated):"), ag); ag.gridx=1; accountPanel.add(passwordField, ag);
        ag.gridx=0; ag.gridy++;
        accountPanel.add(new JLabel("(Note: Password will be auto-generated and sent to the patient)"), ag);

        // Bind email -> username preview
        emailField.getDocument().addDocumentListener(new DocumentListener(){ public void insertUpdate(DocumentEvent e){ usernameField.setText(emailField.getText().trim()); } public void removeUpdate(DocumentEvent e){ usernameField.setText(emailField.getText().trim()); } public void changedUpdate(DocumentEvent e){ usernameField.setText(emailField.getText().trim()); } });

        container.add(form, BorderLayout.CENTER);

        // Build right column to mirror Admin layout: identification block + documents + account panel
        JPanel right = new JPanel(); right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS)); right.setBackground(Color.WHITE);

        // Identification block (visible for patient in Admin dialog)
        JPanel idBlock = new JPanel(new GridBagLayout()); idBlock.setBackground(Color.WHITE);
        idBlock.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_BORDER), "Identification (optional)"));
        GridBagConstraints idgbc = new GridBagConstraints(); idgbc.insets = new Insets(10,10,10,10); idgbc.fill = GridBagConstraints.HORIZONTAL; idgbc.gridx=0; idgbc.gridy=0;
        idgbc.gridx=0; idBlock.add(new JLabel("ID Type:"), idgbc); idgbc.gridx=1; idBlock.add(new JComboBox<>(new String[]{"National ID","PhilHealth","Driver's License","Passport","Other"}), idgbc); idgbc.gridy++;
        idgbc.gridx=0; idBlock.add(new JLabel("ID Number:"), idgbc); idgbc.gridx=1; JTextField idNumberField = new JTextField(); idNumberField.setPreferredSize(new Dimension(240,30)); idBlock.add(idNumberField, idgbc); idgbc.gridy++;
        JCheckBox minorCheck = new JCheckBox("Minor (under 18) — allow Student ID instead (optional)"); idgbc.gridx=0; idgbc.gridwidth=2; idBlock.add(minorCheck, idgbc); idgbc.gridy++; idgbc.gridwidth=1;
        idgbc.gridx=0; idBlock.add(new JLabel("Student ID (if minor):"), idgbc); idgbc.gridx=1; JTextField studentIdField = new JTextField(); studentIdField.setEnabled(false); studentIdField.setPreferredSize(new Dimension(200,30)); idBlock.add(studentIdField, idgbc); idgbc.gridy++;
        minorCheck.addActionListener(e -> studentIdField.setEnabled(minorCheck.isSelected()));

        // System / documents panel (2x2, ID front/back)
        JPanel sysdocs = new JPanel(new GridBagLayout()); sysdocs.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_BORDER), "System / Documents")); sysdocs.setBackground(Color.WHITE);
        GridBagConstraints sgbc = new GridBagConstraints(); sgbc.insets = new Insets(10,10,10,10); sgbc.fill = GridBagConstraints.HORIZONTAL; sgbc.gridx=0; sgbc.gridy=0;
        JTextField picField = new JTextField(); picField.setEditable(false); picField.setPreferredSize(new Dimension(240,30));
        JTextField idFrontField = new JTextField(); idFrontField.setEditable(false); idFrontField.setPreferredSize(new Dimension(240,30));
        JTextField idBackField = new JTextField(); idBackField.setEditable(false); idBackField.setPreferredSize(new Dimension(240,30));
        JButton picBtn = new JButton("Choose 2x2"); picBtn.addActionListener(e->{ JFileChooser fc=new JFileChooser(); fc.setFileFilter(new FileNameExtensionFilter("Image files","jpg","jpeg","png")); if(fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) picField.setText(fc.getSelectedFile().getAbsolutePath()); });
        JButton idFrontBtn = new JButton("ID Front"); idFrontBtn.addActionListener(e->{ JFileChooser fc=new JFileChooser(); fc.setFileFilter(new FileNameExtensionFilter("Image/PDF","jpg","jpeg","png","pdf")); if(fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) idFrontField.setText(fc.getSelectedFile().getAbsolutePath()); });
        JButton idBackBtn = new JButton("ID Back"); idBackBtn.addActionListener(e->{ JFileChooser fc=new JFileChooser(); fc.setFileFilter(new FileNameExtensionFilter("Image/PDF","jpg","jpeg","png","pdf")); if(fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) idBackField.setText(fc.getSelectedFile().getAbsolutePath()); });
        sgbc.gridx=0; sgbc.weightx=0; sysdocs.add(new JLabel("2x2 Picture:"), sgbc); sgbc.gridx=1; sgbc.weightx=1; sysdocs.add(picField, sgbc); sgbc.gridx=2; sysdocs.add(picBtn, sgbc); sgbc.gridy++;
        sgbc.gridx=0; sysdocs.add(new JLabel("ID Front:"), sgbc); sgbc.gridx=1; sysdocs.add(idFrontField, sgbc); sgbc.gridx=2; sysdocs.add(idFrontBtn, sgbc); sgbc.gridy++;
        sgbc.gridx=0; sysdocs.add(new JLabel("ID Back:"), sgbc); sgbc.gridx=1; sysdocs.add(idBackField, sgbc); sgbc.gridx=2; sysdocs.add(idBackBtn, sgbc); sgbc.gridy++;

        // Assemble right column
        right.add(idBlock); right.add(Box.createVerticalStrut(14));
        right.add(sysdocs); right.add(Box.createVerticalStrut(14));
        right.add(accountPanel);
        right.add(Box.createVerticalGlue());

        // Place left and right into container using a JSplitPane so alignment matches Admin
        JPanel leftWrap = new JPanel(new BorderLayout()); leftWrap.setBackground(Color.WHITE);
        leftWrap.add(form, BorderLayout.NORTH);
        // ensure left form has a comfortable preferred width
        leftWrap.setPreferredSize(new Dimension(700, 760));

        JPanel rightWrap = new JPanel(new BorderLayout()); rightWrap.setBackground(Color.WHITE);
        // stack right column items with padding
        JPanel rightContent = new JPanel(); rightContent.setLayout(new BoxLayout(rightContent, BoxLayout.Y_AXIS)); rightContent.setBackground(Color.WHITE);
        rightContent.add(idBlock);
        rightContent.add(Box.createVerticalStrut(10));
        rightContent.add(sysdocs);
        rightContent.add(Box.createVerticalStrut(10));
        rightContent.add(accountPanel);
        rightContent.add(Box.createVerticalGlue());
        rightWrap.add(rightContent, BorderLayout.NORTH);
        rightWrap.setPreferredSize(new Dimension(480, 760));

        JScrollPane leftScroll = new JScrollPane(leftWrap); leftScroll.setBorder(null); leftScroll.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane rightScroll = new JScrollPane(rightWrap); rightScroll.setBorder(null); rightScroll.getVerticalScrollBar().setUnitIncrement(16);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightScroll);
        split.setResizeWeight(0.65); // left gets more space
        split.setContinuousLayout(true);
        split.setOneTouchExpandable(true);
        split.setDividerLocation(720);
        container.add(split, BorderLayout.CENTER);

        // Dialog footer
        int res = showLargeDialog(container, "Register New Patient (Staff)");
        if (res != JOptionPane.OK_OPTION) return;

        // Validate required fields
        String fn = givenField.getText().trim(); String ln = surnameField.getText().trim(); String dobStr = dobField.getText().trim();
        String gen = (genderBox.getSelectedItem()==null)?"":genderBox.getSelectedItem().toString(); String ph = phoneField.getText().trim();
        String em = emailField.getText().trim(); String addr = addressField.getText().trim(); String emergName = emergencyNameField.getText().trim(); String emergContact = emergencyContactField.getText().trim();

        java.util.List<String> missing = new java.util.ArrayList<>();
        if (fn.isEmpty()) missing.add("Given Name"); if (ln.isEmpty()) missing.add("Surname"); if (dobStr.isEmpty()) missing.add("DOB"); if (gen.isEmpty()) missing.add("Gender");
        if (ph.isEmpty()) missing.add("Phone"); if (em.isEmpty()) missing.add("Email"); if (addr.isEmpty()) missing.add("Address");
        if (!missing.isEmpty()) { warn("Please fill required fields: " + String.join(", ", missing)); return; }

        final java.time.LocalDate dob; try { dob = java.time.LocalDate.parse(dobStr); } catch (Exception ex) { warn("DOB format should be YYYY-MM-DD"); return; }

        // email validation
        if (!em.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) { warn("Please provide a valid email address (used as username)."); return; }

        // Create account: username = email
        String uname = em;
        String plainPw = userService.generateNextPlainPassword(); passwordField.setText(plainPw);
        char[] pw = plainPw.toCharArray();
        try {
            userService.createUser(uname, pw, Model.Role.PATIENT);
            // clear pw array
            Arrays.fill(pw, '\0');
            // Populate profile and create Patient model linked to user
            userService.findByUsername(uname).ifPresent(u -> {
                // populate profile
                PatientService.PatientProfile profile = PatientService.getInstance().getProfileByUsername(u.getUsername());
                profile.surname = ln; profile.firstName = fn; profile.middleName = middleField.getText().trim();
                profile.dateOfBirth = dobStr; profile.gender = gen; profile.phone = ph; profile.email = em; profile.address = addr;
                profile.emergencyContactName = emergName; profile.emergencyContactNumber = emergContact;
                PatientService.getInstance().saveProfile(u.getUsername(), profile);

                // create Patient model for user
                try {
                    Patient p = PatientService.getInstance().createPatientForUser(u, fn, ln, dob, gen, ph, addr);
                    // update patient table
                    DefaultTableModel m = (DefaultTableModel) patientRegTable.getModel();
                    int age = 0; try { age = Math.max(0, java.time.Period.between(dob, java.time.LocalDate.now()).getYears()); } catch (Exception ignored) {}
                    m.addRow(new Object[]{p.getId(), p.getFirstName() + " " + p.getLastName(), age, p.getGender(), "Active"});
                } catch (Exception ignored) {
                    // fallback: if createPatientForUser fails, do nothing — profile still saved
                }
            });

            JOptionPane.showMessageDialog(this, "Patient account created (username: " + uname + ")\nGenerated password: " + plainPw, "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Failed to create user: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void openViewPatientDialog() {
        int row = patientRegTable.getSelectedRow(); if (row==-1){warn("Select a patient first"); return;}
        DefaultTableModel m=(DefaultTableModel)patientRegTable.getModel();
        info(String.format("ID: %s\nName: %s\nAge: %s\nGender: %s\nStatus: %s",
            m.getValueAt(row,0), m.getValueAt(row,1), m.getValueAt(row,2), m.getValueAt(row,3), m.getValueAt(row,4)));
    }
    private void openDeactivatePatientDialog() {
        int row = patientRegTable.getSelectedRow(); if (row==-1){warn("Select a patient first"); return;}
        int c = confirm("Deactivate this patient?"); if (c==JOptionPane.YES_OPTION){ ((DefaultTableModel)patientRegTable.getModel()).setValueAt("Inactive", row, 4); info("Patient deactivated."); }
    }

    // MEDICAL RECORDS DIALOGS ----------------------------------------
    private void openAddMedicalRecordDialog() {
        JPanel p=new JPanel(new GridLayout(4,2,8,8)); p.setBorder(new EmptyBorder(10,10,10,10));
        JTextField patient=field(p,"Patient:"); JTextField type=field(p,"Type:"); JTextField notes=field(p,"Notes:"); field(p,"Extra:");
        if (showDialog(p,"Add Medical Record")==JOptionPane.OK_OPTION){ if(!patient.getText().isEmpty()){ DefaultTableModel m=(DefaultTableModel)medicalRecordTable.getModel(); m.addRow(new Object[]{m.getRowCount()+101, patient.getText(), type.getText(), notes.getText()}); info("Record added."); } else warn("Patient required"); }
    }
    private void openViewMedicalRecordDialog() {
        int r=medicalRecordTable.getSelectedRow(); if(r==-1){warn("Select a record first"); return;} DefaultTableModel m=(DefaultTableModel)medicalRecordTable.getModel();
        info(String.format("Record: %s\nPatient: %s\nType: %s\nNotes: %s",m.getValueAt(r,0),m.getValueAt(r,1),m.getValueAt(r,2),m.getValueAt(r,3)));
    }
    private void openDeleteMedicalRecordDialog() {
        int r=medicalRecordTable.getSelectedRow(); if(r==-1){warn("Select a record first"); return;} if(confirm("Delete this record?")==JOptionPane.YES_OPTION){ ((DefaultTableModel)medicalRecordTable.getModel()).removeRow(r); info("Record deleted."); }
    }

    // BILLING DIALOGS ------------------------------------------------
    private void openExportBillingDialog() { info("Export billing (placeholder)"); }
    private void openMarkPaidDialog() {
        int r=billingTable.getSelectedRow(); if(r==-1){warn("Select a bill first"); return;} ((DefaultTableModel)billingTable.getModel()).setValueAt("Paid", r, 4); info("Marked as paid.");
    }
    private void openAddPaymentDialog() {
        int row = billingTable.getSelectedRow();
        if (row == -1) { warn("Select a bill to pay"); return; }
        DefaultTableModel m = (DefaultTableModel) billingTable.getModel();
        String patientId = (String) m.getValueAt(row, 1);
        String amountStr = (String) m.getValueAt(row, 2);
        String description = "Payment for bill on " + m.getValueAt(row, 0); // Date column

        JPanel form = new JPanel(new GridBagLayout()); form.setBorder(new EmptyBorder(10,10,10,10));
        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(8,8,8,8); gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridx=0; gbc.gridy=0;

        JTextField patientField = new JTextField(patientId); patientField.setEditable(false);
        JTextField amountField = new JTextField(amountStr); amountField.setEditable(false);
        JTextField descriptionField = new JTextField(description); descriptionField.setEditable(false);
        JTextField paymentAmount = new JTextField(); paymentAmount.setToolTipText("Enter amount to pay (up to total due)");
        JTextField receiptIdField = new JTextField(); receiptIdField.setEditable(false);

        JComboBox<String> paymentMethod = new JComboBox<>(new String[]{"Cash","Check","Credit Card","Debit Card","Online Transfer"});
        JTextField transactionIdField = new JTextField(); transactionIdField.setEnabled(false);
        JTextField notesField = new JTextField(); notesField.setToolTipText("Optional notes about the payment");

        // Wire payment method to show/hide transaction ID field
        paymentMethod.addActionListener(e -> {
            boolean isOnline = "Online Transfer".equalsIgnoreCase((String) paymentMethod.getSelectedItem());
            transactionIdField.setEnabled(isOnline);
            if (!isOnline) transactionIdField.setText("");
        });

        // Layout helper
        java.util.function.BiConsumer<String, Component> addRow = (label, comp) -> { gbc.gridx=0; gbc.weightx=0; form.add(new JLabel(label), gbc); gbc.gridx=1; gbc.weightx=1; form.add(comp, gbc); gbc.gridy++; };
        addRow.accept("Patient ID:", patientField);
        addRow.accept("Total Amount Due:", amountField);
        addRow.accept("Bill Description:", descriptionField);
        addRow.accept("Payment Amount:", paymentAmount);
        addRow.accept("Receipt ID:", receiptIdField);
        addRow.accept("Payment Method:", paymentMethod);
        addRow.accept("Transaction ID:", transactionIdField);
        addRow.accept("Notes:", notesField);

        int res = showLargeDialog(form, "Add Payment");
        if (res != JOptionPane.OK_OPTION) return;

        // Validate
        String payAmountStr = paymentAmount.getText().trim();
        if (payAmountStr.isEmpty()) { warn("Payment amount is required"); return; }
        double paymentAmt;
        try { paymentAmt = Double.parseDouble(payAmountStr); }
        catch (NumberFormatException e) { warn("Invalid payment amount"); return; }

        // Check if fully paid
        boolean isFullPayment = (amountStr != null && amountStr.equals(payAmountStr));

        // Update billing table: mark as Paid if fully paid, or Partially Paid if not
        if (isFullPayment) {
            m.setValueAt("Paid", row, 4);
            // Generate receipt ID (simple incrementing ID for now)
            String newReceiptId = "R" + (1000 + ((DefaultTableModel)billingTable.getModel()).getRowCount());
            receiptIdField.setText(newReceiptId);
            info("Payment received. Receipt ID: " + newReceiptId);
        } else {
            m.setValueAt("Partially Paid", row, 4);
            info("Partial payment accepted. Please collect remaining balance.");
        }

        // Notify via System Inbox (UI-only, no actual notification sent)
        String method = (String) paymentMethod.getSelectedItem();
        String transactionId = transactionIdField.getText().trim();
        String notes = notesField.getText().trim();
        String notificationMsg = String.format("Payment Received: Patient=%s, Amount=%.2f, Method=%s%s",
            patientId, paymentAmt, method, (transactionId.isEmpty()?"":"; Transaction ID="+transactionId));
        if (!notes.isEmpty()) notificationMsg += "; Notes=" + notes;
        try {
            Service.NotificationService.getInstance().notifyUser("billing-dept", notificationMsg);
        } catch (Throwable ignored) {}

        // Clear fields for next entry
        paymentAmount.setText("");
        receiptIdField.setText("");
        transactionIdField.setText("");
        notesField.setText("");
    }

    // SMALL UTILS -----------------------------------------------------
    private JTextField field(JPanel p, String label){ p.add(new JLabel(label)); JTextField f=new JTextField(); p.add(f); return f; }
    private int showDialog(JPanel panel, String title){ return JOptionPane.showConfirmDialog(this,panel,title,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE); }
    // Replaces small JOptionPane dialogs with a resizable modal JDialog that is larger for readability
    private int showLargeDialog(JPanel panel, String title) {
        final int[] result = {JOptionPane.CLOSED_OPTION};
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.APPLICATION_MODAL);
        dlg.getContentPane().setLayout(new BorderLayout());
        JScrollPane sp = new JScrollPane(panel);
        // Use nearly full screen size to maximize space
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Math.max(900, screen.width - 80);
        int h = Math.max(600, screen.height - 120);
        sp.setPreferredSize(new Dimension(w, h));
        dlg.getContentPane().add(sp, BorderLayout.CENTER);
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("OK"); JButton cancel = new JButton("Cancel");
        ok.addActionListener(e -> { result[0] = JOptionPane.OK_OPTION; dlg.dispose(); });
        cancel.addActionListener(e -> { result[0] = JOptionPane.CANCEL_OPTION; dlg.dispose(); });
        foot.add(cancel); foot.add(ok);
        dlg.getContentPane().add(foot, BorderLayout.SOUTH);
        // make the dialog undecorated BEFORE it becomes displayable
        dlg.setUndecorated(true);
        // prepare size then show once
        dlg.pack(); dlg.setLocation(20, 20);
        dlg.setBounds(0, 0, screen.width, screen.height);
        dlg.setVisible(true);
        return result[0];
    }
    private void info(String msg){ JOptionPane.showMessageDialog(this,msg,"Info",JOptionPane.INFORMATION_MESSAGE); }
    private void warn(String msg){ JOptionPane.showMessageDialog(this,msg,"Warning",JOptionPane.WARNING_MESSAGE); }
    private int confirm(String msg){ return JOptionPane.showConfirmDialog(this,msg,"Confirm",JOptionPane.YES_NO_OPTION); }
    private int parseIntSafe(String s){ try{ return Integer.parseInt(s.trim()); }catch(Exception e){ return 0; } }

    // Safe reflection-backed reader for PatientProfile fields (tolerant to different builds)
    private String safeReadProfileField(Service.PatientService.PatientProfile prof, String... candidates) {
        if (prof == null) return "";
        for (String name : candidates) {
            if (name == null || name.isBlank()) continue;
            try {
                // try public field first
                java.lang.reflect.Field f = prof.getClass().getField(name);
                Object v = f.get(prof);
                if (v != null) return String.valueOf(v);
            } catch (NoSuchFieldException | IllegalAccessException ignored) {}
            // try getter method
            try {
                String getter = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
                java.lang.reflect.Method m = prof.getClass().getMethod(getter);
                Object v = m.invoke(prof);
                if (v != null) return String.valueOf(v);
            } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException ignored) {}
        }
        return "";
    }

    // PUBLIC UPDATE API -----------------------------------------------
    public void updateSummary(int patients, int pendingBills, int labPending){
        if(lblTotalPatients!=null) lblTotalPatients.setText(String.valueOf(patients));
        if(lblPendingBills!=null) lblPendingBills.setText(String.valueOf(pendingBills));
        if(lblLabPending!=null) lblLabPending.setText(String.valueOf(labPending));
    }
    public JTable getPatientRegTable(){ return patientRegTable; }
    public JTable getMedicalRecordTable(){ return medicalRecordTable; }
    public JTable getBillingTable(){ return billingTable; }
    public JTable getLabTable(){ return labTable; }
    public JTable getAdmissionTable(){ return admissionTable; }

    // GlobalSearchable implementation --------------------------------
    @Override
    public Map<String, JTable> getSearchableTables() {
        Map<String, JTable> map = new LinkedHashMap<>();
        if (patientRegTable != null) map.put("patients", patientRegTable);
        if (medicalRecordTable != null) map.put("medical", medicalRecordTable);
        if (billingTable != null) map.put("billing", billingTable);
        if (labTable != null) map.put("lab", labTable);
        if (admissionTable != null) map.put("admission", admissionTable);
        return map;
    }
    @Override
    public void applyGlobalSearch(String query) { globalSearchQuery = (query==null||query.isBlank())?null:query.trim(); refreshAllFilters(); }
    @Override
    public void clearGlobalSearch() { globalSearchQuery = null; refreshAllFilters(); }
    @Override
    public void applyGlobalFilter(String tableName, String columnName, String value) {
        if (tableName==null||columnName==null) return;
        Map<String,String> map = columnFilters.computeIfAbsent(tableName,k->new HashMap<>());
        if (value==null||value.isBlank()) { map.remove(columnName); if(map.isEmpty()) columnFilters.remove(tableName);} else map.put(columnName,value.trim());
        JTable t = getSearchableTables().get(tableName); if (t!=null) applyFiltersToTable(tableName,t);
    }
    @Override
    public void clearGlobalFilter() { columnFilters.clear(); refreshAllFilters(); }
    private void refreshAllFilters(){ getSearchableTables().forEach(this::applyFiltersToTable); }
    @SuppressWarnings("unchecked")
    private void applyFiltersToTable(String logicalName, JTable table){
        if (table.getRowSorter()==null) table.setRowSorter(new TableRowSorter<>(table.getModel()));
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) table.getRowSorter();
        List<RowFilter<TableModel,Object>> filters = new ArrayList<>();
        if (globalSearchQuery!=null){ final String q=globalSearchQuery.toLowerCase(); filters.add(new RowFilter<TableModel,Object>(){ @Override public boolean include(Entry<? extends TableModel, ? extends Object> entry){ for(int i=0;i<entry.getValueCount();i++){ Object v=entry.getValue(i); if(v!=null && v.toString().toLowerCase().contains(q)) return true;} return false; }}); }
        Map<String,String> colMap = columnFilters.get(logicalName);
        if (colMap!=null){ for(Map.Entry<String,String> e: colMap.entrySet()){ String colName=e.getKey(); String val=e.getValue(); if(val==null||val.isBlank()) continue; int colIndex; try{ colIndex=table.getColumnModel().getColumnIndex(colName);} catch(IllegalArgumentException ex){ continue;} final String qv=val.toLowerCase(); filters.add(new RowFilter<TableModel,Object>(){ @Override public boolean include(Entry<? extends TableModel, ? extends Object> entry){ Object v=entry.getValue(colIndex); return v!=null && v.toString().toLowerCase().contains(qv);} }); }}
        if (filters.isEmpty()) sorter.setRowFilter(null); else if(filters.size()==1) sorter.setRowFilter(filters.get(0)); else sorter.setRowFilter(RowFilter.andFilter(filters));
    }

    // PATIENT REGISTRATION FILTERING ----------------------------------
    private void filterPatientRegTable(String query) {
        @SuppressWarnings("unchecked")
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) patientRegTable.getRowSorter();
        if (sorter == null) {
            sorter = new TableRowSorter<>(patientRegTable.getModel());
            patientRegTable.setRowSorter(sorter);
        }
        if (query == null || query.trim().isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            // Search across Name (1), Gender (3), Status (4)
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + query, 1, 3, 4));
        }
    }

    private void filterMedicalTable(String query) {
        @SuppressWarnings("unchecked")
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) medicalRecordTable.getRowSorter();
        if (sorter == null) {
            sorter = new TableRowSorter<>(medicalRecordTable.getModel());
            medicalRecordTable.setRowSorter(sorter);
        }
        if (query == null || query.trim().isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + query, 1, 2, 3));
        }
    }

    private void filterBillingTable(String query) {
        @SuppressWarnings("unchecked")
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) billingTable.getRowSorter();
        if (sorter == null) {
            sorter = new TableRowSorter<>(billingTable.getModel());
            billingTable.setRowSorter(sorter);
        }
        if (query == null || query.trim().isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + query, 1, 3, 4));
        }
    }

    private void filterLabTable(String query) {
        @SuppressWarnings("unchecked")
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) labTable.getRowSorter();
        if (sorter == null) {
            sorter = new TableRowSorter<>(labTable.getModel());
            labTable.setRowSorter(sorter);
        }
        if (query == null || query.trim().isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + query, 1, 2, 3));
        }
    }

    private void filterAdmissionTable(String query) {
        @SuppressWarnings("unchecked")
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) admissionTable.getRowSorter();
        if (sorter == null) {
            sorter = new TableRowSorter<>(admissionTable.getModel());
            admissionTable.setRowSorter(sorter);
        }
        if (query == null || query.trim().isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + query, 1, 3));
        }
    }

    // Admission handlers (simple UI-only implementations)
    private void openAdmitPatientDialog() {
        // Attempt to prefill from selected patient in patientRegTable
        String prefPatient = "";
        String prefId = null;
        try {
            int sel = patientRegTable.getSelectedRow();
            if (sel != -1) {
                DefaultTableModel pm = (DefaultTableModel) patientRegTable.getModel();
                prefPatient = String.valueOf(pm.getValueAt(sel, 1));
                Object idv = pm.getValueAt(sel, 0);
                prefId = idv == null ? null : String.valueOf(idv);
            }
        } catch (Throwable ignored) {}

        JPanel panel = new JPanel(new GridLayout(4,2,8,8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JTextField patientField = new JTextField(prefPatient);
        JTextField idField = new JTextField(prefId==null?"":prefId);
        JTextField wardField = new JTextField();
        JTextArea notes = new JTextArea(3,20);
        panel.add(new JLabel("Patient Name (optional):")); panel.add(patientField);
        panel.add(new JLabel("Patient ID (optional):")); panel.add(idField);
        panel.add(new JLabel("Ward / Unit:")); panel.add(wardField);
        panel.add(new JLabel("Notes / Orders:")); panel.add(new JScrollPane(notes));

        int res = showDialog(panel, "Admit Patient"); if (res != JOptionPane.OK_OPTION) return;
        String pid = idField.getText().trim(); String pname = patientField.getText().trim(); String ward = wardField.getText().trim();
        if (pid.isEmpty() && pname.isEmpty()) { warn("Enter patient ID or name to admit"); return; }
        if (ward.isEmpty()) { warn("Ward/Unit is required"); return; }
        DefaultTableModel am = (DefaultTableModel) admissionTable.getModel();
        int nextId = 801 + am.getRowCount();
        String patientDisplay = pname.isEmpty() ? (pid.isEmpty() ? "(Unknown)" : pid) : (pname + (pid.isEmpty()?"":" ("+pid+")"));
        am.addRow(new Object[]{ nextId, patientDisplay, ward, "Admitted" });
        // update patient registry status if we have an id
        if (!pid.isEmpty()) {
            try { DefaultTableModel pm = (DefaultTableModel) patientRegTable.getModel(); for (int r=0;r<pm.getRowCount();r++){ Object idv = pm.getValueAt(r,0); if (idv!=null && String.valueOf(idv).equals(pid)){ pm.setValueAt("Admitted", r, 4); break; } } } catch (Throwable ignored) {}
        }
        info("Patient admitted: " + patientDisplay + " -> " + ward);
    }

    private void openDischargePatientDialog() {
        int r = admissionTable.getSelectedRow(); if (r == -1) { warn("Select an admission first"); return; }
        DefaultTableModel am = (DefaultTableModel) admissionTable.getModel();
        Object patientCell = am.getValueAt(r, 1);
        String patientDisplay = String.valueOf(patientCell==null?"(Unknown)":patientCell.toString());

        JPanel panel = new JPanel(new GridLayout(4,2,8,8)); panel.setBorder(new EmptyBorder(10,10,10,10));
        JTextField dt = new JTextField(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        JComboBox<String> dtype = new JComboBox<>(new String[]{"Routine","AMA","Transfer-out","Death"});
        JTextArea summary = new JTextArea(4,20);
        panel.add(new JLabel("Discharge Date/Time:")); panel.add(dt);
        panel.add(new JLabel("Discharge Type:")); panel.add(dtype);
        panel.add(new JLabel("Discharge Summary:")); panel.add(new JScrollPane(summary));
        panel.add(new JLabel("") ); panel.add(new JLabel("") );

        int res = showDialog(panel, "Discharge Patient"); if (res != JOptionPane.OK_OPTION) return;
        String dts = dt.getText().trim(); String dtyp = String.valueOf(dtype.getSelectedItem()); String summ = summary.getText().trim();
        // basic validation
        if (dts.isEmpty()) { warn("Discharge date/time is required"); return; }
        am.setValueAt("Discharged", r, 3);
        // update patient registry status if possible
        try { String pid = null; String s = String.valueOf(patientCell==null?"":patientCell.toString()); int p = s.lastIndexOf('('); int q = s.lastIndexOf(')'); if (p!=-1 && q>p) pid = s.substring(p+1,q); if (pid!=null) { DefaultTableModel pm = (DefaultTableModel) patientRegTable.getModel(); for (int i=0;i<pm.getRowCount();i++){ Object idv = pm.getValueAt(i,0); if (idv!=null && pid.equals(String.valueOf(idv))){ pm.setValueAt("Discharged", i, 4); break; } } } } catch (Throwable ignored) {}
        // optionally add discharge summary to medical records
        if (!summ.isBlank()) {
            try { DefaultTableModel mr = (DefaultTableModel) medicalRecordTable.getModel(); mr.addRow(new Object[]{ mr.getRowCount()+1001, patientDisplay, "Discharge Summary", summ }); } catch (Throwable ignored) {}
        }
        info("Patient discharged: " + patientDisplay + " — " + dtyp);
    }

    private void openTransferPatientDialog() {
        int r = admissionTable.getSelectedRow(); if (r == -1) { warn("Select an admission first"); return; }
        DefaultTableModel am = (DefaultTableModel) admissionTable.getModel();
        Object patientCell = am.getValueAt(r, 1); String patientDisplay = String.valueOf(patientCell==null?"(Unknown)":patientCell.toString());
        String originWard = String.valueOf(am.getValueAt(r, 2));

        JPanel panel = new JPanel(new GridLayout(4,2,8,8)); panel.setBorder(new EmptyBorder(10,10,10,10));
        JTextField destWard = new JTextField(); JTextField dt = new JTextField(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        JTextArea reason = new JTextArea(3,20);
        panel.add(new JLabel("Transfer Date/Time:")); panel.add(dt);
        panel.add(new JLabel("Destination Ward/Unit:")); panel.add(destWard);
        panel.add(new JLabel("Reason:")); panel.add(new JScrollPane(reason));
        panel.add(new JLabel("")); panel.add(new JLabel(""));

        int res = showDialog(panel, "Transfer Patient"); if (res != JOptionPane.OK_OPTION) return;
        String dw = destWard.getText().trim(); String rsn = reason.getText().trim(); if (dw.isEmpty()) { warn("Destination ward is required"); return; }
        am.setValueAt(dw, r, 2); am.setValueAt("Transferred", r, 3);
        // add transfer note to medical records
        try { DefaultTableModel mr = (DefaultTableModel) medicalRecordTable.getModel(); String note = "Transfer from " + originWard + " to " + dw + "\nReason: " + rsn; mr.addRow(new Object[]{ mr.getRowCount()+2001, patientDisplay, "Transfer Note", note }); } catch (Throwable ignored) {}
        info("Patient transferred: " + patientDisplay + " -> " + dw);
    }
}
