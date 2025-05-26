package com.example.attendancesystem.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.attendancesystem.R;
import com.example.attendancesystem.activities.StudentDashboardActivity;

/**
 * Helper class pour gérer les notifications locales
 * Alternative simple à Firebase Cloud Messaging
 */
public class NotificationHelper {

    private static final String CHANNEL_ID = "attendance_notifications";
    private static final String CHANNEL_NAME = "Notifications de Présence";
    private static final String CHANNEL_DESCRIPTION = "Notifications pour les présences et rappels de cours";

    public static final int NOTIFICATION_ID_ATTENDANCE = 1001;
    public static final int NOTIFICATION_ID_COURSE_REMINDER = 1002;
    public static final int NOTIFICATION_ID_JUSTIFICATION = 1003;
    public static final int NOTIFICATION_ID_THRESHOLD = 1004;

    private Context context;
    private NotificationManagerCompat notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
        createNotificationChannel();
    }

    /**
     * Créer le canal de notification (requis pour Android 8.0+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Notification de présence réussie
     */
    public void showAttendanceSuccess(String courseName, String time) {
        String title = "Présence enregistrée ✅";
        String message = String.format("Votre présence au cours de %s a été enregistrée à %s",
                courseName, time);

        showNotification(
                NOTIFICATION_ID_ATTENDANCE,
                title,
                message,
                R.drawable.ic_person,
                createDashboardIntent()
        );
    }

    /**
     * Rappel de cours à venir
     */
    public void showCourseReminder(String courseName, String timeRemaining, String room) {
        String title = "Cours dans " + timeRemaining + " 📚";
        String message = String.format("%s - %s", courseName, room);

        showNotification(
                NOTIFICATION_ID_COURSE_REMINDER,
                title,
                message,
                R.drawable.ic_badge,
                createDashboardIntent()
        );
    }

    /**
     * Notification de justification traitée
     */
    public void showJustificationUpdate(String courseName, String status) {
        String title = "Justification " + getStatusText(status);
        String message = String.format("Votre justification pour %s a été %s",
                courseName, getStatusText(status).toLowerCase());

        int icon = status.equals("approved") ? R.drawable.ic_badge : R.drawable.ic_email;

        showNotification(
                NOTIFICATION_ID_JUSTIFICATION,
                title,
                message,
                icon,
                createDashboardIntent()
        );
    }

    /**
     * Alerte seuil d'absences
     */
    public void showAbsenceThresholdAlert(int absentCount, int threshold, String courseName) {
        String title = "⚠️ Seuil d'absences atteint";
        String message = String.format("Vous avez %d absences sur %d autorisées en %s",
                absentCount, threshold, courseName);

        showNotification(
                NOTIFICATION_ID_THRESHOLD,
                title,
                message,
                R.drawable.ic_email,
                createDashboardIntent()
        );
    }

    /**
     * Méthode générique pour afficher une notification
     */
    private void showNotification(int notificationId, String title, String message,
                                  int iconResId, PendingIntent pendingIntent) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(iconResId)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(context.getResources().getColor(R.color.primary_color));

        try {
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
            // Permission de notification non accordée
            Utils.logError("NotificationHelper", "Permission denied for notification: " + e.getMessage());
        }
    }

    /**
     * Créer un PendingIntent vers le dashboard
     */
    private PendingIntent createDashboardIntent() {
        Intent intent = new Intent(context, StudentDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /**
     * Convertir le statut en texte lisible
     */
    private String getStatusText(String status) {
        switch (status) {
            case "approved": return "approuvée";
            case "rejected": return "rejetée";
            case "under_review": return "en cours d'examen";
            default: return status;
        }
    }

    /**
     * Vérifier si les notifications sont activées
     */
    public boolean areNotificationsEnabled() {
        return notificationManager.areNotificationsEnabled();
    }

    /**
     * Annuler une notification spécifique
     */
    public void cancelNotification(int notificationId) {
        notificationManager.cancel(notificationId);
    }

    /**
     * Annuler toutes les notifications
     */
    public void cancelAllNotifications() {
        notificationManager.cancelAll();
    }

    /**
     * Planifier une notification de rappel de cours
     * (nécessite WorkManager pour une implémentation complète)
     */
    public void scheduleReminder(String courseName, long timeInMillis, String room) {
        // TODO: Implémenter avec WorkManager pour les rappels programmés
        // WorkManager.getInstance(context).enqueue(reminderWork);
    }
}