package Repository;

import Model.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.*;

/**
 * Database-backed Patient repository.
 * Handles persistence of Patient entities to the patients table.
 */
public class PatientRepository extends DatabaseRepository<String, Patient> {
    
    private static final PatientRepository INSTANCE = new PatientRepository();
    
    public static PatientRepository getInstance() {
        return INSTANCE;
    }
    
    public PatientRepository() {
        super("patients");
    }
    
    @Override
    protected Patient mapResultSetToEntity(ResultSet rs) throws SQLException {
        String patientId = rs.getString("id");
        String userId = rs.getString("user_id");
        String patientNumber = rs.getString("patient_number");
        LocalDate dob = rs.getDate("date_of_birth") != null ? rs.getDate("date_of_birth").toLocalDate() : null;
        String sex = rs.getString("sex");
        String bloodType = rs.getString("blood_type");
        String civilStatus = rs.getString("civil_status");
        String address = rs.getString("address");
        String contactNumber = rs.getString("contact_number");
        String emergencyContactName = rs.getString("emergency_contact_name");
        String emergencyContactNumber = rs.getString("emergency_contact_number");
        
        // Get user if userId exists
        User user = null;
        if (userId != null) {
            user = UserRepository.getInstance().findById(userId).orElse(null);
        }
        
        Patient patient;
        if (user != null && dob != null) {
            patient = new Patient(user, patientNumber, dob, sex, bloodType, civilStatus, address, 
                                 contactNumber, emergencyContactName, emergencyContactNumber);
        } else {
            // Fallback for records without user
            String firstName = rs.getString("first_name");
            String lastName = rs.getString("last_name");
            String gender = rs.getString("gender");
            patient = new Patient(firstName, lastName, dob, gender, contactNumber, 
                                 userId != null ? userId : firstName, address);
        }
        
        // Set additional fields
        try {
            java.lang.reflect.Field idField = Patient.class.getDeclaredField("patientId");
            idField.setAccessible(true);
            idField.set(patient, patientId);
        } catch (Exception e) {
            System.err.println("[PatientRepository] Error mapping patient: " + e.getMessage());
        }
        
        return patient;
    }
    
    @Override
    protected String getEntityId(Patient entity) {
        return entity.getId();
    }
    
    @Override
    protected String getIdColumnName() {
        return "id";
    }
    
    @Override
    protected Patient insertEntity(Patient entity) {
        try {
            String sql = "INSERT INTO patients " +
                "(id, user_id, patient_number, first_name, last_name, date_of_birth, sex, gender, blood_type, " +
                "civil_status, address, contact_number, emergency_contact_name, emergency_contact_number, " +
                "age, allergies, current_medications, insurance_provider, insurance_number, phil_health_number, " +
                "insurance_expiry, occupation, employer_name, work_address, religion, preferred_language, " +
                "preferred_contact_method, symptoms, height_cm, weight_kg, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
            
            executeUpdate(sql,
                entity.getId(),
                entity.getUser() != null ? entity.getUser().getId() : null,
                entity.getPatientNumber(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getDateOfBirth(),
                entity.getSex(),
                entity.getGender(),
                entity.getBloodType(),
                entity.getCivilStatus(),
                entity.getAddress(),
                entity.getContactNumber(),
                entity.getEmergencyContactName(),
                entity.getEmergencyContactNumber(),
                entity.getAge(),
                entity.getAllergies(),
                entity.getCurrentMedications(),
                entity.getInsuranceProvider(),
                entity.getInsuranceNumber(),
                entity.getPhilHealthNumber(),
                entity.getInsuranceExpiry(),
                entity.getOccupation(),
                entity.getEmployerName(),
                entity.getWorkAddress(),
                entity.getReligion(),
                entity.getPreferredLanguage(),
                entity.getPreferredContactMethod(),
                entity.getSymptoms(),
                entity.getHeightCm(),
                entity.getWeightKg()
            );
            
            return entity;
        } catch (SQLException ex) {
            System.err.println("[PatientRepository] Error inserting patient: " + ex.getMessage());
            return entity;
        }
    }
    
    @Override
    protected Patient updateEntity(Patient entity) {
        try {
            String sql = "UPDATE patients SET " +
                "user_id = ?, patient_number = ?, first_name = ?, last_name = ?, date_of_birth = ?, " +
                "sex = ?, gender = ?, blood_type = ?, civil_status = ?, address = ?, contact_number = ?, " +
                "emergency_contact_name = ?, emergency_contact_number = ?, age = ?, allergies = ?, " +
                "current_medications = ?, insurance_provider = ?, insurance_number = ?, phil_health_number = ?, " +
                "insurance_expiry = ?, occupation = ?, employer_name = ?, work_address = ?, religion = ?, " +
                "preferred_language = ?, preferred_contact_method = ?, symptoms = ?, height_cm = ?, " +
                "weight_kg = ?, updated_at = NOW() WHERE id = ?";
            
            executeUpdate(sql,
                entity.getUser() != null ? entity.getUser().getId() : null,
                entity.getPatientNumber(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getDateOfBirth(),
                entity.getSex(),
                entity.getGender(),
                entity.getBloodType(),
                entity.getCivilStatus(),
                entity.getAddress(),
                entity.getContactNumber(),
                entity.getEmergencyContactName(),
                entity.getEmergencyContactNumber(),
                entity.getAge(),
                entity.getAllergies(),
                entity.getCurrentMedications(),
                entity.getInsuranceProvider(),
                entity.getInsuranceNumber(),
                entity.getPhilHealthNumber(),
                entity.getInsuranceExpiry(),
                entity.getOccupation(),
                entity.getEmployerName(),
                entity.getWorkAddress(),
                entity.getReligion(),
                entity.getPreferredLanguage(),
                entity.getPreferredContactMethod(),
                entity.getSymptoms(),
                entity.getHeightCm(),
                entity.getWeightKg(),
                entity.getId()
            );
            
            return entity;
        } catch (SQLException ex) {
            System.err.println("[PatientRepository] Error updating patient: " + ex.getMessage());
            return entity;
        }
    }
    
    /**
     * Find patient by patient number.
     */
    public Optional<Patient> findByPatientNumber(String patientNumber) {
        try {
            return executeSingleQuery("SELECT * FROM patients WHERE patient_number = ?", patientNumber);
        } catch (SQLException ex) {
            System.err.println("[PatientRepository] Error finding by patient number: " + ex.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Find all patients by last name.
     */
    public List<Patient> findByLastName(String lastName) {
        try {
            return executeQuery("SELECT * FROM patients WHERE last_name LIKE ?", "%" + lastName + "%");
        } catch (SQLException ex) {
            System.err.println("[PatientRepository] Error finding by last name: " + ex.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Find all patients created after a given date.
     */
    public List<Patient> findCreatedAfter(LocalDate date) {
        try {
            return executeQuery("SELECT * FROM patients WHERE DATE(created_at) >= ?", date.toString());
        } catch (SQLException ex) {
            System.err.println("[PatientRepository] Error finding by creation date: " + ex.getMessage());
            return new ArrayList<>();
        }
    }
}
