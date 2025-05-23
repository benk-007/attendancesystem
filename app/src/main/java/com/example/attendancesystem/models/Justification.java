package com.example.attendancesystem.models;

import com.google.firebase.Timestamp;
import java.util.Date; // Import Date for justificationDate
import java.util.HashMap;
import java.util.Map;

public class Justification {
    private String justificationId;
    private String studentEmail;
    private String studentName;
    private String studentId;
    private String courseId;
    private String courseName;
    // Removed sessionId and attendanceId as per the new requirement
    private Date justificationDate; // NEW: Date of the absence being justified

    private String reason;
    private String description;

    private String status; // "submitted", "under_review", "approved", "rejected"
    private Timestamp submittedAt;

    // Review fields (only for admin)
    private String reviewedBy; // Email of reviewer
    private Timestamp reviewedAt;
    private String reviewComments;
    private String approvalReason; // Reason for approval or rejection

    private Timestamp createdAt;
    private Timestamp lastUpdatedAt;

    // Default constructor for Firebase
    public Justification() {
        this.status = "submitted";
        this.createdAt = Timestamp.now();
        this.lastUpdatedAt = Timestamp.now();
        this.submittedAt = Timestamp.now();
    }

    // Constructor for new student justification (no session/attendance ID, with date)
    public Justification(String studentEmail, String studentName, String studentId,
                         String courseId, String courseName, Date justificationDate,
                         String reason, String description) {
        this.studentEmail = studentEmail;
        this.studentName = studentName;
        this.studentId = studentId;
        this.courseId = courseId;
        this.courseName = courseName;
        this.justificationDate = justificationDate; // Set the justification date
        this.reason = reason;
        this.description = description;
        this.status = "submitted"; // Default status when created by student
        this.createdAt = Timestamp.now();
        this.lastUpdatedAt = Timestamp.now();
        this.submittedAt = Timestamp.now();
    }

    // Review methods (used by admin)
    public void approve(String reviewerEmail, String comments, String approvalReason) {
        this.status = "approved";
        this.reviewedBy = reviewerEmail;
        this.reviewedAt = Timestamp.now();
        this.reviewComments = comments;
        this.approvalReason = approvalReason;
        this.lastUpdatedAt = Timestamp.now();
    }

    public void reject(String reviewerEmail, String comments, String reason) {
        this.status = "rejected";
        this.reviewedBy = reviewerEmail;
        this.reviewedAt = Timestamp.now();
        this.reviewComments = comments;
        this.approvalReason = reason;
        this.lastUpdatedAt = Timestamp.now();
    }

    public void setUnderReview(String reviewerEmail) {
        this.status = "under_review";
        this.reviewedBy = reviewerEmail;
        this.lastUpdatedAt = Timestamp.now();
    }

    // Status check methods
    public boolean isSubmitted() { return "submitted".equals(status); }
    public boolean isUnderReview() { return "under_review".equals(status); }
    public boolean isApproved() { return "approved".equals(status); }
    public boolean isRejected() { return "rejected".equals(status); }

    // Convert to Map for Firebase (Firestore automatically handles Date to Timestamp conversion)
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("justificationId", justificationId);
        map.put("studentEmail", studentEmail);
        map.put("studentName", studentName);
        map.put("studentId", studentId);
        map.put("courseId", courseId);
        map.put("courseName", courseName);
        map.put("justificationDate", justificationDate); // Include the new field
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
    public Date getJustificationDate() { return justificationDate; } // New getter
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
    public void setJustificationDate(Date justificationDate) { this.justificationDate = justificationDate; } // New setter
    public void setReason(String reason) { this.reason = reason; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(String status) { this.status = status; }
    public void voidsetSubmittedAt(Timestamp submittedAt) { this.submittedAt = submittedAt; } // Fix: Removed extra 'void'
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
                ", studentName='" + studentName + '\'' +
                ", courseName='" + courseName + '\'' +
                ", justificationDate=" + justificationDate + // Include in toString
                ", reason='" + reason + '\'' +
                ", status='" + status + '\'' +
                ", submittedAt=" + submittedAt +
                '}';
    }
}