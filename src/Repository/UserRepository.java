package Repository;

import Model.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * Database-backed User repository.
 * Handles persistence of User entities to the users table.
 */
public class UserRepository extends DatabaseRepository<String, User> {
    
    private static final UserRepository INSTANCE = new UserRepository();
    
    public static UserRepository getInstance() {
        return INSTANCE;
    }
    
    public UserRepository() {
        super("users");
    }
    
    @Override
    protected User mapResultSetToEntity(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String username = rs.getString("username");
        String email = rs.getString("email");
        String fullName = rs.getString("full_name");
        String passwordHash = rs.getString("password_hash");
        String roleStr = rs.getString("role");
        String profilePictureUrl = rs.getString("profile_picture_url");
        String statusStr = rs.getString("status");
        String staffNumber = rs.getString("staff_number");
        String linkedPatientId = rs.getString("linked_patient_id");
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        
        Role role = Role.valueOf(roleStr);
        UserStatus status = UserStatus.valueOf(statusStr);
        
        // Reconstruct user object
        User user = new User(username, email, fullName, passwordHash, role);
        
        // Restore fields using reflection or setters
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
            
            java.lang.reflect.Field createdAtField = User.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(user, Instant.ofEpochMilli(createdAtTs.getTime()));
        } catch (Exception e) {
            System.err.println("[UserRepository] Error mapping user: " + e.getMessage());
        }
        
        user.setProfilePictureUrl(profilePictureUrl);
        user.setStatus(status);
        user.setStaffNumber(staffNumber);
        user.setLinkedPatientId(linkedPatientId);
        
        return user;
    }
    
    @Override
    protected String getEntityId(User entity) {
        return entity.getId();
    }
    
    @Override
    protected String getIdColumnName() {
        return "id";
    }
    
    @Override
    protected User insertEntity(User entity) {
        try {
            String sql = "INSERT INTO users " +
                "(id, username, email, full_name, password_hash, role, status, profile_picture_url, staff_number, linked_patient_id, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
            
            executeUpdate(sql,
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getFullName(),
                entity.getPasswordHash(),
                entity.getRole().name(),
                entity.getStatus().name(),
                entity.getProfilePictureUrl(),
                entity.getStaffNumber(),
                entity.getLinkedPatientId()
            );
            
            return entity;
        } catch (SQLException ex) {
            System.err.println("[UserRepository] Error inserting user: " + ex.getMessage());
            return entity;
        }
    }
    
    @Override
    protected User updateEntity(User entity) {
        try {
            String sql = "UPDATE users SET " +
                "email = ?, full_name = ?, password_hash = ?, role = ?, status = ?, " +
                "profile_picture_url = ?, staff_number = ?, linked_patient_id = ?, updated_at = NOW() " +
                "WHERE id = ?";
            
            executeUpdate(sql,
                entity.getEmail(),
                entity.getFullName(),
                entity.getPasswordHash(),
                entity.getRole().name(),
                entity.getStatus().name(),
                entity.getProfilePictureUrl(),
                entity.getStaffNumber(),
                entity.getLinkedPatientId(),
                entity.getId()
            );
            
            return entity;
        } catch (SQLException ex) {
            System.err.println("[UserRepository] Error updating user: " + ex.getMessage());
            return entity;
        }
    }
    
    /**
     * Find user by username (case-insensitive).
     */
    public Optional<User> findByUsername(String username) {
        try {
            return executeSingleQuery("SELECT * FROM users WHERE LOWER(username) = LOWER(?)", username);
        } catch (SQLException ex) {
            System.err.println("[UserRepository] Error finding by username: " + ex.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Find user by email (case-insensitive).
     */
    public Optional<User> findByEmail(String email) {
        try {
            return executeSingleQuery("SELECT * FROM users WHERE LOWER(email) = LOWER(?)", email);
        } catch (SQLException ex) {
            System.err.println("[UserRepository] Error finding by email: " + ex.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Find all users by role.
     */
    public List<User> findByRole(Role role) {
        try {
            return executeQuery("SELECT * FROM users WHERE role = ?", role.name());
        } catch (SQLException ex) {
            System.err.println("[UserRepository] Error finding by role: " + ex.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Find all active users.
     */
    public List<User> findActive() {
        try {
            return executeQuery("SELECT * FROM users WHERE status = ?", UserStatus.ACTIVE.name());
        } catch (SQLException ex) {
            System.err.println("[UserRepository] Error finding active users: " + ex.getMessage());
            return new ArrayList<>();
        }
    }
}
