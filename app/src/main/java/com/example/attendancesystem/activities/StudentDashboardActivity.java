package com.example.attendancesystem.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Session;
import com.example.attendancesystem.models.Student;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.NotificationHelper;
import com.example.attendancesystem.utils.Utils;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class StudentDashboardActivity extends AppCompatActivity {

    private static final String TAG = "StudentDashboard";

    // Views
    private TextView tvWelcome, tvTodayStatus, tvAttendanceRate, tvNextCourse;
    private CardView cardProfile, cardHistory, cardJustification, cardSchedule, cardStats, cardCourses;
    private View btnViewPhoto;
    private ProgressBar progressBarMain;
    private CircleImageView ivProfileImage;

    // Data
    private FirebaseManager firebaseManager;
    private Student currentStudent;
    private boolean isDataLoading = false;
    private NotificationHelper notificationHelper;

    // Constants
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        // Configure the toolbar properly
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Dashboard Étudiant");
            }
        } else {
            // Fallback to default action bar if toolbar is not found
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Dashboard Étudiant");
            }
        }

        // Initialize Firebase
        firebaseManager = FirebaseManager.getInstance();

        // Initialize notification helper
        notificationHelper = new NotificationHelper(this);

        // Initialize views
        initViews();

        // Check notification permission
        checkNotificationPermission();

        // Load user data
        loadUserData();

        // Configure listeners
        setupListeners();
    }

    private void checkNotificationPermission() {
        if (!Utils.hasNotificationPermission(this)) {
            Utils.requestNotificationPermission(this, NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Utils.showToast(this, "Notifications activées ✅");
                testNotification();
            } else {
                Utils.showToast(this, "Notifications désactivées. Activez-les dans les paramètres.");
            }
        }
    }

    private void testNotification() {
        if (currentStudent != null && notificationHelper.areNotificationsEnabled()) {
            // Notification de bienvenue
            notificationHelper.showAttendanceSuccess(
                    "Test",
                    Utils.getCurrentTime()
            );
        }
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tv_welcome);
        tvTodayStatus = findViewById(R.id.tv_today_status);
        tvAttendanceRate = findViewById(R.id.tv_attendance_rate);
        tvNextCourse = findViewById(R.id.tv_next_course);

        cardProfile = findViewById(R.id.card_profile);
        cardHistory = findViewById(R.id.card_history);
        cardJustification = findViewById(R.id.card_justification);
        cardSchedule = findViewById(R.id.card_schedule);
        cardStats = findViewById(R.id.card_stats); // <-- Add this, replace with actual ID
        cardCourses = findViewById(R.id.card_courses); // <-- Add this, replace with actual ID
        btnViewPhoto = findViewById(R.id.btn_view_photo); // <-- Add this, replace with actual ID (e.g., R.id.iv_profile_image if it's the same)


        progressBarMain = findViewById(R.id.progress_bar_main);
        ivProfileImage = findViewById(R.id.iv_profile_image);

        // Initialiser avec état de chargement
        showMainLoading(true);
    }

    private void loadUserData() {
        String userEmail = Utils.getSavedUserEmail(this);
        if (userEmail != null) {
            Log.d(TAG, "Loading student data for: " + userEmail);

            showMainLoading(true);

            firebaseManager.getStudentByEmail(userEmail, new FirebaseManager.DataCallback<Student>() {
                @Override
                public void onSuccess(Student student) {
                    currentStudent = student;
                    Log.d(TAG, "Student loaded: " + student.getFullName() +
                            " - Field: " + student.getField() +
                            " - Year: " + student.getYear() +
                            " - Department: " + student.getDepartment());
                    updateUI();
                    loadTodayAttendance();
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Error loading student data: " + error);

                    showMainLoading(false);
                    tvWelcome.setText("Erreur de chargement");
                    tvTodayStatus.setText("Impossible de charger les données");
                    tvAttendanceRate.setText("--");
                    tvNextCourse.setText("Non disponible");

                    Utils.showToast(StudentDashboardActivity.this,
                            "Erreur de chargement: " + error + "\nVeuillez réessayer.");
                }
            });
        } else {
            Log.e(TAG, "No saved user email found");
            showMainLoading(false);
            Utils.showToast(this, "Session expirée. Veuillez vous reconnecter.");

            // Rediriger vers login
            Intent loginIntent = new Intent(this, LoginActivity.class);
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            finish();
        }
    }

    private void updateUI() {
        if (currentStudent != null) {
            String firstName = Utils.getFirstName(currentStudent.getFullName());
            String welcomeText = "Bonjour, " + firstName + " !";
            tvWelcome.setText(welcomeText);

            loadProfileImage();

            Log.d(TAG, "UI updated for student: " + firstName);
            showMainLoading(false);
        }
    }

    private void loadProfileImage() {
        if (currentStudent != null && currentStudent.getProfileImageUrl() != null
                && !currentStudent.getProfileImageUrl().isEmpty()) {

            Glide.with(this)
                    .load(currentStudent.getProfileImageUrl())
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(ivProfileImage);
        } else {
            // Image par défaut
            ivProfileImage.setImageResource(R.drawable.ic_person);
        }
    }

    private void showMainLoading(boolean show) {
        isDataLoading = show;

        if (show) {
            progressBarMain.setVisibility(View.VISIBLE);
            // Désactiver les cartes pendant le chargement
            cardProfile.setEnabled(false);
            cardHistory.setEnabled(false);
            cardJustification.setEnabled(false);
            cardSchedule.setEnabled(false);
        } else {
            progressBarMain.setVisibility(View.GONE);
            // Réactiver les cartes
            cardProfile.setEnabled(true);
            cardHistory.setEnabled(true);
            cardJustification.setEnabled(true);
            cardSchedule.setEnabled(true);
        }
    }

    private void loadTodayAttendance() {
        if (currentStudent == null) {
            Log.w(TAG, "Cannot load today attendance - currentStudent is null");
            return;
        }

        Log.d(TAG, "Loading today's sessions for: " +
                "Department=" + currentStudent.getDepartment() +
                ", Field=" + currentStudent.getField() +
                ", Year=" + currentStudent.getYear());

        // Load today's sessions for this student's department, field and year
        firebaseManager.getTodaySessionsForStudent(
                currentStudent.getEmail(),
                currentStudent.getDepartment(),
                currentStudent.getField(),
                currentStudent.getYear(),
                new FirebaseManager.DataCallback<List<Session>>() {
                    @Override
                    public void onSuccess(List<Session> sessions) {
                        Log.d(TAG, "Today's sessions loaded successfully: " + sessions.size() + " sessions found");
                        updateTodayStatus(sessions);
                        loadNextSession();
                        loadAttendanceStatistics();
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error loading today's sessions: " + error);
                        Utils.showToast(StudentDashboardActivity.this, "Erreur lors du chargement des sessions: " + error);
                        tvTodayStatus.setText("Erreur de chargement");
                        tvNextCourse.setText("Données non disponibles");
                    }
                });
    }

    private void updateTodayStatus(List<Session> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            tvTodayStatus.setText("Aucun cours aujourd'hui");
            Log.d(TAG, "No sessions today");
            return;
        }

        int totalSessions = sessions.size();
        int attendedSessions = 0;
        int activeSessions = 0;

        for (Session session : sessions) {
            Log.d(TAG, "Processing session: " + session.getCourseName() + " - Status: " + session.getStatus());

            if (session.isActive()) {
                activeSessions++;
            } else if (session.isCompleted() &&
                    session.getPresentStudentEmails() != null &&
                    session.getPresentStudentEmails().contains(currentStudent.getEmail())) {
                attendedSessions++;

                // Notification de présence réussie (si pas déjà notifié)
                if (shouldNotifyAttendance(session)) {
                    notificationHelper.showAttendanceSuccess(
                            session.getCourseName(),
                            Utils.formatTime(session.getEndTime())
                    );
                }
            }
        }

        String statusText;
        if (activeSessions > 0) {
            statusText = "Session en cours - " + totalSessions + " cours aujourd'hui";
        } else if (attendedSessions == totalSessions && totalSessions > 0) {
            statusText = "Tous les cours assistés aujourd'hui (" + totalSessions + "/" + totalSessions + ")";
        } else {
            statusText = attendedSessions + "/" + totalSessions + " cours assistés aujourd'hui";
        }

        tvTodayStatus.setText(statusText);
        Log.d(TAG, "Today status updated: " + statusText);
    }

    private boolean shouldNotifyAttendance(Session session) {
        // Vérifier si on a déjà notifié pour cette session
        String key = "notified_" + session.getSessionId();
        return !Utils.getBooleanPref(this, key, false);
    }

    private void loadNextSession() {
        if (currentStudent == null) {
            Log.w(TAG, "Cannot load next session - currentStudent is null");
            tvNextCourse.setText("Non disponible");
            return;
        }

        Log.d(TAG, "Loading next session for student");

        // Load next upcoming session for this student
        firebaseManager.getNextSessionForStudent(
                currentStudent.getEmail(),
                currentStudent.getDepartment(),
                currentStudent.getField() != null ? currentStudent.getField() : "",
                currentStudent.getYear(),
                new FirebaseManager.DataCallback<Session>() {
                    @Override
                    public void onSuccess(Session session) {
                        if (session != null) {
                            Log.d(TAG, "Next session found: " + session.getCourseName() +
                                    " at " + session.getStartTime() +
                                    " in " + session.getRoom());

                            String timeStr = Utils.formatTime(session.getStartTime());
                            String sessionText = session.getCourseName() + " - " + timeStr;
                            tvNextCourse.setText(sessionText);

                            // Programmer un rappel si le cours est dans moins de 1 heure
                            scheduleReminderIfNeeded(session);
                        } else {
                            Log.d(TAG, "No next session found");
                            tvNextCourse.setText("Aucun cours programmé");
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error loading next session: " + error);
                        tvNextCourse.setText("Erreur de chargement");
                    }
                });
    }

    private void scheduleReminderIfNeeded(Session session) {
        if (session.getStartTime() == null) return;

        long sessionTimeMs = session.getStartTime().toDate().getTime();
        long currentTimeMs = System.currentTimeMillis();
        long timeDiffMs = sessionTimeMs - currentTimeMs;

        // Si le cours est dans moins d'1 heure et plus de 5 minutes
        long oneHourMs = 60 * 60 * 1000;
        long fiveMinutesMs = 5 * 60 * 1000;

        if (timeDiffMs > fiveMinutesMs && timeDiffMs <= oneHourMs) {
            long minutesRemaining = timeDiffMs / (60 * 1000);
            String timeRemaining = minutesRemaining + " min";

            // Vérifier les préférences de l'utilisateur
            if (currentStudent.getNotificationPreferences() != null &&
                    currentStudent.getNotificationPreferences().isCourseReminders()) {

                notificationHelper.showCourseReminder(
                        session.getCourseName(),
                        timeRemaining,
                        session.getRoom()
                );
            }
        }
    }

    private void loadAttendanceStatistics() {
        if (currentStudent == null) {
            Log.w(TAG, "Cannot load attendance statistics - currentStudent is null");
            tvAttendanceRate.setText("N/A");
            return;
        }

        Log.d(TAG, "Loading attendance statistics for student");

        // Load attendance statistics for this student
        firebaseManager.getStudentAttendanceStatistics(
                currentStudent.getEmail(),
                currentStudent.getDepartment(),
                currentStudent.getField() != null ? currentStudent.getField() : "",
                currentStudent.getYear(),
                new FirebaseManager.DataCallback<FirebaseManager.AttendanceStatsDetailed>() {
                    @Override
                    public void onSuccess(FirebaseManager.AttendanceStatsDetailed stats) {
                        if (stats != null && stats.getTotalSessions() > 0) {
                            double rate = stats.getAttendanceRate();
                            tvAttendanceRate.setText(String.format("%.1f%%", rate));
                            Log.d(TAG, "Attendance rate loaded: " + rate + "%");
                        } else {
                            tvAttendanceRate.setText("0%");
                            Log.d(TAG, "No attendance statistics available - showing 0%");
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error loading attendance statistics: " + error);
                        tvAttendanceRate.setText("N/A");
                    }
                });
    }

    private void setupListeners() {
        cardProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            if (currentStudent != null) {
                intent.putExtra("userEmail", currentStudent.getEmail());
                intent.putExtra("userRole", "student");
            }
            startActivity(intent);
        });
        // FIX: Corriger le listener pour cardSchedule
        cardSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScheduleActivity.class);
            startActivity(intent);
        });

        // Ajouter un nouveau bouton/card pour les statistiques
        cardStats.setOnClickListener(v -> {
            Intent intent = new Intent(this, AttendanceStatisticsActivity.class);
            startActivity(intent);
        });

        // Pour la photo de profil
        btnViewPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfilePhotoViewActivity.class);
            startActivity(intent);
        });

        // Pour voir les cours
        cardCourses.setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentCoursesActivity.class);
            startActivity(intent);
        });

        cardHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, AttendanceHistoryActivity.class);
            if (currentStudent != null) {
                intent.putExtra("userEmail", currentStudent.getEmail());
            }
            startActivity(intent);
        });

        cardJustification.setOnClickListener(v -> {
            Intent intent = new Intent(this, JustificationActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            refreshData();
            Utils.showToast(this, "Actualisation...");
            return true;
        } else if (id == R.id.action_test_system) {
            Utils.showToast(this, "Test système - Fonctionnalité à venir");
            return true;
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void refreshData() {
        if (!isDataLoading) {
            loadUserData();
        }
    }

    private void logout() {
        performLogout(); // Utiliser la nouvelle méthode sécurisée
    }

    private void performLogout() {
        // Créer un dialogue de confirmation
        new AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Êtes-vous sûr de vouloir vous déconnecter ?")
                .setIcon(R.drawable.ic_person)
                .setPositiveButton("Se déconnecter", (dialog, which) -> {
                    executeLogout();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }
    /**
     * Exécuter la déconnexion
     */
    private void executeLogout() {
        try {
            // Afficher un feedback utilisateur
            Utils.showToast(this, "Déconnexion en cours...");

            // 1. Annuler toutes les notifications en cours
            if (notificationHelper != null) {
                notificationHelper.cancelAllNotifications();
            }

            // 2. Nettoyer les données temporaires et cache
            clearTemporaryData();

            // 3. Se déconnecter de Firebase Auth
            firebaseManager.signOut();

            // 4. Effacer toutes les données utilisateur locales
            Utils.clearUserData(this);

            // 5. Nettoyer les préférences de session
            clearSessionPreferences();

            // 6. Rediriger vers l'écran de connexion
            redirectToLogin();

            // 7. Message de confirmation
            Utils.showToast(this, "Déconnexion réussie");

            // Log pour le debug
            Utils.logInfo(TAG, "User logged out successfully");

        } catch (Exception e) {
            // Gestion des erreurs
            Utils.logError(TAG, "Error during logout: " + e.getMessage());
            Utils.showToast(this, "Erreur lors de la déconnexion");

            // Forcer la redirection même en cas d'erreur
            redirectToLogin();
        }
    }

    /**
     * Nettoyer les données temporaires
     */
    private void clearTemporaryData() {
        try {
            // Nettoyer les variables d'instance
            currentStudent = null;

            // Annuler les tâches en arrière-plan si nécessaire
            // (par exemple, des handlers ou des timers)

            // Nettoyer le cache des images si nécessaire
            // Glide.get(this).clearMemory(); // Optionnel

        } catch (Exception e) {
            Utils.logError(TAG, "Error clearing temporary data: " + e.getMessage());
        }
    }

    /**
     * Nettoyer les préférences de session
     */
    private void clearSessionPreferences() {
        try {
            // Nettoyer les préférences spécifiques à la session
            Utils.setBooleanPref(this, "is_first_launch", false);
            Utils.setLongPref(this, "last_sync_time", 0);

            // Nettoyer d'autres préférences temporaires si nécessaire
            Utils.setStringPref(this, "last_notification_id", "");

        } catch (Exception e) {
            Utils.logError(TAG, "Error clearing session preferences: " + e.getMessage());
        }
    }

    /**
     * Rediriger vers l'écran de connexion
     */
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);

        // Vider la pile d'activités pour empêcher le retour
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Ajouter une animation de transition (optionnel)
        startActivity(intent);

        // Fermer l'activité actuelle
        finish();

        // Animation de fermeture (optionnel)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * Déconnexion d'urgence sans dialogue de confirmation
     * Utilisée en cas d'erreur critique ou de session expirée
     */
    public void emergencyLogout(String reason) {
        Utils.logError(TAG, "Emergency logout triggered: " + reason);
        Utils.showToast(this, "Session expirée. Redirection vers la connexion...");
        executeLogout();
    }

// 5. AJOUTER cette méthode pour gérer la déconnexion automatique en cas d'inactivité

    /**
     * Vérifier l'expiration de session (à appeler dans onResume si nécessaire)
     */
    private void checkSessionExpiry() {
        // Vérifier si l'utilisateur est toujours connecté dans Firebase
        if (!firebaseManager.isUserLoggedIn()) {
            emergencyLogout("Firebase session expired");
            return;
        }

        // Vérifier si les données locales sont cohérentes
        String savedEmail = Utils.getSavedUserEmail(this);
        String currentEmail = firebaseManager.getCurrentUserEmail();

        if (savedEmail == null || currentEmail == null || !savedEmail.equals(currentEmail)) {
            emergencyLogout("Session data inconsistency");
            return;
        }
    }



    @Override
    protected void onResume() {
        super.onResume();

        // Vérifier l'état de la session
        checkSessionExpiry();

        // Refresh data when returning to the activity (code existant)
        if (currentStudent != null) {
            Log.d(TAG, "onResume - refreshing data");
            loadTodayAttendance();
        }
    }
}