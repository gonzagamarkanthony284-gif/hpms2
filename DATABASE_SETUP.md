# HPMS Database Setup Guide

## Overview
This document describes the complete database architecture and setup process for the Hospital Patient Management System (HPMS).

## Database Configuration

### Requirements
- MySQL 5.7 or later (or MariaDB compatible version)
- XAMPP or standalone MySQL installation
- Java 11 or later (with JDBC support)

### Configuration Files
- **Primary Config**: `config/db.properties` - Database connection settings
- **Example Config**: `config/db.properties.example` - Template with instructions
- **Schema Script**: `sql/schema.sql` - Complete database schema and table definitions
- **Init Script**: `sql/mysql_init.sql` - MySQL database and user creation script

### Steps to Set Up Database

#### 1. Start MySQL Server
```bash
# On Windows with XAMPP
C:\xampp\mysql\bin\mysql.exe -u root

# Or use the XAMPP Control Panel to start MySQL service
```

#### 2. Create Database and User
Run the initialization script:
```bash
mysql -u root < sql/mysql_init.sql
```

Or manually execute:
```sql
CREATE DATABASE IF NOT EXISTS hpms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'hpmsuser'@'localhost' IDENTIFIED BY 'StrongPasswordHere';
GRANT ALL PRIVILEGES ON hpms.* TO 'hpmsuser'@'localhost';
FLUSH PRIVILEGES;
```

#### 3. Configure Connection
Edit `config/db.properties`:
```properties
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/hpms?serverTimezone=UTC&useSSL=false&allowMultiQueries=true
db.user=hpmsuser
db.password=StrongPasswordHere
db.init=true
db.init.on.startup=true
```

#### 4. Run Application
The application will automatically:
1. Load configuration from `config/db.properties`
2. Test the database connection
3. Execute the schema script to create tables
4. Run smoke tests to verify CRUD operations
5. Initialize demo users

## Database Architecture

### Entity Relationships

```
Users (Core Authentication)
├── Patients (linked via user_id)
│   ├── Medical Records
│   ├── Medical Profiles
│   ├── Insurance Policies
│   ├── Admissions
│   │   └── Beds (linked via bed_id)
│   ├── Appointments (with Doctors)
│   ├── Lab Orders
│   ├── Billings
│   └── Visits
├── Doctors (linked via user_id)
│   ├── Doctor Education
│   ├── Doctor Experience
│   ├── Doctor Schedules
│   └── Appointments
├── Staff (linked via user_id)
│   └── Staff Schedules
└── Admin (linked via user_id)

Organizational Structure
├── Departments
│   ├── Doctors (department_id)
│   ├── Staff (department_id)
│   └── Rooms (ward_id)
│       └── Beds (room_id)

Supporting Tables
├── Notifications
├── User Activity Log
├── Contact Info
└── Address Info
```

### Core Tables

#### users
Stores all user accounts with authentication credentials and roles.
- **Fields**: id, username, email, full_name, password_hash, role, status, created_at, updated_at
- **Indexes**: username, email, role, status

#### patients
Patient demographic and health information, linked to users.
- **Fields**: id, user_id, patient_number, first_name, last_name, date_of_birth, sex, blood_type, contact_number, insurance_info, etc.
- **Relationships**: FK to users, linked to admissions, medical_records, appointments

#### doctors
Doctor profiles linked to users with specialization and credentials.
- **Fields**: id, user_id, specialization, license_number, years_of_experience, consultation_fee, status
- **Relationships**: FK to users, department, schedule

#### staff
Hospital staff profiles including nurses, clerks, receptionists.
- **Fields**: id, user_id, first_name, last_name, role_type, department_id, hire_date, status
- **Relationships**: FK to users, department, schedule

#### appointments
Appointment scheduling between patients and doctors.
- **Fields**: id, patient_id, doctor_id, schedule_date, schedule_time, reason, status
- **Status Values**: PENDING, CONFIRMED, COMPLETED, CANCELLED
- **Unique Constraint**: (patient_id, doctor_id, schedule_date, schedule_time)

#### admissions
Patient hospital admissions and bed assignments.
- **Fields**: id, patient_id, admitted_at, admitted_by, room_id, bed_id, status, discharged_at
- **Status Values**: ACTIVE, ADMITTED, TRANSFERRED, DISCHARGED, CANCELLED
- **Relationships**: FK to patients, users, rooms, beds

#### medical_records
Detailed visit notes and consultations.
- **Fields**: id, patient_id, doctor_id, visit_date, chief_complaint, diagnosis, treatment_plan, notes
- **Relationships**: FK to patients, doctors

#### billings
Patient charges and invoices.
- **Fields**: id, patient_id, amount, description, status, service_date, due_date, paid_date
- **Status Values**: PENDING, PAID, CANCELLED
- **Relationships**: FK to patients

#### lab_orders & lab_results
Laboratory test orders and results.
- **Order Fields**: id, patient_id, doctor_id, test_type, status, created_at
- **Result Fields**: id, lab_order_id, patient_id, result_value, abnormal_flag, interpretation
- **Relationships**: lab_results FK to lab_orders

#### notifications
User system notifications and alerts.
- **Fields**: id, recipient_user_id, message, seen, seen_at, created_at
- **Relationships**: FK to users

### Key Indexes for Performance
- `idx_appointments_date_status` - Fast appointment lookups by date and status
- `idx_medical_records_patient_date` - Medical history queries
- `idx_admissions_patient_status` - Active admission tracking
- `idx_lab_orders_patient_status` - Test order management
- `idx_notifications_recipient_seen` - User notification queries

## Repository Layer

### Database-Backed Repositories
The system includes the following database repositories:

- **UserRepository** - User account management
- **PatientRepository** - Patient records
- **DoctorRepository** - Doctor profiles
- **StaffRepository** - Staff personnel
- **AppointmentRepository** - Appointment scheduling
- **BillingRepository** - Financial records
- **AdmissionRepository** - Hospital admissions
- **DepartmentRepository** - Organizational units

### Repository Interface
All repositories implement the `Repository<ID, T>` interface:
```java
public interface Repository<ID, T> {
    T save(T entity);           // Create or update
    Optional<T> findById(ID id); // Read by ID
    Collection<T> findAll();     // Read all
    boolean delete(ID id);       // Delete
}
```

### Custom Query Methods
Repositories include specialized query methods:
- **UserRepository**: findByUsername, findByEmail, findByRole, findActive
- **PatientRepository**: findByPatientNumber, findByLastName, findCreatedAfter
- **DoctorRepository**: findByUserId, findBySpecialization, findActive
- **AppointmentRepository**: findByPatientAndDate, findByDoctorAndDate, findByStatus
- **BillingRepository**: findByPatient, findByStatus

## Database Initialization

### Automatic Initialization
When the application starts, `DatabaseInitializer` automatically:

1. **Prints Configuration** - Displays current database settings
2. **Tests Connection** - Verifies MySQL is accessible
3. **Creates Schema** - Executes `sql/schema.sql` to create all tables
4. **Runs Smoke Tests** - Tests all CRUD operations:
   - User CREATE, READ, UPDATE, DELETE
   - Patient CREATE, READ, DELETE
   - Doctor CREATE, READ, DELETE
   - Staff CREATE, READ, DELETE
   - Appointment CREATE, READ, DELETE
   - Billing CREATE, READ, DELETE

### Manual Schema Execution
If automatic initialization fails, manually execute the schema:
```bash
mysql -u hpmsuser -p hpms < sql/schema.sql
```

## Data Persistence Features

### CRUD Operations (All Entities)
- **CREATE**: `repository.save(entity)` - Insert new record
- **READ**: `repository.findById(id)` - Retrieve by primary key
- **READ ALL**: `repository.findAll()` - Retrieve all records
- **UPDATE**: `repository.save(entity)` - Update existing record
- **DELETE**: `repository.delete(id)` - Remove record

### Specialized Queries
Each repository provides domain-specific query methods for common searches:
- Find by username, email, role (Users)
- Find by patient number, last name (Patients)
- Find by specialization, department (Doctors)
- Find by date, status (Appointments)
- Find by patient, status (Billings)

### Transactional Integrity
- Primary keys ensure record uniqueness
- Foreign keys enforce referential integrity
- Unique constraints prevent duplicate appointments
- Timestamps track record creation and modification

## Security Considerations

### Password Security
- Passwords are hashed using PBKDF2 with 10,000 iterations
- Raw passwords are never stored in the database
- Password char arrays are cleared from memory after hashing

### Access Control
- User roles (PATIENT, DOCTOR, STAFF, ADMIN) control system access
- User status (ACTIVE, INACTIVE, DEACTIVATED) tracks account state
- Patient data is linked to user accounts for authorization

### Data Isolation
- Patient records are scoped to their user account
- Doctor and staff records link to user authentication
- Billing and medical records are patient-specific

## Backup and Recovery

### Regular Backups
```bash
# Backup the entire database
mysqldump -u hpmsuser -p hpms > backup_hpms_$(date +%Y%m%d_%H%M%S).sql

# Restore from backup
mysql -u hpmsuser -p hpms < backup_hpms_20231215_120000.sql
```

### Data Archival
The system supports soft-delete for patients:
- `PatientService.archivePatient(id)` - Soft delete
- `PatientService.unarchivePatient(id)` - Restore
- Archived patients are excluded from active queries

## Troubleshooting

### Connection Errors
If you see "Cannot connect to database":
1. Verify MySQL is running: `mysql -u root`
2. Check `config/db.properties` settings
3. Ensure database and user exist
4. Verify firewall allows localhost connections

### Schema Errors
If table creation fails:
1. Check MySQL user has CREATE TABLE permissions
2. Verify database character set is utf8mb4
3. Look for conflicting tables: `DROP TABLE IF EXISTS tablename;`
4. Review console output from DatabaseInitializer

### CRUD Operation Failures
If CRUD smoke tests fail:
1. Check database connection is working
2. Verify all tables were created successfully
3. Ensure foreign key constraints are satisfied
4. Review error messages in console output

### Performance Issues
If queries are slow:
1. Run `ANALYZE TABLE` on frequently queried tables
2. Verify indexes exist: `SHOW INDEX FROM tablename;`
3. Monitor MySQL with: `SHOW PROCESSLIST;`
4. Check disk space for database files

## Performance Optimization

### Query Optimization
- Use indexed columns for WHERE clauses
- Combine filters for multi-field searches
- Consider pagination for large result sets

### Index Usage
Key indexes are automatically created for:
- User lookups (username, email, role)
- Patient searches (last name, patient number)
- Appointment queries (date, status)
- Notification retrieval (recipient, read status)

### Batch Operations
For bulk inserts/updates:
```java
for (Entity e : entities) {
    repository.save(e);
}
```

## Monitoring and Logging

### Database Logging
The `DatabaseInitializer` logs all operations:
- Connection test results
- Schema execution progress
- CRUD test outcomes
- Error details for troubleshooting

### Error Handling
All database operations include try-catch blocks:
- SQL errors are caught and logged
- Operations fail gracefully with informative messages
- System continues operation when non-critical errors occur

## Maintenance

### Regular Maintenance
```bash
# Optimize database
mysql -u hpmsuser -p hpms -e "OPTIMIZE TABLE users, patients, doctors, appointments, billings;"

# Check database integrity
mysqlcheck -u hpmsuser -p hpms --all-databases
```

### Updating Database Schema
To add new columns to existing tables:
1. Execute ALTER TABLE statements
2. Update repository mapping code
3. Test thoroughly with smoke tests
4. Back up data before major changes

## Additional Resources

- [MySQL Documentation](https://dev.mysql.com/doc/)
- [JDBC Driver Documentation](https://dev.mysql.com/doc/connector-j/en/)
- [Database Design Best Practices](https://dev.mysql.com/doc/refman/8.0/en/optimization.html)
