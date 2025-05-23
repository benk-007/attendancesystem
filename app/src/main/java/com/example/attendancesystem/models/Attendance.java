package com.example.attendancesystem.models;

import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class Attendance {
    private String attendanceId;
    private String studentEmail; // Email de l'étudiant (identifiant unique)
    private String studentName;
    private String studentId;
    private String courseId;
    private String courseName;
    private String sessionId;
    private Timestamp timestamp;
    private String status; // "present", "absent", "justified"
    private double confidence;
    private boolean isManualEntry;
    private String modifiedBy; // Email de celui qui a modifié
    private String modificationReason;
    private AttendanceDetails attendanceDetails;
    private Timestamp createdAt;
    private Timestamp lastModifiedAt;

    // Constructeur vide requis pour Firebase
    public Attendance() {
        this.attendanceDetails = new AttendanceDetails();
    }

    // Constructeur pour pointage automatique
    public Attendance(String studentEmail, String studentName, String studentId,
                      String courseId, String courseName, double confidence) {
        this.studentEmail = studentEmail;
        this.studentName = studentName;
        this.studentId = studentId;
        this.courseId = courseId;
        this.courseName = courseName;
        this.confidence = confidence;
        this.timestamp = Timestamp.now();
        this.status = "present";
        this.isManualEntry = false;
        this.attendanceDetails = new AttendanceDetails();
        this.createdAt = Timestamp.now();
    }

    // Constructeur pour entrée manuelle
    public Attendance(String studentEmail, String studentName, String studentId,
                      String courseId, String courseName, String status, String modifiedBy) {
        this.studentEmail = studentEmail;
        this.studentName = studentName;
        this.studentId = studentId;
        this.courseId = courseId;
        this.courseName = courseName;
        this.status = status;
        this.modifiedBy = modifiedBy;
        this.timestamp = Timestamp.now();
        this.isManualEntry = true;
        this.confidence = 1.0; // Confiance maximale pour entrée manuelle
        this.attendanceDetails = new AttendanceDetails();
        this.createdAt = Timestamp.now();
    }

    // Classe interne pour les détails du pointage
    public static class AttendanceDetails {
        private Timestamp captureTime;
        private long processingTime; // en millisecondes
        private int retryCount;
        private String location; // Localisation fixe du terminal unique

        public AttendanceDetails() {
            this.captureTime = Timestamp.now();
            this.retryCount = 0;
            this.location = "Terminal Principal"; // Nom fixe du terminal unique
        }

        // Méthode pour convertir en Map pour Firebase
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("captureTime", captureTime);
            map.put("processingTime", processingTime);
            map.put("retryCount", retryCount);
            map.put("location", location);
            return map;
        }

        // Getters et setters
        public Timestamp getCaptureTime() { return captureTime; }
        public void setCaptureTime(Timestamp captureTime) { this.captureTime = captureTime; }

        public long getProcessingTime() { return processingTime; }
        public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }

        public int getRetryCount() { return retryCount; }
        public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
    }

    // Méthodes utilitaires
    public boolean isPresent() { return "present".equals(status); }
    public boolean isAbsent() { return "absent".equals(status); }
    public boolean isJustified() { return "justified".equals(status); }

    public String getStatusDisplayName() {
        switch (status) {
            case "present": return "Présent";
            case "absent": return "Absent";
            case "justified": return "Justifié";
            default: return status;
        }
    }

    // Méthode pour convertir en Map pour Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("attendanceId", attendanceId);
        map.put("studentEmail", studentEmail);
        map.put("studentName", studentName);
        map.put("studentId", studentId);
        map.put("courseId", courseId);
        map.put("courseName", courseName);
        map.put("sessionId", sessionId);
        map.put("timestamp", timestamp);
        map.put("status", status);
        map.put("confidence", confidence);
        map.put("isManualEntry", isManualEntry);
        map.put("modifiedBy", modifiedBy);
        map.put("modificationReason", modificationReason);
        if (attendanceDetails != null) {
            map.put("attendanceDetails", attendanceDetails.toMap());
        }
        map.put("createdAt", createdAt);
        map.put("lastModifiedAt", lastModifiedAt);
        return map;
    }

    // Getters
    public String getAttendanceId() { return attendanceId; }
    public String getStudentEmail() { return studentEmail; }
    public String getStudentName() { return studentName; }
    public String getStudentId() { return studentId; }
    public String getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public String getSessionId() { return sessionId; }
    public Timestamp getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
    public double getConfidence() { return confidence; }
    public boolean isManualEntry() { return isManualEntry; }
    public String getModifiedBy() { return modifiedBy; }
    public String getModificationReason() { return modificationReason; }
    public AttendanceDetails getAttendanceDetails() { return attendanceDetails; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getLastModifiedAt() { return lastModifiedAt; }

    // Setters
    public void setAttendanceId(String attendanceId) { this.attendanceId = attendanceId; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public void setStatus(String status) { this.status = status; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public void setManualEntry(boolean manualEntry) { this.isManualEntry = manualEntry; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }
    public void setModificationReason(String modificationReason) { this.modificationReason = modificationReason; }
    public void setAttendanceDetails(AttendanceDetails attendanceDetails) { this.attendanceDetails = attendanceDetails; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setLastModifiedAt(Timestamp lastModifiedAt) { this.lastModifiedAt = lastModifiedAt; }

    @Override
    public String toString() {
        return "Attendance{" +
                "studentEmail='" + studentEmail + '\'' +
                ", courseName='" + courseName + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}