package com.blooddonor.models;

public class Notification {
    private String notificationId;
    private String title;
    private String message;
    private String type;          // "BLOOD_REQUEST", "DONOR_FOUND", "GENERAL"
    private String relatedId;     // requestId or userId
    private boolean isRead;
    private long createdAt;

    public Notification() {} // Required for Firebase

    public Notification(String title, String message, String type, String relatedId) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.relatedId = relatedId;
        this.isRead = false;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters
    public String getNotificationId() { return notificationId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public String getRelatedId() { return relatedId; }
    public boolean isRead() { return isRead; }
    public long getCreatedAt() { return createdAt; }

    // Setters
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setType(String type) { this.type = type; }
    public void setRelatedId(String relatedId) { this.relatedId = relatedId; }
    public void setRead(boolean read) { isRead = read; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
