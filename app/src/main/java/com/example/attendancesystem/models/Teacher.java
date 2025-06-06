package com.example.attendancesystem.models;

import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class Teacher {
    private String email; // Identifiant unique
    private String fullName;
    private String employeeId;
    private String profileImageUrl;
    private Timestamp createdAt;
    private boolean isActive;
    private String phoneNumber;
    private String department;
    private NotificationPreferences notificationPreferences;
    private Timestamp lastLoginAt;
    private Timestamp lastUpdatedAt;

    // Constructeur vide requis pour Firebase
    public Teacher() {
        this.notificationPreferences = new NotificationPreferences();
    }

    // Constructeur avec paramètres essentiels
    public Teacher(String email, String fullName, String employeeId, String department) {
        this.email = email;
        this.fullName = fullName;
        this.employeeId = employeeId;
        this.department = department;
        this.createdAt = Timestamp.now();
        this.isActive = true;
        this.profileImageUrl = "";
        this.notificationPreferences = new NotificationPreferences();
        this.lastUpdatedAt = Timestamp.now();
    }

    // Méthode pour convertir en Map pour Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("email", email);
        map.put("fullName", fullName);
        map.put("employeeId", employeeId);
        map.put("profileImageUrl", profileImageUrl);
        map.put("createdAt", createdAt);
        map.put("isActive", isActive);
        map.put("phoneNumber", phoneNumber);
        map.put("department", department);
        if (notificationPreferences != null) {
            map.put("notificationPreferences", notificationPreferences.toMap());
        }
        map.put("lastLoginAt", lastLoginAt);
        map.put("lastUpdatedAt", lastUpdatedAt);
        return map;
    }

    // Getters
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getEmployeeId() { return employeeId; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public Timestamp getCreatedAt() { return createdAt; }
    public boolean isActive() { return isActive; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getDepartment() { return department; }
    public NotificationPreferences getNotificationPreferences() { return notificationPreferences; }
    public Timestamp getLastLoginAt() { return lastLoginAt; }
    public Timestamp getLastUpdatedAt() { return lastUpdatedAt; }

    // Setters
    public void setEmail(String email) { this.email = email; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setActive(boolean active) { this.isActive = active; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setDepartment(String department) { this.department = department; }
    public void setNotificationPreferences(NotificationPreferences notificationPreferences) {
        this.notificationPreferences = notificationPreferences;
    }
    public void setLastLoginAt(Timestamp lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public void setLastUpdatedAt(Timestamp lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    @Override
    public String toString() {
        return "Teacher{" +
                "email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", employeeId='" + employeeId + '\'' +
                ", department='" + department + '\'' +
                '}';
    }
}