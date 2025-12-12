# HPMS - Hospital Patient Management System

## Database Configuration (CRITICAL)

**The system now has full database integration with MySQL!**

See [DATABASE_SETUP.md](DATABASE_SETUP.md) for complete database setup and architecture documentation.

### Quick Setup
1. Start MySQL server (XAMPP or standalone)
2. Configure `config/db.properties` with your credentials
3. Run the application - automatic initialization will:
   - Create database schema with all tables, relationships, and indexes
   - Initialize tables from `sql/schema.sql`
   - Run comprehensive CRUD smoke tests to verify all operations
   - Report any issues to the console

### Database Components
- **Schema File**: [sql/schema.sql](sql/schema.sql) - 50+ tables with proper foreign keys and indexes
- **Init Script**: [sql/mysql_init.sql](sql/mysql_init.sql) - Database and user creation
- **Repositories**: Database-backed persistence for Users, Patients, Doctors, Staff, Appointments, Billings, Admissions, and more
- **Initializer**: DatabaseInitializer handles schema setup, connection testing, and CRUD verification

---

# HPMS - Auto Account Provisioning for Patients

As of Dec 2025, HPMS automatically creates a user account for every new patient created via `Service.PatientService#createPatient`.

What happens on patient creation:
- A unique username is generated based on the patient's first and last name (e.g., `jane.doe`, `jane.doe1` if taken).
- A temporary password is generated (pattern: `CapitalizedFirstName + YearOfBirth + 3-digit random`, e.g., `Jane1990661`), which meets the current password policy (>= 8 chars, letters and digits).
- A new user is created with role `PATIENT` and is linked to the patient via `linkedPatientId` on the `Model.User`.

How to fetch the credentials for display:
- Call `PatientService.getProvisionedAccountForPatient(patientId)` right after `createPatient(...)` to get an `Optional<ProvisionedAccount>` containing the generated `username` and `temporaryPassword`.
- Show this info to the registrar so it can be handed to the patient or sent via secure channel.

Notes:
- Credentials are stored transiently in-memory (for UI display/testing). The actual password is hashed in the `UserService` store.
- The in-memory services are singletons or simple instances; replace with DB-backed implementations when moving to production.

Quick demo run:
- Compile and run the included smoke test to see the message printed after patient creation.

```
cd HPMS/src
for /f %i in ('dir /s /b *.java') do @echo %i > sources.tmp
javac -d ..\bin @sources.tmp
cd ..\bin
java Service.ServicesSmokeTest
```

You should see output similar to:

```
Created patient id=<uuid>
Auto-provisioned account -> username=jane.doe, tempPassword=Jane1990XXX
...
```

Next steps (optional):
- Wire a real "Register New Patient" form to `PatientService#createPatient` and immediately display the credentials.
- Add email/SMS delivery for the temporary password.
- Persist users/patients in a database and enforce global username uniqueness.
- Add a first-login password change flow for patient accounts.
