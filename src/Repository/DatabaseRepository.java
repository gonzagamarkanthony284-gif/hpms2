package Repository;

import Util.DB;
import java.sql.*;
import java.util.*;

/**
 * Abstract base class for database-backed repositories.
 * Provides common JDBC operations and connection management.
 * Subclasses implement specific table operations via abstract methods.
 *
 * @param <ID> the identifier type (typically String for our models)
 * @param <T> the entity type
 */
public abstract class DatabaseRepository<ID, T> implements Repository<ID, T> {
    
    protected final String tableName;
    
    public DatabaseRepository(String tableName) {
        this.tableName = tableName;
    }
    
    /**
     * Get a database connection.
     */
    protected Connection getConnection() throws SQLException {
        return DB.getConnection();
    }
    
    /**
     * Map a ResultSet row to the entity type.
     * Subclasses implement this to construct the entity from database columns.
     */
    protected abstract T mapResultSetToEntity(ResultSet rs) throws SQLException;
    
    /**
     * Get the ID/primary key from an entity.
     */
    protected abstract ID getEntityId(T entity);
    
    /**
     * Get the primary key column name.
     */
    protected abstract String getIdColumnName();
    
    /**
     * Convert entity ID to SQL value.
     */
    protected Object idToSqlValue(ID id) {
        return id;
    }
    
    /**
     * Save (insert or update) an entity.
     */
    @Override
    public T save(T entity) {
        ID id = getEntityId(entity);
        if (id == null) {
            return insertEntity(entity);
        } else {
            // Check if exists
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                String query = "SELECT 1 FROM " + tableName + " WHERE " + getIdColumnName() + " = ?";
                PreparedStatement pstmt = conn.prepareStatement(query);
                pstmt.setObject(1, idToSqlValue(id));
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return updateEntity(entity);
                    } else {
                        return insertEntity(entity);
                    }
                }
            } catch (SQLException ex) {
                System.err.println("[DatabaseRepository] Error checking if entity exists: " + ex.getMessage());
                return insertEntity(entity);
            }
        }
    }
    
    /**
     * Insert a new entity. Subclasses override to implement specific INSERT logic.
     */
    protected abstract T insertEntity(T entity);
    
    /**
     * Update an existing entity. Subclasses override to implement specific UPDATE logic.
     */
    protected abstract T updateEntity(T entity);
    
    /**
     * Find entity by ID.
     */
    @Override
    public Optional<T> findById(ID id) {
        try (Connection conn = getConnection()) {
            String query = "SELECT * FROM " + tableName + " WHERE " + getIdColumnName() + " = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setObject(1, idToSqlValue(id));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToEntity(rs));
                    }
                }
            }
        } catch (SQLException ex) {
            System.err.println("[DatabaseRepository] Error finding entity by ID: " + ex.getMessage());
        }
        return Optional.empty();
    }
    
    /**
     * Find all entities.
     */
    @Override
    public Collection<T> findAll() {
        Collection<T> results = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {
            
            while (rs.next()) {
                results.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException ex) {
            System.err.println("[DatabaseRepository] Error finding all entities: " + ex.getMessage());
        }
        return results;
    }
    
    /**
     * Delete entity by ID.
     */
    @Override
    public boolean delete(ID id) {
        try (Connection conn = getConnection()) {
            String query = "DELETE FROM " + tableName + " WHERE " + getIdColumnName() + " = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setObject(1, idToSqlValue(id));
                int rowsAffected = stmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException ex) {
            System.err.println("[DatabaseRepository] Error deleting entity: " + ex.getMessage());
        }
        return false;
    }
    
    /**
     * Helper method to execute an update/insert/delete query.
     */
    protected int executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeUpdate();
        }
    }
    
    /**
     * Helper method to query and map results.
     */
    protected List<T> executeQuery(String sql, Object... params) throws SQLException {
        List<T> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSetToEntity(rs));
                }
            }
        }
        return results;
    }
    
    /**
     * Helper to execute query and return single result.
     */
    protected Optional<T> executeSingleQuery(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEntity(rs));
                }
            }
        }
        return Optional.empty();
    }
}
