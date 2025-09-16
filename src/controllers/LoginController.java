package controllers;

import database.UserDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import models.User;
import models.UserRole;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private TextField emailField;
    @FXML private Button loginButton;
    @FXML private Button toggleButton;
    @FXML private Label statusLabel;
    @FXML private Label loginMessage;
    @FXML private VBox roleSection;
    @FXML private VBox emailSection;
    
    private UserDAO userDAO;
    private boolean isSignUpMode = false;
    private LoginCallback callback;
    
    public interface LoginCallback {
        void onLoginSuccess(User user);
        void onLoginCancel();
    }
    
    @FXML
    public void initialize() {
        userDAO = new UserDAO();
        roleComboBox.setValue("REGULAR");
        
        // Enter key handling
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER")) {
                if (isSignUpMode) handleSignUp(); else handleLogin();
            }
        });
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER")) {
                if (isSignUpMode) handleSignUp(); else handleLogin();
            }
        });
    }
    
    public void setCallback(LoginCallback callback) {
        this.callback = callback;
    }
    
    @FXML
    private void handleLogin() {
        if (isSignUpMode) {
            handleSignUp();
            return;
        }
        
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Please fill in all fields", "error");
            return;
        }
        
        try {
            User user = userDAO.authenticateUser(username, password);
            if (user != null) {
                showStatus("Login successful! Welcome " + user.getUsername(), "success");
                if (callback != null) {
                    callback.onLoginSuccess(user);
                }
            } else {
                showStatus("Invalid username or password", "error");
                passwordField.clear();
            }
        } catch (Exception e) {
            showStatus("Database connection error: " + e.getMessage(), "error");
        }
    }
    
    @FXML
    private void handleSignUp() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String email = emailField.getText().trim();
        String roleStr = roleComboBox.getValue();
        
        // Input validation
        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Username and password are required", "error");
            return;
        }
        
        if (username.length() < 3) {
            showStatus("Username must be at least 3 characters", "error");
            return;
        }
        
        if (password.length() < 4) {
            showStatus("Password must be at least 4 characters", "error");
            return;
        }
        
        // Test database connection first
        if (!database.DatabaseConnection.testConnection()) {
            showStatus("Database connection failed. Please check your database server.", "error");
            return;
        }
        
        try {
            UserRole role = UserRole.valueOf(roleStr);
            
            int result = userDAO.createUser(username, password, role, email.isEmpty() ? null : email);
            
            switch (result) {
                case 0: // Success
                    showStatus("Account created successfully! You can now sign in.", "success");
                    toggleSignUp(); // Switch back to login mode
                    usernameField.setText(username);
                    passwordField.clear();
                    break;
                    
                case 1: // Username exists
                    showStatus("Username '" + username + "' already exists. Please choose a different username.", "error");
                    usernameField.selectAll();
                    usernameField.requestFocus();
                    break;
                    
                case 2: // Invalid password
                    showStatus("Password does not meet requirements (minimum 4 characters)", "error");
                    passwordField.clear();
                    passwordField.requestFocus();
                    break;
                    
                case 3: // Database error
                    showStatus("Database error occurred. Please try again or contact support.", "error");
                    break;
                    
                default:
                    showStatus("Unknown error occurred. Please try again.", "error");
                    break;
            }
            
        } catch (IllegalArgumentException e) {
            showStatus("Invalid user role selected", "error");
        } catch (Exception e) {
            System.err.println("Unexpected error during sign up: " + e.getMessage());
            e.printStackTrace();
            showStatus("Unexpected error: " + e.getMessage(), "error");
        }
    }
    
    @FXML
    private void toggleSignUp() {
        isSignUpMode = !isSignUpMode;
        
        if (isSignUpMode) {
            // Switch to Sign Up mode
            loginMessage.setText("Create a new account");
            loginButton.setText("Sign Up");
            toggleButton.setText("Sign In");
            roleSection.setVisible(true);
            roleSection.setManaged(true);
            emailSection.setVisible(true);
            emailSection.setManaged(true);
        } else {
            // Switch to Sign In mode
            loginMessage.setText("Please sign in to continue");
            loginButton.setText("Sign In");
            toggleButton.setText("Sign Up");
            roleSection.setVisible(false);
            roleSection.setManaged(false);
            emailSection.setVisible(false);
            emailSection.setManaged(false);
        }
        
        clearFields();
        statusLabel.setText("");
    }
    
    private void showStatus(String message, String type) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("success-message", "error-message");
        statusLabel.getStyleClass().add(type.equals("success") ? "success-message" : "error-message");
        
        System.out.println("[" + type.toUpperCase() + "] " + message);
    }
    
    private void clearFields() {
        usernameField.clear();
        passwordField.clear();
        emailField.clear();
        roleComboBox.setValue("REGULAR");
    }
    
    public void focusUsername() {
        usernameField.requestFocus();
    }
}