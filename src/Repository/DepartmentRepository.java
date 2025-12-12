package Repository;

import Model.*;
import java.sql.*;
import java.util.*;

/**
 * Database-backed Department repository.
 */
public class DepartmentRepository extends DatabaseRepository<String, Department> {
    
    private static final DepartmentRepository INSTANCE = new DepartmentRepository();
    
    public static DepartmentRepository getInstance() {
        return INSTANCE;
    }
    
    public DepartmentRepository() {
        super("departments");
    }
    
    @Override
    protected Department mapResultSetToEntity(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        
        Department department = new Department(id, name, description);
        return department;
    }
    
    @Override
    protected String getEntityId(Department entity) {
        return entity.getId();
    }
    
    @Override
    protected String getIdColumnName() {
        return "id";
    }
    
    @Override
    protected Department insertEntity(Department entity) {
        try {
            String sql = "INSERT INTO departments " +
                "(id, name, description, created_at) " +
                "VALUES (?, ?, ?, NOW())";
            
            executeUpdate(sql,
                entity.getId(),
                entity.getName(),
                entity.getDescription()
            );
            
            return entity;
        } catch (SQLException ex) {
            System.err.println("[DepartmentRepository] Error inserting department: " + ex.getMessage());
            return entity;
        }
    }
    
    @Override
    protected Department updateEntity(Department entity) {
        try {
            String sql = "UPDATE departments SET " +
                "name = ?, description = ? WHERE id = ?";
            
            executeUpdate(sql,
                entity.getName(),
                entity.getDescription(),
                entity.getId()
            );
            
            return entity;
        } catch (SQLException ex) {
            System.err.println("[DepartmentRepository] Error updating department: " + ex.getMessage());
            return entity;
        }
    }
    
    /**
     * Find department by name.
     */
    public Optional<Department> findByName(String name) {
        try {
            return executeSingleQuery("SELECT * FROM departments WHERE name = ?", name);
        } catch (SQLException ex) {
            System.err.println("[DepartmentRepository] Error finding by name: " + ex.getMessage());
            return Optional.empty();
        }
    }
}
