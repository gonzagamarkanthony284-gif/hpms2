package Repository;

import Model.*;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;

/**
 * Database-backed Billing repository.
 */
public class BillingRepository extends DatabaseRepository<String, Billing> {
    
    private static final BillingRepository INSTANCE = new BillingRepository();
    
    public static BillingRepository getInstance() {
        return INSTANCE;
    }
    
    public BillingRepository() {
        super("billings");
    }
    
    @Override
    protected Billing mapResultSetToEntity(ResultSet rs) throws SQLException {
        String patientId = rs.getString("patient_id");
        BigDecimal amount = rs.getBigDecimal("amount");
        String description = rs.getString("description");
        String statusStr = rs.getString("status");
        BillingStatus status = BillingStatus.valueOf(statusStr);
        
        Billing billing = new Billing(patientId, amount, description);
        billing.setStatus(status);
        
        // Set ID from database
        try {
            java.lang.reflect.Field idField = Billing.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(billing, rs.getString("id"));
        } catch (Exception e) {
            System.err.println("[BillingRepository] Error mapping billing: " + e.getMessage());
        }
        
        return billing;
    }
    
    @Override
    protected String getEntityId(Billing entity) {
        return entity.getId();
    }
    
    @Override
    protected String getIdColumnName() {
        return "id";
    }
    
    @Override
    protected Billing insertEntity(Billing entity) {
        try {
            String sql = "INSERT INTO billings " +
                "(id, patient_id, amount, description, status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), NOW())";
            
            executeUpdate(sql,
                entity.getId(),
                entity.getPatientId(),
                entity.getAmount(),
                entity.getDescription(),
                entity.getStatus().name()
            );
            
            return entity;
        } catch (SQLException ex) {
            System.err.println("[BillingRepository] Error inserting billing: " + ex.getMessage());
            return entity;
        }
    }
    
    @Override
    protected Billing updateEntity(Billing entity) {
        try {
            String sql = "UPDATE billings SET " +
                "status = ?, updated_at = NOW() WHERE id = ?";
            
            executeUpdate(sql,
                entity.getStatus().name(),
                entity.getId()
            );
            
            return entity;
        } catch (SQLException ex) {
            System.err.println("[BillingRepository] Error updating billing: " + ex.getMessage());
            return entity;
        }
    }
    
    /**
     * Find all billings for a patient.
     */
    public List<Billing> findByPatient(String patientId) {
        try {
            return executeQuery("SELECT * FROM billings WHERE patient_id = ? ORDER BY created_at DESC", patientId);
        } catch (SQLException ex) {
            System.err.println("[BillingRepository] Error finding by patient: " + ex.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Find billings by status.
     */
    public List<Billing> findByStatus(BillingStatus status) {
        try {
            return executeQuery("SELECT * FROM billings WHERE status = ? ORDER BY created_at DESC", status.name());
        } catch (SQLException ex) {
            System.err.println("[BillingRepository] Error finding by status: " + ex.getMessage());
            return new ArrayList<>();
        }
    }
}
