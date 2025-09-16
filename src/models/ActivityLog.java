package models;

import java.time.LocalDateTime;

public class ActivityLog {
    private int activityId;
    private int userId;
    private String username; // For display purposes
    private String fileName;
    private String filePath;
    private String action;
    private LocalDateTime timestamp;
    private String details;

    // Constructors
    public ActivityLog() {}

    public ActivityLog(int userId, String fileName, String filePath, String action, String details) {
        this.userId = userId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.action = action;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public int getActivityId() { return activityId; }
    public void setActivityId(int activityId) { this.activityId = activityId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
