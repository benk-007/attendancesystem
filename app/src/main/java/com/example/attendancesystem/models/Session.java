package com.example.attendancesystem.models;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Session {
    private String sessionId;
    private String courseId;
    private String courseName;
    private String teacherEmail;
    private String teacherName;
    private Timestamp startTime;
    private Timestamp endTime;
    private String status; // "scheduled", "active", "completed", "cancelled"
    private String room;
    private boolean isManuallyManaged;

    // Field/Department info - NEW
    private String department; // "Informatique", "Mathématiques", etc.
    private String field; // "Data Science", "Software Engineering", "Cybersecurity", etc.
    private List<String> targetYears; // ["L1", "L2", "L3"] - years that attend this session

    // Weekly schedule info - NEW
    private String dayOfWeek; // "monday", "tuesday", etc.
    private String timeSlot; // "08:00-10:00", "14:00-16:00"
    private boolean isRecurring; // always true for fixed weekly schedule

    // Student lists
    private List<String> enrolledStudentEmails;
    private List<String> presentStudentEmails;
    private List<String> absentStudentEmails;

    // Statistics
    private SessionStatistics statistics;

    private Timestamp createdAt;
    private Timestamp lastUpdatedAt;

    // Constructeur vide requis pour Firebase
    public Session() {
        this.enrolledStudentEmails = new ArrayList<>();
        this.presentStudentEmails = new ArrayList<>();
        this.absentStudentEmails = new ArrayList<>();
        this.targetYears = new ArrayList<>();
        this.statistics = new SessionStatistics();
        this.isRecurring = true; // Default to recurring for fixed timetable
    }

    // Constructeur avec paramètres essentiels pour emploi du temps fixe
    public Session(String courseId, String courseName, String teacherEmail, String teacherName,
                   Timestamp startTime, Timestamp endTime, String room, String department,
                   String field, List<String> targetYears, String dayOfWeek, String timeSlot) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.teacherEmail = teacherEmail;
        this.teacherName = teacherName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.department = department;
        this.field = field;
        this.targetYears = targetYears != null ? new ArrayList<>(targetYears) : new ArrayList<>();
        this.dayOfWeek = dayOfWeek;
        this.timeSlot = timeSlot;
        this.status = "scheduled";
        this.isManuallyManaged = false;
        this.isRecurring = true;
        this.enrolledStudentEmails = new ArrayList<>();
        this.presentStudentEmails = new ArrayList<>();
        this.absentStudentEmails = new ArrayList<>();
        this.statistics = new SessionStatistics();
        this.createdAt = Timestamp.now();
        this.lastUpdatedAt = Timestamp.now();
    }

    // Classe interne pour les statistiques
    public static class SessionStatistics {
        private int totalEnrolled;
        private int totalPresent;
        private int totalAbsent;
        private double attendanceRate;

        public SessionStatistics() {
            this.totalEnrolled = 0;
            this.totalPresent = 0;
            this.totalAbsent = 0;
            this.attendanceRate = 0.0;
        }

        public SessionStatistics(int totalEnrolled, int totalPresent, int totalAbsent) {
            this.totalEnrolled = totalEnrolled;
            this.totalPresent = totalPresent;
            this.totalAbsent = totalAbsent;
            this.attendanceRate = totalEnrolled > 0 ? (double) totalPresent / totalEnrolled * 100 : 0.0;
        }

        // Méthode pour recalculer les statistiques
        public void recalculate(List<String> enrolled, List<String> present, List<String> absent) {
            this.totalEnrolled = enrolled.size();
            this.totalPresent = present.size();
            this.totalAbsent = absent.size();
            this.attendanceRate = totalEnrolled > 0 ? (double) totalPresent / totalEnrolled * 100 : 0.0;
        }

        // Méthode pour convertir en Map pour Firebase
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("totalEnrolled", totalEnrolled);
            map.put("totalPresent", totalPresent);
            map.put("totalAbsent", totalAbsent);
            map.put("attendanceRate", attendanceRate);
            return map;
        }

        // Getters et setters
        public int getTotalEnrolled() { return totalEnrolled; }
        public void setTotalEnrolled(int totalEnrolled) { this.totalEnrolled = totalEnrolled; }

        public int getTotalPresent() { return totalPresent; }
        public void setTotalPresent(int totalPresent) { this.totalPresent = totalPresent; }

        public int getTotalAbsent() { return totalAbsent; }
        public void setTotalAbsent(int totalAbsent) { this.totalAbsent = totalAbsent; }

        public double getAttendanceRate() { return attendanceRate; }
        public void setAttendanceRate(double attendanceRate) { this.attendanceRate = attendanceRate; }
    }

    // Méthodes utilitaires
    public boolean isActive() { return "active".equals(status); }
    public boolean isCompleted() { return "completed".equals(status); }
    public boolean isScheduled() { return "scheduled".equals(status); }
    public boolean isCancelled() { return "cancelled".equals(status); }

    // Check if student should be enrolled based on department, field, and year
    public boolean isStudentEligible(String studentDepartment, String studentField, String studentYear) {
        return this.department.equals(studentDepartment) &&
                this.field.equals(studentField) &&
                this.targetYears.contains(studentYear);
    }

    public void markStudentPresent(String studentEmail) {
        if (enrolledStudentEmails.contains(studentEmail)) {
            if (!presentStudentEmails.contains(studentEmail)) {
                presentStudentEmails.add(studentEmail);
            }
            absentStudentEmails.remove(studentEmail);
            updateStatistics();
        }
    }

    public void markStudentAbsent(String studentEmail) {
        if (enrolledStudentEmails.contains(studentEmail)) {
            if (!absentStudentEmails.contains(studentEmail)) {
                absentStudentEmails.add(studentEmail);
            }
            presentStudentEmails.remove(studentEmail);
            updateStatistics();
        }
    }

    public void enrollStudent(String studentEmail) {
        if (!enrolledStudentEmails.contains(studentEmail)) {
            enrolledStudentEmails.add(studentEmail);
            updateStatistics();
        }
    }

    public void removeStudent(String studentEmail) {
        enrolledStudentEmails.remove(studentEmail);
        presentStudentEmails.remove(studentEmail);
        absentStudentEmails.remove(studentEmail);
        updateStatistics();
    }

    private void updateStatistics() {
        if (statistics != null) {
            statistics.recalculate(enrolledStudentEmails, presentStudentEmails, absentStudentEmails);
        }
        this.lastUpdatedAt = Timestamp.now();
    }

    public void startSession() {
        this.status = "active";
        this.lastUpdatedAt = Timestamp.now();
    }

    public void endSession() {
        this.status = "completed";
        // Marquer les étudiants non présents comme absents
        for (String studentEmail : enrolledStudentEmails) {
            if (!presentStudentEmails.contains(studentEmail)) {
                markStudentAbsent(studentEmail);
            }
        }
        this.lastUpdatedAt = Timestamp.now();
    }

    public void cancelSession() {
        this.status = "cancelled";
        this.lastUpdatedAt = Timestamp.now();
    }

    // Méthode pour convertir en Map pour Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("sessionId", sessionId);
        map.put("courseId", courseId);
        map.put("courseName", courseName);
        map.put("teacherEmail", teacherEmail);
        map.put("teacherName", teacherName);
        map.put("startTime", startTime);
        map.put("endTime", endTime);
        map.put("status", status);
        map.put("room", room);
        map.put("isManuallyManaged", isManuallyManaged);
        map.put("department", department);
        map.put("field", field);
        map.put("targetYears", targetYears);
        map.put("dayOfWeek", dayOfWeek);
        map.put("timeSlot", timeSlot);
        map.put("isRecurring", isRecurring);
        map.put("enrolledStudentEmails", enrolledStudentEmails);
        map.put("presentStudentEmails", presentStudentEmails);
        map.put("absentStudentEmails", absentStudentEmails);
        if (statistics != null) {
            map.put("statistics", statistics.toMap());
        }
        map.put("createdAt", createdAt);
        map.put("lastUpdatedAt", lastUpdatedAt);
        return map;
    }

    // Getters - existing ones
    public String getSessionId() { return sessionId; }
    public String getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public String getTeacherEmail() { return teacherEmail; }
    public String getTeacherName() { return teacherName; }
    public Timestamp getStartTime() { return startTime; }
    public Timestamp getEndTime() { return endTime; }
    public String getStatus() { return status; }
    public String getRoom() { return room; }
    public boolean isManuallyManaged() { return isManuallyManaged; }
    public List<String> getEnrolledStudentEmails() { return enrolledStudentEmails; }
    public List<String> getPresentStudentEmails() { return presentStudentEmails; }
    public List<String> getAbsentStudentEmails() { return absentStudentEmails; }
    public SessionStatistics getStatistics() { return statistics; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getLastUpdatedAt() { return lastUpdatedAt; }

    // NEW Getters for field-based architecture
    public String getDepartment() { return department; }
    public String getField() { return field; }
    public List<String> getTargetYears() { return targetYears; }
    public String getDayOfWeek() { return dayOfWeek; }
    public String getTimeSlot() { return timeSlot; }
    public boolean isRecurring() { return isRecurring; }

    // Setters - existing ones
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public void setTeacherEmail(String teacherEmail) { this.teacherEmail = teacherEmail; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }
    public void setStatus(String status) { this.status = status; }
    public void setRoom(String room) { this.room = room; }
    public void setManuallyManaged(boolean manuallyManaged) { this.isManuallyManaged = manuallyManaged; }
    public void setEnrolledStudentEmails(List<String> enrolledStudentEmails) {
        this.enrolledStudentEmails = enrolledStudentEmails;
        updateStatistics();
    }
    public void setPresentStudentEmails(List<String> presentStudentEmails) {
        this.presentStudentEmails = presentStudentEmails;
        updateStatistics();
    }
    public void setAbsentStudentEmails(List<String> absentStudentEmails) {
        this.absentStudentEmails = absentStudentEmails;
        updateStatistics();
    }
    public void setStatistics(SessionStatistics statistics) { this.statistics = statistics; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setLastUpdatedAt(Timestamp lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    // NEW Setters for field-based architecture
    public void setDepartment(String department) { this.department = department; }
    public void setField(String field) { this.field = field; }
    public void setTargetYears(List<String> targetYears) { this.targetYears = targetYears; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
    public void setRecurring(boolean recurring) { this.isRecurring = recurring; }

    @Override
    public String toString() {
        return "Session{" +
                "sessionId='" + sessionId + '\'' +
                ", courseName='" + courseName + '\'' +
                ", teacherName='" + teacherName + '\'' +
                ", field='" + field + '\'' +
                ", department='" + department + '\'' +
                ", status='" + status + '\'' +
                ", room='" + room + '\'' +
                ", dayOfWeek='" + dayOfWeek + '\'' +
                ", timeSlot='" + timeSlot + '\'' +
                ", startTime=" + startTime +
                '}';
    }
}