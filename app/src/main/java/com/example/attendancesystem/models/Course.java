package com.example.attendancesystem.models;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Course {
    private String courseId;
    private String courseName;
    private String teacherEmail; // Email de l'enseignant (identifiant unique)
    private String teacherName;
    private String department;
    private String field; // Ajouté pour correspondre à Firestore
    private List<String> targetYears; // Ajouté pour correspondre à Firestore
    private boolean isActive;
    private Timestamp createdAt;

    // Planning du cours
    private Schedule courseScheduleEntry; // Correspond à 'courseScheduleEntry' dans Firestore

    // Statistiques du cours
    private CourseStatistics statistics;

    // Constructeur vide requis pour Firebase
    public Course() {
        this.targetYears = new ArrayList<>(); // Initialiser la liste
        this.courseScheduleEntry = new Schedule();
        this.statistics = new CourseStatistics();
    }

    // Constructeur avec paramètres essentiels
    public Course(String courseName, String teacherEmail, String teacherName, String department, String field, List<String> targetYears) {
        this.courseName = courseName;
        this.teacherEmail = teacherEmail;
        this.teacherName = teacherName;
        this.department = department;
        this.field = field; // Nouveau champ
        this.targetYears = targetYears != null ? targetYears : new ArrayList<>(); // Nouveau champ
        this.isActive = true;
        this.createdAt = Timestamp.now();
        this.courseScheduleEntry = new Schedule();
        this.statistics = new CourseStatistics();
    }

    // Classe interne pour le planning
    public static class Schedule {
        private String dayOfWeek; // "monday", "tuesday", etc.
        private String startTime; // "08:00"
        private String endTime; // "10:00"
        private String room; // "Amphi A", "Salle B101"
        private boolean isRecurring; // Cours récurrent ou ponctuel

        public Schedule() {
            this.isRecurring = true; // Par défaut récurrent
        }

        public Schedule(String dayOfWeek, String startTime, String endTime, String room, boolean isRecurring) {
            this.dayOfWeek = dayOfWeek;
            this.startTime = startTime;
            this.endTime = endTime;
            this.room = room;
            this.isRecurring = isRecurring;
        }

        // Méthode pour convertir en Map pour Firebase
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("dayOfWeek", dayOfWeek);
            map.put("startTime", startTime);
            map.put("endTime", endTime);
            map.put("room", room);
            map.put("isRecurring", isRecurring);
            return map;
        }

        // Getters et setters
        public String getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }

        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }

        public String getRoom() { return room; }
        public void setRoom(String room) { this.room = room; }

        public boolean isRecurring() { return isRecurring; }
        public void setRecurring(boolean recurring) { this.isRecurring = recurring; }
    }

    // Classe interne pour les statistiques
    public static class CourseStatistics {
        private int totalSessions;
        private double averageAttendanceRate;
        private int totalEnrolledStudents;

        public CourseStatistics() {
            this.totalSessions = 0;
            this.averageAttendanceRate = 0.0;
            this.totalEnrolledStudents = 0;
        }

        // Méthode pour convertir en Map pour Firebase
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("totalSessions", totalSessions);
            map.put("averageAttendanceRate", averageAttendanceRate);
            map.put("totalEnrolledStudents", totalEnrolledStudents);
            return map;
        }

        // Getters et setters
        public int getTotalSessions() { return totalSessions; }
        public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }

        public double getAverageAttendanceRate() { return averageAttendanceRate; }
        public void setAverageAttendanceRate(double averageAttendanceRate) { this.averageAttendanceRate = averageAttendanceRate; }

        public int getTotalEnrolledStudents() { return totalEnrolledStudents; }
        public void setTotalEnrolledStudents(int totalEnrolledStudents) { this.totalEnrolledStudents = totalEnrolledStudents; }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("courseId", courseId);
        map.put("courseName", courseName);
        map.put("teacherEmail", teacherEmail);
        map.put("teacherName", teacherName);
        map.put("department", department);
        map.put("field", field); // Ajouté
        map.put("targetYears", targetYears); // Ajouté
        map.put("isActive", isActive);
        map.put("createdAt", createdAt);
        if (courseScheduleEntry != null) {
            map.put("courseScheduleEntry", courseScheduleEntry.toMap()); // Renommé ici
        }
        if (statistics != null) {
            map.put("statistics", statistics.toMap());
        }
        return map;
    }
    // Getters
    public String getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public String getTeacherEmail() { return teacherEmail; }
    public String getTeacherName() { return teacherName; }
    public String getDepartment() { return department; }
    public boolean isActive() { return isActive; }
    public Timestamp getCreatedAt() { return createdAt; }
    public CourseStatistics getStatistics() { return statistics; }
    public String getField() { return field; }
    public List<String> getTargetYears() { return targetYears; }
    public Schedule getCourseScheduleEntry() { return courseScheduleEntry; } // Renommé le getter

    // Setters (ajustés)
    public void setField(String field) { this.field = field; }
    public void setTargetYears(List<String> targetYears) { this.targetYears = targetYears; }
    public void setCourseScheduleEntry(Schedule courseScheduleEntry) { this.courseScheduleEntry = courseScheduleEntry; } // Renommé le setter
    // Setters
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public void setTeacherEmail(String teacherEmail) { this.teacherEmail = teacherEmail; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public void setDepartment(String department) { this.department = department; }
    public void setActive(boolean active) { this.isActive = active; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setStatistics(CourseStatistics statistics) { this.statistics = statistics; }

    @Override
    public String toString() {
        return "Course{" +
                "courseId='" + courseId + '\'' +
                ", courseName='" + courseName + '\'' +
                ", teacherEmail='" + teacherEmail + '\'' +
                ", department='" + department + '\'' +
                '}';
    }
}