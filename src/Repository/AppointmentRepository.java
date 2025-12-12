package Repository;

import Model.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Database-backed Appointment repository.
 */
public class AppointmentRepository extends DatabaseRepository<String, Appointment> {
    
    private static final AppointmentRepository INSTANCE = new AppointmentRepository();
    
    public static AppointmentRepository getInstance() {
        return INSTANCE;
    }
    
    public AppointmentRepository() {
        super("appointments");
    }
    
    @Override
    protected Appointment mapResultSetToEntity(ResultSet rs) throws SQLException {
        String patientId = rs.getString("patient_id");
        String doctorId = rs.getString("doctor_id");
        LocalDate scheduleDate = rs.getDate("schedule_date").toLocalDate();
        java.sql.Time scheduleTime = rs.getTime("schedule_time");
        java.time.LocalTime time = scheduleTime != null ? scheduleTime.toLocalTime() : java.time.LocalTime.of(0, 0);
        String reason = rs.getString("reason");
        String statusStr = rs.getString("status");
        AppointmentStatus status = AppointmentStatus.valueOf(statusStr);
        
        Appointment appointment = new Appointment(patientId, doctorId, scheduleDate, time, reason);
        appointment.setStatus(status);
        
        // Set ID from database
        try {
            java.lang.reflect.Field idField = Appointment.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(appointment, rs.getString("id"));
        } catch (Exception e) {
            System.err.println("[AppointmentRepository] Error mapping appointment: " + e.getMessage());
        }
        
        return appointment;
    }
    
    @Override
    protected String getEntityId(Appointment entity) {
        return entity.getId();
    }
    
    @Override
    protected String getIdColumnName() {
        return "id";
    }
    
    @Override
    protected Appointment insertEntity(Appointment entity) {
        try {
            String sql = "INSERT INTO appointments " +
                "(id, patient_id, doctor_id, schedule_date, schedule_time, reason, status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
            
            executeUpdate(sql,
                entity.getId(),
                entity.getPatientId(),
                entity.getDoctorId(),
                entity.getScheduleDate(),
                java.sql.Time.valueOf(entity.getScheduleTime()),
                entity.getReason(),
                entity.getStatus().name()
            );
            
            return entity;
        } catch (SQLException ex) {
            System.err.println("[AppointmentRepository] Error inserting appointment: " + ex.getMessage());
            return entity;
        }
    }
    
    @Override
    protected Appointment updateEntity(Appointment entity) {
        try {
            String sql = "UPDATE appointments SET " +
                "reason = ?, status = ?, updated_at = NOW() WHERE id = ?";
            
            executeUpdate(sql,
                entity.getReason(),
                entity.getStatus().name(),
                entity.getId()
            );
            
            return entity;
        } catch (SQLException ex) {
            System.err.println("[AppointmentRepository] Error updating appointment: " + ex.getMessage());
            return entity;
        }
    }
    
    /**
     * Find appointments for a patient on a specific date.
     */
    public List<Appointment> findByPatientAndDate(String patientId, LocalDate date) {
        try {
            return executeQuery(
                "SELECT * FROM appointments WHERE patient_id = ? AND schedule_date = ? ORDER BY schedule_time",
                patientId, date.toString()
            );
        } catch (SQLException ex) {
            System.err.println("[AppointmentRepository] Error finding appointments: " + ex.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Find all appointments for a doctor on a specific date.
     */
    public List<Appointment> findByDoctorAndDate(String doctorId, LocalDate date) {
        try {
            return executeQuery(
                "SELECT * FROM appointments WHERE doctor_id = ? AND schedule_date = ? ORDER BY schedule_time",
                doctorId, date.toString()
            );
        } catch (SQLException ex) {
            System.err.println("[AppointmentRepository] Error finding doctor appointments: " + ex.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Find appointments by status.
     */
    public List<Appointment> findByStatus(AppointmentStatus status) {
        try {
            return executeQuery("SELECT * FROM appointments WHERE status = ? ORDER BY schedule_date, schedule_time", status.name());
        } catch (SQLException ex) {
            System.err.println("[AppointmentRepository] Error finding by status: " + ex.getMessage());
            return new ArrayList<>();
        }
    }
}
