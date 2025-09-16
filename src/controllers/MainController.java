package controllers;

import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import database.ActivityLogDAO;
import database.DatabaseConnection;
import database.UserDAO;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import javafx.collections.transformation.SortedList;
import javafx.scene.image.*;
import models.ActivityLog;
import models.User;
import models.UserRole;
import java.nio.file.attribute.BasicFileAttributes;

import java.awt.Desktop;
import java.io.*;
import java.nio.file.*;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.Executors;

public class MainController {
    @FXML private TableView<FileItem> table;
    @FXML private TextField searchField;
    @FXML private Button chooseDir;
    @FXML private TextArea filePreviewArea;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Menu databaseMenu;
    @FXML private MenuItem userMgmtMenuItem;
    @FXML private Label userLabel;
    @FXML private MenuItem viewLogsMenuItem;
    @FXML private MenuItem signOutMenuItem;
    @FXML private MenuItem createFileMenuItem;
    @FXML private MenuItem refreshMenuItem;
    @FXML private MenuItem helpMenuItem;
    // Login/signup UI
    @FXML private BorderPane mainView;
    @FXML private VBox loginView;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private TextField emailField;
    @FXML private Button loginButton;
    @FXML private Button toggleButton;
    @FXML private Label loginMessage;
    @FXML private VBox roleSection;
    @FXML private VBox emailSection;


    private boolean isSignUpMode = false;
    
    private final Path workingDirectory = Paths.get(System.getProperty("user.home"),
                                                      "Documents", "JavaFXFileManager");
    private final ObservableList<FileItem> masterList = FXCollections.observableArrayList();
    
    // Database integration
    private User currentUser;
    private UserDAO userDAO;
    private ActivityLogDAO activityLogDAO;
    private boolean isLoggedIn = false;

    @FXML
    public void initialize() {
        userDAO = new UserDAO();
        activityLogDAO = new ActivityLogDAO();

        if (!DatabaseConnection.testConnection()) {
            showAlert("Database Error", "Cannot connect to database. Please check MySQL connection.");
        }

        try {
            Files.createDirectories(workingDirectory);
        } catch (IOException e) {
            showAlert("Error", "Could not create working directory.");
        }

        SortedList<FileItem> sortedList = new SortedList<>(masterList);
        sortedList.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedList);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        setupContextMenu();
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null && isLoggedIn) {
                try {
                    String content = Files.readString(newSel.getPath());
                    filePreviewArea.setText(content);
                    logActivity(newSel.getName(), newSel.getPath().toString(), "ACCESS", "File previewed");
                } catch (IOException e) {
                    filePreviewArea.setText("[Failed to read file]");
                }
            } else {
                filePreviewArea.clear();
            }
        });

        setupKeyShortcuts();
        if (searchField != null) searchField.textProperty().addListener((obs, o, n) -> filterList(n));
        if (chooseDir != null) chooseDir.setOnAction(e -> pickDirectory());

        // Embedded login/signup form
        setupLoginForm();
        showLoginView();

        startWatchService();
    }

    private void pickDirectory() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setInitialDirectory(workingDirectory.toFile());
        File sel = dc.showDialog(table.getScene().getWindow());
        if (sel != null) changeDirectory(sel.toPath());
    }

    // ===== LOGIN/SIGNUP ===== Deprecated, I used another class with a new window
    private void setupLoginForm() {
        roleComboBox.setValue("REGULAR");
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (isSignUpMode) handleSignUp(); else handleLogin();
            }
        });
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (isSignUpMode) handleSignUp(); else handleLogin();
            }
        });
        loginButton.setOnAction(e -> {
            if (isSignUpMode) handleSignUp(); else handleLogin();
        });
        toggleButton.setOnAction(e -> toggleSignUp());
    }

    private void showLoginView() {
        loginView.setVisible(true);
        loginView.setManaged(true);
        mainView.setVisible(false);
        mainView.setManaged(false);
        Platform.runLater(() -> usernameField.requestFocus());
        loginMessage.setText(isSignUpMode ? "Create a new account" : "Please sign in to continue");
        loginButton.setText(isSignUpMode ? "Sign Up" : "Sign In");
        toggleButton.setText(isSignUpMode ? "Sign In" : "Sign Up");
        roleSection.setVisible(isSignUpMode); roleSection.setManaged(isSignUpMode);
        emailSection.setVisible(isSignUpMode); emailSection.setManaged(isSignUpMode);
    }

    private void showMainView() {
        loginView.setVisible(false);
        loginView.setManaged(false);
        mainView.setVisible(true);
        mainView.setManaged(true);
        refreshFileList();
        updateStatusForUser();
    }



    // ===== ACTIVITY LOGGING =====
    private void logActivity(String fileName, String filePath, String action, String details) {
        if (currentUser != null && activityLogDAO != null) {
            try {
                activityLogDAO.logActivity(currentUser.getUserId(), fileName, filePath, action, details);
            } catch (Exception e) {
                System.err.println("Failed to log activity: " + e.getMessage());
            }
        }
    }
    

    @FXML
    private void handleLogin() {
        String u = usernameField.getText().trim();
        String p = passwordField.getText();
        if (u.isEmpty() || p.isEmpty()) {
            showLoginStatus("Please fill in all fields", "error");
            return;
        }
        try {
            User user = userDAO.authenticateUser(u, p);
            if (user != null) {
                currentUser = user; isLoggedIn = true;
                logActivity("System", "N/A", "LOGIN", "User logged in");
                showLoginStatus("Login successful! Welcome " + u, "success");
                Platform.runLater(this::showMainView);
            } else {
                showLoginStatus("Invalid username or password", "error");
                passwordField.clear();
            }
        } catch (Exception ex) {
            showLoginStatus("Database error: " + ex.getMessage(), "error");
        }
    }

    @FXML
    private void handleSignUp() {
        String u = usernameField.getText().trim();
        String p = passwordField.getText();
        String e = emailField.getText().trim();
        String r = roleComboBox.getValue();

        if (u.length() < 3 || p.length() < 4) {
            showLoginStatus("Username and password must be longer", "error");
            return;
        }

        try {
            int result = userDAO.createUser(u, p, UserRole.valueOf(r), e.isEmpty() ? null : e);

            switch (result) {
                case 0:
                    showLoginStatus("Account created! Please sign in.", "success");
                    toggleSignUp();
                    break;
                case 1:
                    showLoginStatus("Username already exists.", "error");
                    break;
                case 2:
                    showLoginStatus("Password doesn't meet criteria.", "error");
                    break;
                case 3:
                default:
                    showLoginStatus("Database error occurred.", "error");
                    break;
            }
        } catch (Exception ex) {
            showLoginStatus("Unexpected error: " + ex.getMessage(), "error");
        }
    }


    private void toggleSignUp() {
        isSignUpMode = !isSignUpMode;
        showLoginView();
    }

    private void showLoginStatus(String msg, String type) {
        loginMessage.setText(msg);
    }
    private void changeDirectory(Path newDir) {
        try {
            Files.createDirectories(newDir);
            // update workingDirectory via reflection? or make workingDirectory non-final
        } catch (IOException e) {
            showAlert("Error","Cannot change directory.");
        }
        // Note: workingDirectory is final; to fully support change, you'd store it in a mutable field.
        refreshFileList();
    }
    private void startWatchService() {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                WatchService ws = FileSystems.getDefault().newWatchService();
                workingDirectory.register(ws,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);
                while (true) {
                    ws.take().pollEvents();
                    Platform.runLater(this::refreshFileList);
                }
            } catch (Exception ignored) {}
        });
    }
    
    private void showLoginDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/LoginDialog.fxml"));
            Parent loginRoot = loader.load();
            
            LoginController loginController = loader.getController();
            loginController.setCallback(new LoginController.LoginCallback() {
                @Override
                public void onLoginSuccess(User user) {
                    currentUser = user;
                    isLoggedIn = true;
                    updateUIForLoginState();
                    refreshFileList();
                    logActivity("System", "N/A", "LOGIN", "User logged in");
                    
                    // Close login dialog
                    Stage loginStage = (Stage) loginRoot.getScene().getWindow();
                    loginStage.close();
                }
                
                @Override
                public void onLoginCancel() {
                    Platform.exit();
                }
            });
            
            Stage loginStage = new Stage();
            loginStage.setTitle("Login Required");
            loginStage.setScene(new Scene(loginRoot));
            loginStage.setResizable(false);
            loginStage.initModality(Modality.APPLICATION_MODAL);
            loginStage.initOwner(table.getScene().getWindow());
            
            // Prevent closing without login
            loginStage.setOnCloseRequest(e -> Platform.exit());
            
            loginStage.showAndWait();
            
            // Focus on username field
            loginController.focusUsername();
            
        } catch (IOException e) {
            showAlert("Error", "Could not load login dialog: " + e.getMessage());
            Platform.exit();
        }
    }
    
    private void updateUIForLoginState() {
        if (isLoggedIn && currentUser != null) {
            // Update user label
            String roleIcon = currentUser.isAdmin() ? "👑" : "👤";
            if (userLabel != null) {
                userLabel.setText(roleIcon + " " + currentUser.getUsername() + " (" + currentUser.getRole().getDisplayName() + ")");
            } else {
                statusLabel.setText("Logged in as: " + roleIcon + " " + currentUser.getUsername());
            }
            
            // Enable/disable UI based on role
            boolean isAdmin = currentUser.isAdmin();
            
            // Admin-only menu items
            if (userMgmtMenuItem != null) {
                userMgmtMenuItem.setDisable(!isAdmin);
            }
            
            // Enable database menu
            if (databaseMenu != null) {
                databaseMenu.setDisable(false);
            }
            
        } else {
            // Not logged in - disable most functionality
            if (userLabel != null) {
                userLabel.setText("Not logged in");
            }
            if (databaseMenu != null) {
                databaseMenu.setDisable(true);
            }
        }
    }

    private void setupContextMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem open = new MenuItem("Open"); 
        open.setOnAction(e -> openSelected());
        
        MenuItem copy = new MenuItem("Copy"); 
        copy.setOnAction(e -> copySelected());
        
        MenuItem move = new MenuItem("Move"); 
        move.setOnAction(e -> moveSelected());
        
        MenuItem delete = new MenuItem("Delete"); 
        delete.setOnAction(e -> {
            if (currentUser != null && currentUser.isAdmin()) {
                deleteSelected();
            } else {
                showAlert("Access Denied", "Only administrators can delete files.");
            }
        });
        
        MenuItem rename = new MenuItem("Rename"); 
        rename.setOnAction(e -> handleRenameFile());
        
        MenuItem duplicate = new MenuItem("Duplicate"); 
        duplicate.setOnAction(e -> duplicateSelected());
        
        MenuItem compress = new MenuItem("Compress"); 
        compress.setOnAction(e -> compressSelected());
        
        MenuItem properties = new MenuItem("Properties"); 
        properties.setOnAction(e -> showProperties());
        
        menu.getItems().addAll(open, copy, move, delete, rename, duplicate, compress, properties);
        table.setContextMenu(menu);

        table.setOnMouseClicked((MouseEvent evt) -> {
            if (evt.getClickCount() == 2) openSelected();
            else handleTableClick(evt);
        });
    }
    @FXML
    private void handleTableClick(MouseEvent event) {
        FileItem sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            String content = Files.readString(sel.getPath());
            filePreviewArea.setText(content);
        } catch (IOException e) {
            showAlert("Error","Failed to read file.");
        }
    }
    private void setupKeyShortcuts() {
        table.setOnKeyPressed((KeyEvent event) -> {
            if (event.getCode() == KeyCode.DELETE) {
                if (currentUser != null && currentUser.isAdmin()) {
                    deleteSelected();
                } else {
                    showAlert("Access Denied", "Only administrators can delete files.");
                }
            }
            if (event.getCode() == KeyCode.F2) handleRenameFile();
            if (event.isControlDown() && event.getCode() == KeyCode.C) copySelected();
            if (event.isControlDown() && event.getCode() == KeyCode.V) moveSelected();
            if (event.isControlDown() && event.getCode() == KeyCode.F) searchField.requestFocus();
        });
    }
    
    @FXML
    private void refreshFileList() {
        if (!isLoggedIn) return;
        
        FileItem selected = table.getSelectionModel().getSelectedItem();
        try {
            List<FileItem> items = Files.list(workingDirectory)
                                        .map(FileItem::new)
                                        .toList();
            masterList.setAll(items);

            // Restore selection
            if (selected != null) {
                for (FileItem item : items) {
                    if (item.getPath().equals(selected.getPath())) {
                        table.getSelectionModel().select(item);
                        break;
                    }
                }
            }

        } catch (IOException e) {
            showAlert("Error", "Failed to read files.");
        }
    }

    private void filterList(String filter) {
        if (!isLoggedIn) return;
        
        if (filter == null || filter.isBlank()) {
            refreshFileList();
        } else {
            masterList.setAll(
                masterList.stream()
                          .filter(item -> item.getName().toLowerCase()
                                              .contains(filter.toLowerCase()))
                          .toList()
            );
        }
    }

    // ===== Database Menu Handlers =====
    @FXML
    private void handleViewLogs() {
        if (!isLoggedIn) return;
        
        try {
            List<ActivityLog> logs = activityLogDAO.getRecentActivities(50);
            
            StringBuilder logText = new StringBuilder();
            logText.append("Recent File Activities:\n");
            logText.append("=".repeat(50)).append("\n\n");
            
            for (ActivityLog log : logs) {
                logText.append(String.format("[%s] %s - %s: %s\n",
                    log.getTimestamp().toString(),
                    log.getUsername(),
                    log.getAction(),
                    log.getFileName()
                ));
                if (log.getDetails() != null && !log.getDetails().isEmpty()) {
                    logText.append("  Details: ").append(log.getDetails()).append("\n");
                }
                logText.append("\n");
            }
            
            // Show in a new window
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Activity Logs");
            alert.setHeaderText("File Activity History");
            
            TextArea textArea = new TextArea(logText.toString());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setPrefSize(600, 400);
            
            alert.getDialogPane().setContent(textArea);
            alert.showAndWait();
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load activity logs: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleUserManagement() {
        if (!isLoggedIn || !currentUser.isAdmin()) {
            showAlert("Access Denied", "Only administrators can manage users.");
            return;
        }
        
        // Simple user management dialog
        List<String> choices = Arrays.asList("View All Users", "Create New User", "Change User Role");
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>("View All Users", choices);
        dialog.setTitle("User Management");
        dialog.setHeaderText("Select an action:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(choice -> {
            switch (choice) {
                case "View All Users" -> showAllUsers();
                case "Create New User" -> createNewUser();
                case "Change User Role" -> changeUserRole();
            }
        });
    }
    
    @FXML
    private void handleSignOut() {
        if (isLoggedIn) {
            logActivity("System", "N/A", "LOGOUT", "User logged out");
        }
        
        currentUser = null;
        isLoggedIn = false;
        updateUIForLoginState();
        masterList.clear();
        filePreviewArea.clear();
        
        // Show login dialog again
        showLoginDialog();
    }

    // ===== User Management Methods =====
    private void showAllUsers() {
        try {
            List<User> users = userDAO.getAllUsers();
            
            StringBuilder userList = new StringBuilder();
            userList.append("All Users:\n");
            userList.append("=".repeat(40)).append("\n\n");
            
            for (User user : users) {
                userList.append(String.format("• %s (%s) - %s\n",
                    user.getUsername(),
                    user.getRole().getDisplayName(),
                    user.getEmail() != null ? user.getEmail() : "No email"
                ));
                if (user.getLastLogin() != null) {
                    userList.append("  Last Login: ").append(user.getLastLogin()).append("\n");
                }
                userList.append("\n");
            }
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("User Management");
            alert.setHeaderText("All Users");
            
            TextArea textArea = new TextArea(userList.toString());
            textArea.setEditable(false);
            textArea.setPrefSize(400, 300);
            
            alert.getDialogPane().setContent(textArea);
            alert.showAndWait();
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load users: " + e.getMessage());
        }
    }
    
    private void createNewUser() {
        TextInputDialog usernameDialog = new TextInputDialog();
        usernameDialog.setTitle("Create User");
        usernameDialog.setHeaderText("Enter username:");
        
        Optional<String> username = usernameDialog.showAndWait();
        if (!username.isPresent() || username.get().trim().isEmpty()) return;
        
        TextInputDialog passwordDialog = new TextInputDialog();
        passwordDialog.setTitle("Create User");
        passwordDialog.setHeaderText("Enter password:");
        
        Optional<String> password = passwordDialog.showAndWait();
        if (!password.isPresent() || password.get().trim().isEmpty()) return;
        
        List<String> roles = Arrays.asList("REGULAR", "ADMIN");
        ChoiceDialog<String> roleDialog = new ChoiceDialog<>("REGULAR", roles);
        roleDialog.setTitle("Create User");
        roleDialog.setHeaderText("Select role:");
        
        Optional<String> role = roleDialog.showAndWait();
        if (!role.isPresent()) return;
        
        try {
        	int result = userDAO.createUser(
        		    username.get().trim(), 
        		    password.get(), 
        		    UserRole.valueOf(role.get()), 
        		    null
        		);

        		if (result == 0) {
        		    showAlert("Success", "User created successfully!");
        		    logActivity("System", "N/A", "CREATE_USER", "Created user: " + username.get());
        		} else if (result == 1) {
        		    showAlert("Error", "Username already exists.");
        		} else if (result == 2) {
        		    showAlert("Error", "Password does not meet the required criteria.");
        		} else {
        		    showAlert("Error", "Failed to create user due to a database error.");
        		}

        } catch (Exception e) {
            showAlert("Error", "Error creating user: " + e.getMessage());
        }
    }
    
    private void changeUserRole() {
        // Implementation for changing user roles
        showAlert("Info", "Change User Role feature - implement based on your needs!");
    }

    // ===== Enhanced File Operations with Logging =====
    @FXML 
    private void handleRefresh() { 
        refreshFileList(); 
    }

    @FXML 
    private void handleCreateFile() {
        if (!isLoggedIn) {
            showAlert("Error", "Please log in first.");
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog("newfile.txt");
        dialog.setTitle("Create File");
        dialog.setGraphic(new Label("📄"));
        dialog.setHeaderText("Enter file name (with extension):");
        dialog.showAndWait().ifPresent(name -> {
            setStatus("Creating file: " + name, true);
            try {
                Path newFile = workingDirectory.resolve(name);
                Files.createFile(newFile);
                refreshFileList();
                setStatus("File created: " + name, false);
                
                // Log activity
                logActivity(name, newFile.toString(), "CREATE", "File created by user");
                
            } catch (IOException e) {
                showAlert("Error", "Failed to create file.");
                setStatus("Failed to create file: " + name, false);
            }
        });
    }
    
    @FXML
    private void handleHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText("JavaFX File Manager - Help");
        alert.setContentText("This file manager is developed for KACST TASK1.\n\n"
                           + "Features:\n"
                           + "• Role-based access (Admin/Regular users)\n"
                           + "• Only admins can delete files\n"
                           + "• All activities are logged to database\n"
                           + "• User management for admins\n\n"
                           + "For questions or feedback:\n"
                           + "albuqami49@outlook.com");

        try {
            Image icon = new Image(getClass().getResourceAsStream("/KACST.png"));
            ImageView iconView = new ImageView(icon);
            iconView.setFitWidth(48);
            iconView.setFitHeight(48);
            alert.setGraphic(iconView);
        } catch (Exception e) {
            // Icon not found, continue without it
        }
        
        alert.showAndWait();
    }

    @FXML 
    private void deleteSelected() {
        if (!isLoggedIn) {
            showAlert("Error", "Please log in first.");
            return;
        }
        
        if (!currentUser.isAdmin()) {
            showAlert("Access Denied", "Only administrators can delete files.");
            return;
        }
        
        List<FileItem> sel = table.getSelectionModel().getSelectedItems();
        if (sel.isEmpty()) { 
            showAlert("Warning","No file selected."); 
            return; 
        }
        
        // Confirmation dialog
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete " + sel.size() + " file(s)?");
        confirmAlert.setContentText("This action cannot be undone.");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            sel.forEach(item -> {
                try { 
                    Files.delete(item.getPath());
                    // Log deletion
                    logActivity(item.getName(), item.getPath().toString(), "DELETE", "File deleted by admin");
                }
                catch (IOException e){ 
                    showAlert("Error","Failed to delete: " + item.getName()); 
                }
            });
            refreshFileList();
        }
    }

    @FXML 
    private void handleRenameFile() {
        if (!isLoggedIn) {
            showAlert("Error", "Please log in first.");
            return;
        }
        
        FileItem sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { 
            showAlert("Warning","No file selected."); 
            return; 
        }
        
        TextInputDialog dialog = new TextInputDialog(sel.getName());
        dialog.setTitle("Rename File");
        dialog.setGraphic(new Label("📄"));
        dialog.setHeaderText("Enter new file name:");
        dialog.showAndWait().ifPresent(newName -> {
            try {
                Path newPath = workingDirectory.resolve(newName);
                Files.move(sel.getPath(), newPath);
                refreshFileList();
                
                // Log rename
                logActivity(sel.getName(), sel.getPath().toString(), "RENAME", "Renamed to: " + newName);
                
            } catch (IOException e) {
                showAlert("Error","Failed to rename file.");
            }
        });
    }

    @FXML private void handleAddTextToFile() {
        FileItem sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Warning","No file selected."); return; }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Text");
        dialog.setGraphic(new Label("📄"));
        dialog.setHeaderText("Enter text to add to file:");
        dialog.showAndWait().ifPresent(text -> {
            try (BufferedWriter writer = Files.newBufferedWriter(
                     sel.getPath(), StandardOpenOption.APPEND)) {
                writer.write(text);
                writer.newLine();
            } catch (IOException e) {
                showAlert("Error","Failed to write to file.");
            }
        });
    }
    @FXML private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }
    private void setStatus(String message, boolean loading) {
        statusLabel.setText(message);
        progressIndicator.setVisible(loading);
    }
    @FXML private void openSelected() {
        table.getSelectionModel().getSelectedItems().forEach(item -> {
            try { Desktop.getDesktop().open(item.getPath().toFile()); }
            catch (IOException e){ showAlert("Error","Cannot open: " + item.getName()); }
        });
    }
    @FXML private void copySelected() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Copy Files To...");
        File dest = fc.showSaveDialog(table.getScene().getWindow());
        if (dest == null) return;
        table.getSelectionModel().getSelectedItems().forEach(item -> {
            try {
                Files.copy(item.getPath(), dest.toPath().resolve(item.getName()));
            } catch (IOException e) {
                showAlert("Error","Copy failed for: " + item.getName());
            }
        });
        refreshFileList();
    }

    @FXML private void moveSelected() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Move Files To...");
        File dest = fc.showSaveDialog(table.getScene().getWindow());
        if (dest == null) return;
        table.getSelectionModel().getSelectedItems().forEach(item -> {
            try {
                Files.move(item.getPath(), dest.toPath().resolve(item.getName()));
            } catch (IOException e) {
                showAlert("Error","Move failed for: " + item.getName());
            }
        });
        refreshFileList();
    }

    @FXML private void duplicateSelected() {
        table.getSelectionModel().getSelectedItems().forEach(item -> {
            Path copy = item.getPath().resolveSibling(item.getName() + "-copy");
            try { Files.copy(item.getPath(), copy); }
            catch (IOException e){ showAlert("Error","Duplicate failed: " + item.getName()); }
        });
        refreshFileList();
    }

    @FXML private void compressSelected() {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("archive.zip");
        File out = fc.showSaveDialog(table.getScene().getWindow());
        if (out == null) return;
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out))) {
            for (FileItem item : table.getSelectionModel().getSelectedItems()) {
                zos.putNextEntry(new ZipEntry(item.getName()));
                Files.copy(item.getPath(), zos);
                zos.closeEntry();
            }
        } catch (IOException e) {
            showAlert("Error","Compression failed.");
        }
    }
    private void showProperties() {
        FileItem item = table.getSelectionModel().getSelectedItem();
        if (item == null) return;
        try {
            BasicFileAttributes attr = Files.readAttributes(item.getPath(), BasicFileAttributes.class);
            String info = String.format("Name: %s\nSize: %d bytes\nCreated: %s\nModified: %s",
                    item.getName(), attr.size(),
                    attr.creationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                    attr.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            showAlert("Properties", info);
        } catch (IOException e) {
            showAlert("Error","Cannot read properties.");
        }
    }


    private void updateStatusForUser() {
        if (currentUser!=null) {
            String icon = currentUser.isAdmin()?"👑":"👤";
            userLabel.setText(icon + " " + currentUser.getUsername() + " (" + currentUser.getRole().getDisplayName() + ")");
        }
    }

}
