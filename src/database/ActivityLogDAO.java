package database;

import models.ActivityLog;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDAO {
    
    public boolean logActivity(int userId, String fileName, String filePath, String action, String details) {
        String sql = "INSERT INTO file_activities (user_id, file_name, file_path, action, details) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setString(2, fileName);
            stmt.setString(3, filePath);
            stmt.setString(4, action);
            stmt.setString(5, details);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public List<ActivityLog> getRecentActivities(int limit) {
        List<ActivityLog> activities = new ArrayList<>();
        String sql = """
            SELECT fa.*, u.username 
            FROM file_activities fa 
            JOIN users u ON fa.user_id = u.user_id 
            ORDER BY fa.timestamp DESC 
            LIMIT ?
            """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                activities.add(mapResultSetToActivityLog(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return activities;
    }
    
    public List<ActivityLog> getActivitiesByUser(int userId) {
        List<ActivityLog> activities = new ArrayList<>();
        String sql = """
            SELECT fa.*, u.username 
            FROM file_activities fa 
            JOIN users u ON fa.user_id = u.user_id 
            WHERE fa.user_id = ? 
            ORDER BY fa.timestamp DESC
            """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                activities.add(mapResultSetToActivityLog(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return activities;
    }
    
    private ActivityLog mapResultSetToActivityLog(ResultSet rs) throws SQLException {
        ActivityLog log = new ActivityLog();
        log.setActivityId(rs.getInt("activity_id"));
        log.setUserId(rs.getInt("user_id"));
        log.setUsername(rs.getString("username"));
        log.setFileName(rs.getString("file_name"));
        log.setFilePath(rs.getString("file_path"));
        log.setAction(rs.getString("action"));
        log.setDetails(rs.getString("details"));
        
        Timestamp timestamp = rs.getTimestamp("timestamp");
        if (timestamp != null) {
            log.setTimestamp(timestamp.toLocalDateTime());
        }
        
        return log;
    }
}