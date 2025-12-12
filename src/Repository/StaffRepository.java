package Repository;

import Model.*;
import java.sql.*;
import java.util.*;

/**
 * Database-backed Staff repository.
 */
public class StaffRepository extends DatabaseRepository<String, Staff> {
    
    private static final StaffRepository INSTANCE = new StaffRepository();
    
    public static StaffRepository getInstance() {
        return INSTANCE;
    }
    
    public StaffRepository() {
        super("staff");
    }
    
    @Override
    protected Staff mapResultSetToEntity(ResultSet rs) throws SQLException {
        String userId = rs.getString("user_id");
        Optional<User> userOpt = UserRepository.getInstance().findById(userId);
        if (userOpt.isEmpty()) {
            return null;
        }
        
        User user = userOpt.get();
        String firstName = rs.getString("first_name");
        String lastName = rs.getString("last_name");
        String roleType = rs.getString("role_type");
        String departmentId = rs.getString("department_id");
        String contactNumber = rs.getString("contact_number");
        java.time.LocalDate hireDate = rs.getDate("hire_date") != null ? rs.getDate("hire_date").toLocalDate() : null;
        String statusStr = rs.getString("status");
        UserStatus status = UserStatus.valueOf(statusStr);
        
        Staff staff = new Staff(user, firstName, lastName, roleType, departmentId, contactNumber, hireDate, status);
        
        // Set ID from database
        try {
            java.lang.reflect.Field idField = Staff.class.getDeclaredField("staffId");
            idField.setAccessible(true);
            idField.set(staff, rs.getString("id"));
        } catch (Exception e) {
            System.err.println("[StaffRepository] Error mapping staff: " + e.getMessage());
        }
        
        return staff;
    }
    
    @Override
    protected String getEntityId(Staff entity) {
        return entity.getStaffId();
    }
    
    @Override
    protected String getIdColumnName() {
        return "id";
    }
    
    @Override
    protected Staff insertEntity(Staff entity) {
        try {
            String sql = "INSERT INTO staff " +
                "(id, user_id, first_name, last_name, role_type, department_id, contact_number, hire_date, status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
            
            executeUpdate(sql,
                entity.getStaffId(),
                entity.getUserId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getRoleType(),
                entity.getDepartmentId(),
                entity.getContactNumber(),
                entity.getHireDate(),
                entity.getStatus().name()
            );
            
            return entity;
        } catch (SQLException ex) {
            System.err.println("[StaffRepository] Error inserting staff: " + ex.getMessage());
            return entity;
        }
    }
    
    @Override
    protected Staff updateEntity(Staff entity) {
        try {
            String sql = "UPDATE staff SET " +
                "first_name = ?, last_name = ?, role_type = ?, department_id = ?, " +
                "contact_number = ?, hire_date = ?, status = ?, updated_at = NOW() WHERE id = ?";
            
            executeUpdate(sql,
                entity.getFirstName(),
                entity.getLastName(),
                entity.getRoleType(),
                entity.getDepartmentId(),
                entity.getContactNumber(),
                entity.getHireDate(),
                entity.getStatus().name(),
                entity.getStaffId()
            );
            
            return entity;
        } catch (SQLException ex) {
            System.err.println("[StaffRepository] Error updating staff: " + ex.getMessage());
            return entity;
        }
    }
    
    /**
     * Find staff by user ID.
     */
    public Optional<Staff> findByUserId(String userId) {
        try {
            return executeSingleQuery("SELECT * FROM staff WHERE user_id = ?", userId);
        } catch (SQLException ex) {
            System.err.println("[StaffRepository] Error finding by user ID: " + ex.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Find all staff by role type.
     */
    public List<Staff> findByRoleType(String roleType) {
        try {
            return executeQuery("SELECT * FROM staff WHERE role_type = ?", roleType);
        } catch (SQLException ex) {
            System.err.println("[StaffRepository] Error finding by role type: " + ex.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Find all active staff.
     */
    public List<Staff> findActive() {
        try {
            return executeQuery("SELECT * FROM staff WHERE status = ?", UserStatus.ACTIVE.name());
        } catch (SQLException ex) {
            System.err.println("[StaffRepository] Error finding active staff: " + ex.getMessage());
            return new ArrayList<>();
        }
    }
}
