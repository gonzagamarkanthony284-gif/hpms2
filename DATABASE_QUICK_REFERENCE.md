# HPMS Database Quick Reference

## Database Connection

```java
// Get a database connection
java.sql.Connection conn = Util.DB.getConnection();

// Test connection
boolean isConnected = Util.DB.smokeTest();

// Initialize database schema
Util.DB.initDatabase();
```

## Using Repositories

### User Repository
```java
Repository.UserRepository userRepo = Repository.UserRepository.getInstance();

// Create
Model.User user = new Model.User("john.doe", "john@example.com", "John Doe",
    Util.PasswordHasher.hash("password".toCharArray()), Model.Role.PATIENT);
Model.User saved = userRepo.save(user);

// Read
Optional<Model.User> user = userRepo.findById(userId);
Optional<Model.User> user = userRepo.findByUsername("john.doe");
List<Model.User> doctors = userRepo.findByRole(Model.Role.DOCTOR);

// Update
user.setEmail("newemail@example.com");
userRepo.save(user);

// Delete
userRepo.delete(userId);
```

### Patient Repository
```java
Repository.PatientRepository patientRepo = Repository.PatientRepository.getInstance();

// Create (requires User)
Model.User user = /* ... created user ... */;
java.time.LocalDate dob = java.time.LocalDate.of(1990, 5, 15);
Model.Patient patient = new Model.Patient(user, "PT-001", dob, "M",
    "O+", "Single", "123 Main St", "555-1234",
    "John Doe", "555-0000");
Model.Patient saved = patientRepo.save(patient);

// Read
Optional<Model.Patient> patient = patientRepo.findById(patientId);
Optional<Model.Patient> patient = patientRepo.findByPatientNumber("PT-001");

// Search
List<Model.Patient> patients = patientRepo.findByLastName("Doe");
```

### Doctor Repository
```java
Repository.DoctorRepository doctorRepo = Repository.DoctorRepository.getInstance();

// Create
Model.User docUser = /* ... doctor user ... */;
Model.Doctor doctor = new Model.Doctor(docUser, "Cardiology", "LIC-12345",
    java.time.LocalDate.of(2025, 12, 31), 10,
    "DEPT-001", java.math.BigDecimal.valueOf(500.00),
    "Senior cardiologist", "555-2222", Model.UserStatus.ACTIVE);
Model.Doctor saved = doctorRepo.save(doctor);

// Read
Optional<Model.Doctor> doctor = doctorRepo.findById(doctorId);

// Search
List<Model.Doctor> cardiologists = doctorRepo.findBySpecialization("Cardiology");
List<Model.Doctor> active = doctorRepo.findActive();
```

### Appointment Repository
```java
Repository.AppointmentRepository apptRepo = Repository.AppointmentRepository.getInstance();

// Create
java.time.LocalDate date = java.time.LocalDate.now().plusDays(7);
java.time.LocalTime time = java.time.LocalTime.of(14, 0); // 2:00 PM
Model.Appointment appt = new Model.Appointment(patientId, doctorId, date, time,
    "Regular checkup");
Model.Appointment saved = apptRepo.save(appt);

// Read
Optional<Model.Appointment> appt = apptRepo.findById(appointmentId);

// Search
List<Model.Appointment> dateAppts = apptRepo.findByPatientAndDate(patientId, date);
List<Model.Appointment> pending = apptRepo.findByStatus(Model.AppointmentStatus.PENDING);
```

### Billing Repository
```java
Repository.BillingRepository billRepo = Repository.BillingRepository.getInstance();

// Create
Model.Billing bill = new Model.Billing(patientId,
    java.math.BigDecimal.valueOf(1500.00),
    "Consultation and lab tests");
Model.Billing saved = billRepo.save(bill);

// Read
Optional<Model.Billing> bill = billRepo.findById(billingId);

// Search
List<Model.Billing> patientBills = billRepo.findByPatient(patientId);
List<Model.Billing> pending = billRepo.findByStatus(Model.BillingStatus.PENDING);
```

### Other Repositories
Similar patterns apply to:
- `StaffRepository.getInstance()`
- `AdmissionRepository.getInstance()`
- `DepartmentRepository.getInstance()`

## Common Tasks

### Check Database Status
```java
if (Util.DB.smokeTest()) {
    System.out.println("Database is online and functional");
} else {
    System.err.println("Database connection failed");
}
```

### List All Active Users
```java
Repository.UserRepository repo = Repository.UserRepository.getInstance();
List<Model.User> active = repo.findActive();
for (Model.User user : active) {
    System.out.println(user.getUsername() + " (" + user.getRole() + ")");
}
```

### Find Patient and Show Medical Records
```java
Repository.PatientRepository patientRepo = Repository.PatientRepository.getInstance();
Optional<Model.Patient> patient = patientRepo.findById(patientId);
if (patient.isPresent()) {
    Model.Patient p = patient.get();
    System.out.println("Patient: " + p.getFirstName() + " " + p.getLastName());
    System.out.println("DOB: " + p.getDateOfBirth());
    System.out.println("Phone: " + p.getContactNumber());
}
```

### Schedule an Appointment
```java
Repository.AppointmentRepository apptRepo = Repository.AppointmentRepository.getInstance();
Repository.PatientRepository patientRepo = Repository.PatientRepository.getInstance();
Repository.DoctorRepository doctorRepo = Repository.DoctorRepository.getInstance();

Optional<Model.Patient> patient = patientRepo.findById(patientId);
Optional<Model.Doctor> doctor = doctorRepo.findById(doctorId);

if (patient.isPresent() && doctor.isPresent()) {
    java.time.LocalDate date = java.time.LocalDate.now().plusDays(7);
    java.time.LocalTime time = java.time.LocalTime.of(14, 0);
    Model.Appointment appt = new Model.Appointment(
        patient.get().getId(),
        doctor.get().getDoctorId(),
        date, time, "Regular checkup"
    );
    Model.Appointment saved = apptRepo.save(appt);
    System.out.println("Appointment created: " + saved.getId());
}
```

### Update Appointment Status
```java
Repository.AppointmentRepository apptRepo = Repository.AppointmentRepository.getInstance();
Optional<Model.Appointment> appt = apptRepo.findById(appointmentId);
if (appt.isPresent()) {
    appt.get().setStatus(Model.AppointmentStatus.COMPLETED);
    apptRepo.save(appt.get());
}
```

### Create Billing Record
```java
Repository.BillingRepository billRepo = Repository.BillingRepository.getInstance();
Model.Billing bill = new Model.Billing(
    patientId,
    java.math.BigDecimal.valueOf(2500.00),
    "Hospital stay (5 days) + surgery + lab tests"
);
Model.Billing saved = billRepo.save(bill);
System.out.println("Bill created: " + saved.getId() + " for $" + saved.getAmount());
```

## Error Handling

### Handle Optional Results
```java
Optional<Model.User> user = userRepo.findById(userId);
if (user.isPresent()) {
    System.out.println("User found: " + user.get().getUsername());
} else {
    System.out.println("User not found");
}

// Or using lambda
user.ifPresent(u -> System.out.println("User: " + u.getUsername()));
user.ifPresentOrElse(
    u -> System.out.println("User: " + u.getUsername()),
    () -> System.out.println("User not found")
);
```

### Handle Exceptions
```java
try {
    Model.User user = new Model.User("john", "john@example.com", "John",
        Util.PasswordHasher.hash("pwd".toCharArray()), Model.Role.PATIENT);
    userRepo.save(user);
    System.out.println("User created successfully");
} catch (Exception ex) {
    System.err.println("Failed to create user: " + ex.getMessage());
}
```

## SQL Direct Access

If you need to run raw SQL:
```java
try (java.sql.Connection conn = Util.DB.getConnection();
     java.sql.Statement stmt = conn.createStatement();
     java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM patients")) {
    
    if (rs.next()) {
        int count = rs.getInt("cnt");
        System.out.println("Total patients: " + count);
    }
} catch (java.sql.SQLException ex) {
    System.err.println("Query failed: " + ex.getMessage());
}
```

## Configuration

Edit `config/db.properties`:
```properties
# MySQL server
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/hpms?serverTimezone=UTC&useSSL=false
db.user=hpmsuser
db.password=StrongPasswordHere

# Auto-initialize schema on startup
db.init=true
```

## Debugging

### View Database Initialization Output
```
[DatabaseInitializer] Starting database initialization...
[DatabaseInitializer] Testing database connection...
[DatabaseInitializer] ✓ Database connection successful
[DatabaseInitializer] Initializing database schema...
[DatabaseInitializer] Running smoke tests...
[DatabaseInitializer] ✓ User CRUD test PASSED
[DatabaseInitializer] ✓ Patient CRUD test PASSED
...
```

### Check Console Errors
Look for messages starting with:
- `[DatabaseRepository]` - Repository-specific errors
- `[DB]` - Database utility errors
- `[DatabaseInitializer]` - Initialization errors

### Query the Database Directly
```bash
mysql -u hpmsuser -p hpms
select count(*) from users;
select count(*) from patients;
select count(*) from appointments where status = 'PENDING';
```

## Performance Tips

### Use Indexes
- Most common queries have indexes
- Filter by indexed columns for best performance
- Use date range queries on scheduled_date (indexed)

### Batch Operations
```java
List<Model.Patient> patients = patientRepo.findAll();
for (Model.Patient p : patients) {
    // Process patient
}
```

### Connection Pooling
- Current implementation uses single connection management
- Consider HikariCP for production applications
- Connection reuse improves performance

## Resources

- [Full Database Setup Guide](DATABASE_SETUP.md)
- [Implementation Summary](IMPLEMENTATION_SUMMARY.md)
- [Verification Checklist](VERIFICATION_CHECKLIST.md)
- [README](README.md)

## Common Repository Methods

All repositories support:
- `save(T entity)` - Create or update
- `findById(ID id)` - Get by primary key
- `findAll()` - Get all records
- `delete(ID id)` - Delete by ID

Most also support custom methods:
- `findByUsername` / `findByEmail` (Users)
- `findByPatientNumber` / `findByLastName` (Patients)
- `findBySpecialization` (Doctors)
- `findByStatus` (Various)
- `findActive` (Users, Doctors, Staff)
