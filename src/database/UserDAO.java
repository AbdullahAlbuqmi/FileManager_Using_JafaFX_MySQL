package database;

import models.User;
import models.UserRole;
import utils.PasswordUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    
    /**
     * Check if a username already exists in the database
     */
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking username existence: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Create a new user with improved error handling
     * Returns: 0 = success, 1 = username exists, 2 = invalid password, 3 = database error
     */

    
    @Deprecated
    public boolean createUser_old(String username, String password, UserRole role, String email) {
        return createUser(username, password, role, email) == 0;
    }
    
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY username";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all users: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }
    
    public boolean updateUserRole(int userId, UserRole newRole) {
        String sql = "UPDATE users SET role = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, newRole.name());
            stmt.setInt(2, userId);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user role: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean changePassword(int userId, String newPassword) {
        if (!PasswordUtils.isValidPassword(newPassword)) {
            return false;
        }
        
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, PasswordUtils.hashPassword(newPassword));
            stmt.setInt(2, userId);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error changing password: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating last login: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(UserRole.valueOf(rs.getString("role")));
        user.setEmail(rs.getString("email"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) {
            user.setLastLogin(lastLogin.toLocalDateTime());
        }
        
        return user;
    }
    
 // Add this debugging version to your UserDAO class
    public User authenticateUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ?";
        
        System.out.println("DEBUG - Attempting to authenticate user: '" + username + "'");
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                System.out.println("DEBUG - User found in database");
                String storedHash = rs.getString("password_hash");
                System.out.println("DEBUG - Retrieved stored hash: '" + storedHash + "'");
                
                // Test the password verification
                boolean passwordMatch = PasswordUtils.verifyPassword(password, storedHash);
                System.out.println("DEBUG - Password verification result: " + passwordMatch);
                
                if (passwordMatch) {
                    System.out.println("DEBUG - Authentication successful");
                    User user = mapResultSetToUser(rs);
                    updateLastLogin(user.getUserId());
                    return user;
                } else {
                    System.out.println("DEBUG - Password verification failed");
                }
            } else {
                System.out.println("DEBUG - User not found in database");
            }
        } catch (SQLException e) {
            System.err.println("Error during authentication: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Also add debugging to your createUser method
    public int createUser(String username, String password, UserRole role, String email) {
        // Validate password first
        if (!PasswordUtils.isValidPassword(password)) {
            System.out.println("Password validation failed for user: " + username);
            return 2; // Invalid password
        }
        
        // Check if username already exists
        if (usernameExists(username)) {
            System.out.println("Username already exists: " + username);
            return 1; // Username exists
        }
        
        String hashedPassword = PasswordUtils.hashPassword(password);
        System.out.println("DEBUG - Creating user: '" + username + "'");
        System.out.println("DEBUG - Original password: '" + password + "'");
        System.out.println("DEBUG - Hashed password: '" + hashedPassword + "'");
        
        String sql = "INSERT INTO users (username, password_hash, role, email) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, role.name());
            stmt.setString(4, email);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("User created successfully: " + username);
                return 0; // Success
            } else {
                System.out.println("No rows affected when creating user: " + username);
                return 3; // Database error
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("Duplicate username constraint violation: " + e.getMessage());
            return 1; // Username exists
        } catch (SQLException e) {
            System.err.println("SQL Error creating user '" + username + "': " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            return 3; // Database error
        }
    }
}