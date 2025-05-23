package com.example.attendancesystem.models;

import java.util.HashMap;
import java.util.Map;

public class NotificationPreferences {
    private boolean attendanceAlerts; // Notification lors pointage réussi
    private boolean absenceThresholdAlerts; // Alerte seuil d'absences
    private boolean courseReminders; // Rappel des cours à venir
    private boolean justificationUpdates; // Notifications traitement justificatifs
    private String preferredTime; // Heure préférée pour les rappels

    // Constructeur avec valeurs par défaut
    public NotificationPreferences() {
        this.attendanceAlerts = true;
        this.absenceThresholdAlerts = true;
        this.courseReminders = true;
        this.justificationUpdates = true;
        this.preferredTime = "08:00";
    }

    // Constructeur complet
    public NotificationPreferences(boolean attendanceAlerts, boolean absenceThresholdAlerts,
                                   boolean courseReminders, boolean justificationUpdates,
                                   String preferredTime) {
        this.attendanceAlerts = attendanceAlerts;
        this.absenceThresholdAlerts = absenceThresholdAlerts;
        this.courseReminders = courseReminders;
        this.justificationUpdates = justificationUpdates;
        this.preferredTime = preferredTime;
    }

    // Méthode pour convertir en Map pour Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("attendanceAlerts", attendanceAlerts);
        map.put("absenceThresholdAlerts", absenceThresholdAlerts);
        map.put("courseReminders", courseReminders);
        map.put("justificationUpdates", justificationUpdates);
        map.put("preferredTime", preferredTime);
        return map;
    }

    // Getters
    public boolean isAttendanceAlerts() { return attendanceAlerts; }
    public boolean isAbsenceThresholdAlerts() { return absenceThresholdAlerts; }
    public boolean isCourseReminders() { return courseReminders; }
    public boolean isJustificationUpdates() { return justificationUpdates; }
    public String getPreferredTime() { return preferredTime; }

    // Setters
    public void setAttendanceAlerts(boolean attendanceAlerts) { this.attendanceAlerts = attendanceAlerts; }
    public void setAbsenceThresholdAlerts(boolean absenceThresholdAlerts) { this.absenceThresholdAlerts = absenceThresholdAlerts; }
    public void setCourseReminders(boolean courseReminders) { this.courseReminders = courseReminders; }
    public void setJustificationUpdates(boolean justificationUpdates) { this.justificationUpdates = justificationUpdates; }
    public void setPreferredTime(String preferredTime) { this.preferredTime = preferredTime; }

    @Override
    public String toString() {
        return "NotificationPreferences{" +
                "attendanceAlerts=" + attendanceAlerts +
                ", absenceThresholdAlerts=" + absenceThresholdAlerts +
                ", courseReminders=" + courseReminders +
                ", justificationUpdates=" + justificationUpdates +
                ", preferredTime='" + preferredTime + '\'' +
                '}';
    }
}