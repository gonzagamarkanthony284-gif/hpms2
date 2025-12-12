package Repository;

import Model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Database-backed Admission repository.
 */
public class AdmissionRepository extends DatabaseRepository<String, Admission> {
    
    private static final AdmissionRepository INSTANCE = new AdmissionRepository();
    
    public static AdmissionRepository getInstance() {
        return INSTANCE;
    }
    
    public AdmissionRepository() {
        super("admissions");
    }
    
    @Override
    protected Admission mapResultSetToEntity(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String patientId = rs.getString("patient_id");
        Timestamp admittedAtTs = rs.getTimestamp("admitted_at");
        LocalDateTime admittedAt = admittedAtTs != null ? admittedAtTs.toLocalDateTime() : null;
        String admittedBy = rs.getString("admitted_by");
        String wardId = rs.getString("ward_id");
        String roomId = rs.getString("room_id");
        String bedId = rs.getString("bed_id");
        String admissionReason = rs.getString("admission_reason");
        String statusStr = rs.getString("status");
        AdmissionStatus status = AdmissionStatus.valueOf(statusStr);
        Timestamp dischargedAtTs = rs.getTimestamp("discharged_at");
        LocalDateTime dischargedAt = dischargedAtTs != null ? dischargedAtTs.toLocalDateTime() : null;
        String dischargeSummaryId = rs.getString("discharge_summary_id");
        
        Admission admission = new Admission(id, patientId, admittedAt, admittedBy, wardId, roomId, bedId,
                                           admissionReason, status, dischargedAt, dischargeSummaryId);
        return admission;
    }
    
    @Override
    protected String getEntityId(Admission entity) {
        return entity.getId();
    }
    
    @Override
    protected String getIdColumnName() {
        return "id";
    }
    
    @Override
    protected Admission insertEntity(Admission entity) {
        try {
            String sql = "INSERT INTO admissions " +
                "(id, patient_id, admitted_at, admitted_by, ward_id, room_id, bed_id, admission_reason, status, discharged_at, discharge_summary_id, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
            
            executeUpdate(sql,
                entity.getId(),
                entity.getPatientId(),
                entity.getAdmittedAt(),
                entity.getAdmittedBy(),
                entity.getWardId(),
                entity.getRoomId(),
                entity.getBedId(),
                entity.getAdmissionReason(),
                entity.getStatus().name(),
                entity.getDischargedAt(),
                entity.getDischargeSummaryId()
            );
            
            return entity;
        } catch (SQLException ex) {
            System.err.println("[AdmissionRepository] Error inserting admission: " + ex.getMessage());
            return entity;
        }
    }
    
    @Override
    protected Admission updateEntity(Admission entity) {
        try {
            String sql = "UPDATE admissions SET " +
                "status = ?, discharged_at = ?, discharge_summary_id = ?, updated_at = NOW() WHERE id = ?";
            
            executeUpdate(sql,
                entity.getStatus().name(),
                entity.getDischargedAt(),
                entity.getDischargeSummaryId(),
                entity.getId()
            );
            
            return entity;
        } catch (SQLException ex) {
            System.err.println("[AdmissionRepository] Error updating admission: " + ex.getMessage());
            return entity;
        }
    }
    
    /**
     * Find active admissions for a patient.
     */
    public Optional<Admission> findActiveAdmission(String patientId) {
        try {
            return executeSingleQuery(
                "SELECT * FROM admissions WHERE patient_id = ? AND status = ? ORDER BY admitted_at DESC LIMIT 1",
                patientId, AdmissionStatus.ACTIVE.name()
            );
        } catch (SQLException ex) {
            System.err.println("[AdmissionRepository] Error finding active admission: " + ex.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Find all admissions for a patient.
     */
    public List<Admission> findByPatient(String patientId) {
        try {
            return executeQuery(
                "SELECT * FROM admissions WHERE patient_id = ? ORDER BY admitted_at DESC",
                patientId
            );
        } catch (SQLException ex) {
            System.err.println("[AdmissionRepository] Error finding admissions: " + ex.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Find all active admissions.
     */
    public List<Admission> findActive() {
        try {
            return executeQuery(
                "SELECT * FROM admissions WHERE status = ? ORDER BY admitted_at DESC",
                AdmissionStatus.ACTIVE.name()
            );
        } catch (SQLException ex) {
            System.err.println("[AdmissionRepository] Error finding active admissions: " + ex.getMessage());
            return new ArrayList<>();
        }
    }
}
