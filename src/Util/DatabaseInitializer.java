package Util;

import java.io.*;
import java.sql.*;
import java.util.*;

/**
 * Database initialization and verification utility for HPMS.
 * Handles:
 * 1. Database and user creation (MySQL)
 * 2. Schema initialization
 * 3. Connection testing
 * 4. Smoke tests for all major CRUD operations
 */
public class DatabaseInitializer {

    private static final String CONFIG_PATH = "config/db.properties";
    private static final String SCHEMA_PATH = "sql/schema.sql";
    private static final String MYSQL_INIT_PATH = "sql/mysql_init.sql";

    /**
     * Initialize the database - called on application startup.
     * Steps:
     * 1. Load configuration
     * 2. Test/create database connection
     * 3. Execute schema initialization
     * 4. Run smoke tests
     */
    public static void initialize() {
        System.out.println("[DatabaseInitializer] Starting database initialization...");

        // Step 1: Test basic connection
        if (!testConnection()) {
            System.err.println("[DatabaseInitializer] CRITICAL: Cannot connect to database!");
            System.err.println("[DatabaseInitializer] Please ensure MySQL is running and configured correctly.");
            System.err.println("[DatabaseInitializer] See: " + CONFIG_PATH);
            return;
        }

        // Step 2: Initialize schema
        System.out.println("[DatabaseInitializer] Initializing database schema...");
        DB.initDatabase();

        // Step 2b: Ensure required seed data exists (e.g., departments)
        ensureDepartmentsSeeded();

        // Step 2c: Purge any leftover demo data to avoid duplicate key errors
        purgeDemoData();

        // Step 3: Run smoke tests
        System.out.println("[DatabaseInitializer] Running smoke tests...");
        runSmokeTests();

        System.out.println("[DatabaseInitializer] Database initialization complete!");
    }

    /**
     * Generate a unique suffix based on time to avoid duplicate keys in smoke
     * tests.
     */
    private static String uniq(String base) {
        long t = System.currentTimeMillis();
        return base + "_" + t;
    }

    /**
     * Ensure core departments exist for FK references used by demo data/tests.
     */
    private static void ensureDepartmentsSeeded() {
        System.out.println("[DatabaseInitializer] Ensuring default departments exist...");
        Repository.DepartmentRepository deptRepo = Repository.DepartmentRepository.getInstance();
        try {
            // Seed a minimal set of departments required by demo inserts
            java.util.List<Model.Department> defaults = java.util.Arrays.asList(
                    new Model.Department("DEPT-001", "Emergency", "Emergency Department"),
                    new Model.Department("DEPT-002", "Orthopedics", "Orthopedic Department"),
                    new Model.Department("DEPT-003", "Pediatrics", "Pediatric Department"));

            for (Model.Department d : defaults) {
                java.util.Optional<Model.Department> existing = deptRepo.findById(d.getId());
                if (existing.isEmpty()) {
                    System.out.println("[DatabaseInitializer] Seeding department: " + d.getId() + " - " + d.getName());
                    deptRepo.save(d);
                }
            }
        } catch (Exception ex) {
            System.err.println("[DatabaseInitializer] Error seeding departments: " + ex.getMessage());
        }
    }

    /**
     * Remove leftover demo records to make tests/idempotent seeding clean.
     */
    private static void purgeDemoData() {
        System.out.println("[DatabaseInitializer] Purging leftover demo data (if any)...");
        Repository.UserRepository userRepo = Repository.UserRepository.getInstance();
        Repository.PatientRepository patientRepo = Repository.PatientRepository.getInstance();
        Repository.DoctorRepository doctorRepo = Repository.DoctorRepository.getInstance();
        Repository.StaffRepository staffRepo = Repository.StaffRepository.getInstance();
        Repository.AppointmentRepository appointmentRepo = Repository.AppointmentRepository.getInstance();

        // Helper to delete a user and any dependent rows
        java.util.function.Consumer<String> deleteUserCascade = username -> {
            try {
                java.util.Optional<Model.User> u = userRepo.findByUsername(username);
                if (u.isPresent()) {
                    String userId = u.get().getId();
                    // Delete patient linked to user first (to satisfy FK)
                    java.util.Optional<Model.Patient> pt = patientRepo.findByUserId(userId);
                    pt.ifPresent(p -> patientRepo.delete(p.getId()));
                    // Delete doctor linked to user
                    java.util.Optional<Model.Doctor> doc = doctorRepo.findByUserId(userId);
                    doc.ifPresent(d -> {
                        // Appointments that reference this doctor will be deleted via FK constraints or
                        // handled separately
                        doctorRepo.delete(d.getDoctorId());
                    });
                    // Delete staff linked to user
                    java.util.Optional<Model.Staff> st = staffRepo.findByUserId(userId);
                    st.ifPresent(s -> staffRepo.delete(s.getStaffId()));
                    // Try to delete user directly (patients may reference users)
                    userRepo.delete(userId);
                }
            } catch (Exception ex) {
                System.err.println("[DatabaseInitializer] purge user '" + username + "' error: " + ex.getMessage());
            }
        };

        // Delete known demo users
        deleteUserCascade.accept("testuser");
        deleteUserCascade.accept("testpatient");
        deleteUserCascade.accept("testdoctor");
        deleteUserCascade.accept("teststaff");
        deleteUserCascade.accept("appointpatient");
        deleteUserCascade.accept("appointdoctor");
        deleteUserCascade.accept("billpatient");

        // Delete known demo patients by patient number
        try {
            java.util.Optional<Model.Patient> p1 = patientRepo.findByPatientNumber("PT-TEST-001");
            p1.ifPresent(p -> patientRepo.delete(p.getId()));
            java.util.Optional<Model.Patient> p2 = patientRepo.findByPatientNumber("PT-APP-001");
            p2.ifPresent(p -> patientRepo.delete(p.getId()));
            java.util.Optional<Model.Patient> p3 = patientRepo.findByPatientNumber("PT-BILL-001");
            p3.ifPresent(p -> patientRepo.delete(p.getId()));
        } catch (Exception ex) {
            System.err.println("[DatabaseInitializer] purge patient numbers error: " + ex.getMessage());
        }
    }

    /**
     * Test database connection.
     */
    public static boolean testConnection() {
        System.out.println("[DatabaseInitializer] Testing database connection...");
        try {
            if (DB.smokeTest()) {
                System.out.println("[DatabaseInitializer] ✓ Database connection successful");
                return true;
            } else {
                System.err.println("[DatabaseInitializer] ✗ Database connection test failed");
                return false;
            }
        } catch (Exception ex) {
            System.err.println("[DatabaseInitializer] ✗ Database connection error: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Run comprehensive smoke tests to verify all CRUD operations.
     */
    public static void runSmokeTests() {
        boolean allPassed = true;

        // Test User CRUD
        if (!testUserCRUD()) {
            System.err.println("[DatabaseInitializer] ✗ User CRUD test FAILED");
            allPassed = false;
        } else {
            System.out.println("[DatabaseInitializer] ✓ User CRUD test PASSED");
        }

        // Test Patient CRUD
        if (!testPatientCRUD()) {
            System.err.println("[DatabaseInitializer] ✗ Patient CRUD test FAILED");
            allPassed = false;
        } else {
            System.out.println("[DatabaseInitializer] ✓ Patient CRUD test PASSED");
        }

        // Test Doctor CRUD
        if (!testDoctorCRUD()) {
            System.err.println("[DatabaseInitializer] ✗ Doctor CRUD test FAILED");
            allPassed = false;
        } else {
            System.out.println("[DatabaseInitializer] ✓ Doctor CRUD test PASSED");
        }

        // Test Staff CRUD
        if (!testStaffCRUD()) {
            System.err.println("[DatabaseInitializer] ✗ Staff CRUD test FAILED");
            allPassed = false;
        } else {
            System.out.println("[DatabaseInitializer] ✓ Staff CRUD test PASSED");
        }

        // Test Appointment CRUD
        if (!testAppointmentCRUD()) {
            System.err.println("[DatabaseInitializer] ✗ Appointment CRUD test FAILED");
            allPassed = false;
        } else {
            System.out.println("[DatabaseInitializer] ✓ Appointment CRUD test PASSED");
        }

        // Test Billing CRUD
        if (!testBillingCRUD()) {
            System.err.println("[DatabaseInitializer] ✗ Billing CRUD test FAILED");
            allPassed = false;
        } else {
            System.out.println("[DatabaseInitializer] ✓ Billing CRUD test PASSED");
        }

        if (allPassed) {
            System.out.println("[DatabaseInitializer] ✓ All smoke tests PASSED!");
        } else {
            System.err.println("[DatabaseInitializer] ✗ Some smoke tests FAILED - check errors above");
        }
    }

    /**
     * Test User CRUD operations.
     */
    private static boolean testUserCRUD() {
        try {
            Repository.UserRepository repo = Repository.UserRepository.getInstance();

            // CREATE
            String uname = uniq("testuser");
            Model.User user = new Model.User(uname, uname + "@example.com", "Test User",
                    Util.PasswordHasher.hash("TestPassword123".toCharArray()), Model.Role.PATIENT);
            Model.User saved = repo.save(user);
            if (saved == null || saved.getId() == null)
                return false;

            // READ
            java.util.Optional<Model.User> found = repo.findById(saved.getId());
            if (found.isEmpty())
                return false;

            // UPDATE
            found.get().setEmail("updated@example.com");
            repo.save(found.get());

            // VERIFY UPDATE
            java.util.Optional<Model.User> updated = repo.findById(saved.getId());
            if (updated.isEmpty() || !updated.get().getEmail().equals("updated@example.com"))
                return false;

            // DELETE
            if (!repo.delete(saved.getId()))
                return false;

            // VERIFY DELETE
            java.util.Optional<Model.User> deleted = repo.findById(saved.getId());
            return deleted.isEmpty();
        } catch (Exception ex) {
            System.err.println("[DatabaseInitializer] User CRUD error: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Test Patient CRUD operations.
     */
    private static boolean testPatientCRUD() {
        try {
            Repository.UserRepository userRepo = Repository.UserRepository.getInstance();
            Repository.PatientRepository patientRepo = Repository.PatientRepository.getInstance();

            // Create associated user first
            String uname = uniq("testpatient");
            Model.User user = new Model.User(uname, uname + "@example.com", "Test Patient",
                    Util.PasswordHasher.hash("TestPassword123".toCharArray()), Model.Role.PATIENT);
            Model.User savedUser = userRepo.save(user);

            // CREATE
            java.time.LocalDate dob = java.time.LocalDate.of(1990, 1, 1);
            String pnum = uniq("PT-TEST");
            Model.Patient patient = new Model.Patient(savedUser, pnum, dob, "M",
                    "O+", "Single", "123 Test St", "555-1234",
                    "John Doe", "555-0000");
            Model.Patient saved = patientRepo.save(patient);
            if (saved == null || saved.getId() == null)
                return false;

            // READ
            java.util.Optional<Model.Patient> found = patientRepo.findById(saved.getId());
            if (found.isEmpty())
                return false;

            // DELETE
            if (!patientRepo.delete(saved.getId()))
                return false;

            // Clean up user
            userRepo.delete(savedUser.getId());

            return true;
        } catch (Exception ex) {
            System.err.println("[DatabaseInitializer] Patient CRUD error: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Test Doctor CRUD operations.
     */
    private static boolean testDoctorCRUD() {
        try {
            Repository.UserRepository userRepo = Repository.UserRepository.getInstance();
            Repository.DoctorRepository doctorRepo = Repository.DoctorRepository.getInstance();

            // Create associated user
            String uname = uniq("testdoctor");
            Model.User user = new Model.User(uname, uname + "@example.com", "Test Doctor",
                    Util.PasswordHasher.hash("TestPassword123".toCharArray()), Model.Role.DOCTOR);
            Model.User savedUser = userRepo.save(user);

            // CREATE
            Model.Doctor doctor = new Model.Doctor(savedUser, "Cardiology", "LIC-123456",
                    java.time.LocalDate.of(2025, 12, 31), 10,
                    "DEPT-001", java.math.BigDecimal.valueOf(500),
                    "Senior cardiologist", "555-2222", Model.UserStatus.ACTIVE);
            Model.Doctor saved = doctorRepo.save(doctor);
            if (saved == null || saved.getDoctorId() == null)
                return false;

            // READ
            java.util.Optional<Model.Doctor> found = doctorRepo.findById(saved.getDoctorId());
            if (found.isEmpty())
                return false;

            // DELETE
            if (!doctorRepo.delete(saved.getDoctorId()))
                return false;

            // Clean up user
            userRepo.delete(savedUser.getId());

            return true;
        } catch (Exception ex) {
            System.err.println("[DatabaseInitializer] Doctor CRUD error: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Test Staff CRUD operations.
     */
    private static boolean testStaffCRUD() {
        try {
            Repository.UserRepository userRepo = Repository.UserRepository.getInstance();
            Repository.StaffRepository staffRepo = Repository.StaffRepository.getInstance();

            // Create associated user
            String uname = uniq("teststaff");
            Model.User user = new Model.User(uname, uname + "@example.com", "Test Staff",
                    Util.PasswordHasher.hash("TestPassword123".toCharArray()), Model.Role.STAFF);
            Model.User savedUser = userRepo.save(user);

            // CREATE
            Model.Staff staff = new Model.Staff(savedUser, "Jane", "Doe", "Nurse", "DEPT-001",
                    "555-3333", java.time.LocalDate.of(2020, 1, 1), Model.UserStatus.ACTIVE);
            Model.Staff saved = staffRepo.save(staff);
            if (saved == null || saved.getStaffId() == null)
                return false;

            // READ
            java.util.Optional<Model.Staff> found = staffRepo.findById(saved.getStaffId());
            if (found.isEmpty())
                return false;

            // DELETE
            if (!staffRepo.delete(saved.getStaffId()))
                return false;

            // Clean up user
            userRepo.delete(savedUser.getId());

            return true;
        } catch (Exception ex) {
            System.err.println("[DatabaseInitializer] Staff CRUD error: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Test Appointment CRUD operations.
     */
    private static boolean testAppointmentCRUD() {
        try {
            Repository.UserRepository userRepo = Repository.UserRepository.getInstance();
            Repository.PatientRepository patientRepo = Repository.PatientRepository.getInstance();
            Repository.DoctorRepository doctorRepo = Repository.DoctorRepository.getInstance();
            Repository.AppointmentRepository appointmentRepo = Repository.AppointmentRepository.getInstance();

            // Create test patient
            String pUser = uniq("appointpatient");
            Model.User patientUser = new Model.User(pUser, pUser + "@example.com",
                    "Appoint Patient", Util.PasswordHasher.hash("TestPassword123".toCharArray()),
                    Model.Role.PATIENT);
            Model.User savedPatientUser = userRepo.save(patientUser);
            java.time.LocalDate dob = java.time.LocalDate.of(1990, 1, 1);
            String pnum = uniq("PT-APP");
            Model.Patient patient = new Model.Patient(savedPatientUser, pnum, dob, "M",
                    "O+", "Single", "123 Test St", "555-1234",
                    "John Doe", "555-0000");
            Model.Patient savedPatient = patientRepo.save(patient);

            // Create test doctor
            String dUser = uniq("appointdoctor");
            Model.User doctorUser = new Model.User(dUser, dUser + "@example.com",
                    "Appoint Doctor", Util.PasswordHasher.hash("TestPassword123".toCharArray()),
                    Model.Role.DOCTOR);
            Model.User savedDoctorUser = userRepo.save(doctorUser);
            Model.Doctor doctor = new Model.Doctor(savedDoctorUser, "Cardiology", "LIC-123456",
                    java.time.LocalDate.of(2025, 12, 31), 10,
                    "DEPT-001", java.math.BigDecimal.valueOf(500),
                    "Senior cardiologist", "555-2222", Model.UserStatus.ACTIVE);
            Model.Doctor savedDoctor = doctorRepo.save(doctor);

            // CREATE appointment
            java.time.LocalDate appointDate = java.time.LocalDate.now().plusDays(7);
            java.time.LocalTime appointTime = java.time.LocalTime.of(14, 0);
            Model.Appointment appointment = new Model.Appointment(savedPatient.getId(), savedDoctor.getDoctorId(),
                    appointDate, appointTime, "Regular checkup");
            Model.Appointment saved = appointmentRepo.save(appointment);
            if (saved == null || saved.getId() == null)
                return false;

            // READ
            java.util.Optional<Model.Appointment> found = appointmentRepo.findById(saved.getId());
            if (found.isEmpty())
                return false;

            // DELETE
            if (!appointmentRepo.delete(saved.getId()))
                return false;

            // Clean up
            patientRepo.delete(savedPatient.getId());
            doctorRepo.delete(savedDoctor.getDoctorId());
            userRepo.delete(savedPatientUser.getId());
            userRepo.delete(savedDoctorUser.getId());

            return true;
        } catch (Exception ex) {
            System.err.println("[DatabaseInitializer] Appointment CRUD error: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Test Billing CRUD operations.
     */
    private static boolean testBillingCRUD() {
        try {
            Repository.UserRepository userRepo = Repository.UserRepository.getInstance();
            Repository.PatientRepository patientRepo = Repository.PatientRepository.getInstance();
            Repository.BillingRepository billingRepo = Repository.BillingRepository.getInstance();

            // Create test patient
            String pUser = uniq("billpatient");
            Model.User patientUser = new Model.User(pUser, pUser + "@example.com",
                    "Bill Patient", Util.PasswordHasher.hash("TestPassword123".toCharArray()),
                    Model.Role.PATIENT);
            Model.User savedPatientUser = userRepo.save(patientUser);
            java.time.LocalDate dob = java.time.LocalDate.of(1990, 1, 1);
            String pnum = uniq("PT-BILL");
            Model.Patient patient = new Model.Patient(savedPatientUser, pnum, dob, "M",
                    "O+", "Single", "123 Test St", "555-1234",
                    "John Doe", "555-0000");
            Model.Patient savedPatient = patientRepo.save(patient);

            // CREATE billing
            Model.Billing billing = new Model.Billing(savedPatient.getId(),
                    java.math.BigDecimal.valueOf(1500.00),
                    "Consultation and lab tests");
            Model.Billing saved = billingRepo.save(billing);
            if (saved == null || saved.getId() == null)
                return false;

            // READ
            java.util.Optional<Model.Billing> found = billingRepo.findById(saved.getId());
            if (found.isEmpty())
                return false;

            // DELETE
            if (!billingRepo.delete(saved.getId()))
                return false;

            // Clean up
            patientRepo.delete(savedPatient.getId());
            userRepo.delete(savedPatientUser.getId());

            return true;
        } catch (Exception ex) {
            System.err.println("[DatabaseInitializer] Billing CRUD error: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Print database configuration information.
     */
    public static void printConfig() {
        System.out.println("\n=== HPMS Database Configuration ===");
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_PATH)) {
            props.load(fis);
            System.out.println("Driver: " + props.getProperty("db.driver"));
            System.out.println("URL: " + props.getProperty("db.url"));
            System.out.println("User: " + props.getProperty("db.user"));
            System.out.println("Init on startup: " + props.getProperty("db.init"));
        } catch (IOException e) {
            System.out.println("Could not read configuration: " + e.getMessage());
        }
        System.out.println("===================================\n");
    }
}
