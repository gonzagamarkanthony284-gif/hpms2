package Repository;

import Model.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Database-backed Doctor repository.
 */
public class DoctorRepository extends DatabaseRepository<String, Doctor> {
    
    private static final DoctorRepository INSTANCE = new DoctorRepository();
    
    public static DoctorRepository getInstance() {
        return INSTANCE;
    }
    
    public DoctorRepository() {
        super("doctors");
    }
    
    @Override
    protected Doctor mapResultSetToEntity(ResultSet rs) throws SQLException {
        String userId = rs.getString("user_id");
        Optional<User> userOpt = UserRepository.getInstance().findById(userId);
        if (userOpt.isEmpty()) {
            return null;
        }
        
        User user = userOpt.get();
        String specialization = rs.getString("specialization");
        String licenseNumber = rs.getString("license_number");
        LocalDate licenseExpiry = rs.getDate("license_expiry") != null ? rs.getDate("license_expiry").toLocalDate() : null;
        int yearsOfExperience = rs.getInt("years_of_experience");
        String departmentId = rs.getString("department_id");
        java.math.BigDecimal consultationFee = rs.getBigDecimal("consultation_fee");
        String biography = rs.getString("biography");
        String contactNumber = rs.getString("contact_number");
        String statusStr = rs.getString("status");
        UserStatus status = UserStatus.valueOf(statusStr);
        
        Doctor doctor = new Doctor(user, specialization, licenseNumber, licenseExpiry, yearsOfExperience,
                                   departmentId, consultationFee, biography, contactNumber, status);
        
        // Set the ID from database
        try {
            java.lang.reflect.Field idField = Doctor.class.getDeclaredField("doctorId");
            idField.setAccessible(true);
            idField.set(doctor, rs.getString("id"));
        } catch (Exception e) {
            System.err.println("[DoctorRepository] Error mapping doctor: " + e.getMessage());
        }
        
        return doctor;
    }
    
    @Override
    protected String getEntityId(Doctor entity) {
        return entity.getDoctorId();
    }
    
    @Override
    protected String getIdColumnName() {
        return "id";
    }
    
    @Override
    protected Doctor insertEntity(Doctor entity) {
        try {
            String sql = "INSERT INTO doctors " +
                "(id, user_id, specialization, license_number, license_expiry, years_of_experience, " +
                "department_id, consultation_fee, biography, contact_number, status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
            
            executeUpdate(sql,
                entity.getDoctorId(),
                entity.getUserId(),
                entity.getSpecialization(),
                entity.getLicenseNumber(),
                entity.getLicenseExpiry(),
                entity.getYearsOfExperience(),
                entity.getDepartmentId(),
                entity.getConsultationFee(),
                entity.getBiography(),
                entity.getContactNumber(),
                entity.getStatus().name()
            );
            
            return entity;
        } catch (SQLException ex) {
            System.err.println("[DoctorRepository] Error inserting doctor: " + ex.getMessage());
            return entity;
        }
    }
    
    @Override
    protected Doctor updateEntity(Doctor entity) {
        try {
            String sql = "UPDATE doctors SET " +
                "specialization = ?, license_number = ?, license_expiry = ?, years_of_experience = ?, " +
                "department_id = ?, consultation_fee = ?, biography = ?, contact_number = ?, " +
                "status = ?, updated_at = NOW() WHERE id = ?";
            
            executeUpdate(sql,
                entity.getSpecialization(),
                entity.getLicenseNumber(),
                entity.getLicenseExpiry(),
                entity.getYearsOfExperience(),
                entity.getDepartmentId(),
                entity.getConsultationFee(),
                entity.getBiography(),
                entity.getContactNumber(),
                entity.getStatus().name(),
                entity.getDoctorId()
            );
            
            return entity;
        } catch (SQLException ex) {
            System.err.println("[DoctorRepository] Error updating doctor: " + ex.getMessage());
            return entity;
        }
    }
    
    /**
     * Find doctor by user ID.
     */
    public Optional<Doctor> findByUserId(String userId) {
        try {
            return executeSingleQuery("SELECT * FROM doctors WHERE user_id = ?", userId);
        } catch (SQLException ex) {
            System.err.println("[DoctorRepository] Error finding by user ID: " + ex.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Find all doctors by specialization.
     */
    public List<Doctor> findBySpecialization(String specialization) {
        try {
            return executeQuery("SELECT * FROM doctors WHERE specialization = ?", specialization);
        } catch (SQLException ex) {
            System.err.println("[DoctorRepository] Error finding by specialization: " + ex.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Find all active doctors.
     */
    public List<Doctor> findActive() {
        try {
            return executeQuery("SELECT * FROM doctors WHERE status = ?", UserStatus.ACTIVE.name());
        } catch (SQLException ex) {
            System.err.println("[DoctorRepository] Error finding active doctors: " + ex.getMessage());
            return new ArrayList<>();
        }
    }
}
