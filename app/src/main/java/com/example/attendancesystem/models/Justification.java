package com.example.attendancesystem.models;

import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class Justification {
    private String justificationId;
    private String studentEmail; // Email de l'étudiant (identifiant unique)
    private String studentName;
    private String studentId;
    private String courseId;
    private String courseName;
    private String sessionId;
    private String attendanceId;

    private String reason; // Motif sélectionné
    private String description; // Description détaillée du formulaire

    private String status; // "submitted", "under_review", "approved", "rejected"
    private Timestamp submittedAt;

    // Traitement par l'enseignant/admin
    private String reviewedBy; // Email du revieweur
    private Timestamp reviewedAt;
    private String reviewComments;
    private String approvalReason;

    private Timestamp createdAt;
    private Timestamp lastUpdatedAt;

    // Constructeur vide requis pour Firebase
    public Justification() {}

    // Constructeur pour nouvelle justification
    public Justification(String studentEmail, String studentName, String studentId,
                         String courseId, String courseName, String reason, String description) {
        this.studentEmail = studentEmail;
        this.studentName = studentName;
        this.studentId = studentId;
        this.courseId = courseId;
        this.courseName = courseName;
        this.reason = reason;
        this.description = description;
        this.status = "submitted";
        this.submittedAt = Timestamp.now();
        this.createdAt = Timestamp.now();
        this.lastUpdatedAt = Timestamp.now();
    }

    // Constructeur pour justification liée à une présence spécifique
    public Justification(String studentEmail, String studentName, String studentId,
                         String courseId, String courseName, String sessionId, String attendanceId,
                         String reason, String description) {
        this(studentEmail, studentName, studentId, courseId, courseName, reason, description);
        this.sessionId = sessionId;
        this.attendanceId = attendanceId;
    }

    // Méthodes utilitaires pour le statut
    public boolean isSubmitted() { return "submitted".equals(status); }
    public boolean isUnderReview() { return "under_review".equals(status); }
    public boolean isApproved() { return "approved".equals(status); }
    public boolean isRejected() { return "rejected".equals(status); }

    public String getStatusDisplayName() {
        switch (status) {
            case "submitted": return "Soumise";
            case "under_review": return "En cours d'examen";
            case "approved": return "Approuvée";
            case "rejected": return "Refusée";
            default: return status;
        }
    }

    // Méthodes pour gérer le processus de révision
    public void startReview(String reviewerEmail) {
        this.status = "under_review";
        this.reviewedBy = reviewerEmail;
        this.reviewedAt = Timestamp.now();
        this.lastUpdatedAt = Timestamp.now();
    }

    public void approve(String reviewerEmail, String approvalReason) {
        this.status = "approved";
        this.reviewedBy = reviewerEmail;
        this.reviewedAt = Timestamp.now();
        this.approvalReason = approvalReason;
        this.lastUpdatedAt = Timestamp.now();
    }

    public void reject(String reviewerEmail, String rejectionReason) {
        this.status = "rejected";
        this.reviewedBy = reviewerEmail;
        this.reviewedAt = Timestamp.now();
        this.reviewComments = rejectionReason;
        this.lastUpdatedAt = Timestamp.now();
    }

    // Méthode pour convertir en Map pour Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("justificationId", justificationId);
        map.put("studentEmail", studentEmail);
        map.put("studentName", studentName);
        map.put("studentId", studentId);
        map.put("courseId", courseId);
        map.put("courseName", courseName);
        map.put("sessionId", sessionId);
        map.put("attendanceId", attendanceId);
        map.put("reason", reason);
        map.put("description", description);
        map.put("status", status);
        map.put("submittedAt", submittedAt);
        map.put("reviewedBy", reviewedBy);
        map.put("reviewedAt", reviewedAt);
        map.put("reviewComments", reviewComments);
        map.put("approvalReason", approvalReason);
        map.put("createdAt", createdAt);
        map.put("lastUpdatedAt", lastUpdatedAt);
        return map;
    }

    // Getters
    public String getJustificationId() { return justificationId; }
    public String getStudentEmail() { return studentEmail; }
    public String getStudentName() { return studentName; }
    public String getStudentId() { return studentId; }
    public String getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public String getSessionId() { return sessionId; }
    public String getAttendanceId() { return attendanceId; }
    public String getReason() { return reason; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public Timestamp getSubmittedAt() { return submittedAt; }
    public String getReviewedBy() { return reviewedBy; }
    public Timestamp getReviewedAt() { return reviewedAt; }
    public String getReviewComments() { return reviewComments; }
    public String getApprovalReason() { return approvalReason; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getLastUpdatedAt() { return lastUpdatedAt; }

    // Setters
    public void setJustificationId(String justificationId) { this.justificationId = justificationId; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setAttendanceId(String attendanceId) { this.attendanceId = attendanceId; }
    public void setReason(String reason) { this.reason = reason; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(String status) { this.status = status; }
    public void setSubmittedAt(Timestamp submittedAt) { this.submittedAt = submittedAt; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public void setReviewedAt(Timestamp reviewedAt) { this.reviewedAt = reviewedAt; }
    public void setReviewComments(String reviewComments) { this.reviewComments = reviewComments; }
    public void setApprovalReason(String approvalReason) { this.approvalReason = approvalReason; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setLastUpdatedAt(Timestamp lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    @Override
    public String toString() {
        return "Justification{" +
                "justificationId='" + justificationId + '\'' +
                ", studentEmail='" + studentEmail + '\'' +
                ", courseName='" + courseName + '\'' +
                ", reason='" + reason + '\'' +
                ", status='" + status + '\'' +
                ", submittedAt=" + submittedAt +
                '}';
    }
}