package com.example.attendancesystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Session;
import com.example.attendancesystem.models.Teacher;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.Utils;

import java.util.List;

public class TeacherDashboardActivity extends AppCompatActivity {

    private static final String TAG = "TeacherDashboard";

    // Views
    private TextView tvWelcome, tvTodayCourses, tvStudentsPresent, tvUpcomingCourse;
    private CardView cardManageSession, cardViewReports, cardStudentList, cardCourseManagement;

    // Firebase
    private FirebaseManager firebaseManager;
    private Teacher currentTeacher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        // Configurer la toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Dashboard Enseignant");
        }

        // Initialiser Firebase
        firebaseManager = FirebaseManager.getInstance();

        // Initialiser les views
        initViews();

        // Charger les données utilisateur
        loadUserData();

        // Configurer les listeners
        setupListeners();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tv_welcome);
        tvTodayCourses = findViewById(R.id.tv_today_courses);
        tvStudentsPresent = findViewById(R.id.tv_students_present);
        tvUpcomingCourse = findViewById(R.id.tv_upcoming_course);

        cardManageSession = findViewById(R.id.card_manage_session);
        cardViewReports = findViewById(R.id.card_view_reports);
        cardStudentList = findViewById(R.id.card_student_list);
        cardCourseManagement = findViewById(R.id.card_course_management);
    }

    private void loadUserData() {
        String userEmail = Utils.getSavedUserEmail(this);
        if (userEmail != null) {
            Log.d(TAG, "Loading teacher data for: " + userEmail);
            firebaseManager.getTeacherByEmail(userEmail, new FirebaseManager.DataCallback<Teacher>() {
                @Override
                public void onSuccess(Teacher teacher) {
                    currentTeacher = teacher;
                    Log.d(TAG, "Teacher loaded: " + teacher.getFullName() +
                            " - Department: " + teacher.getDepartment());
                    updateUI();
                    loadTodayStatistics();
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Error loading teacher data: " + error);
                    Utils.showToast(TeacherDashboardActivity.this, "Erreur de chargement: " + error);
                }
            });
        } else {
            Log.e(TAG, "No saved user email found");
            Utils.showToast(this, "Utilisateur non connecté");
        }
    }

    private void updateUI() {
        if (currentTeacher != null) {
            String firstName = Utils.getFirstName(currentTeacher.getFullName());
            String welcomeText = "Bonjour, Prof. " + firstName + " !";
            tvWelcome.setText(welcomeText);

            Log.d(TAG, "UI updated for teacher: " + firstName);
        }
    }

    private void loadTodayStatistics() {
        if (currentTeacher == null) {
            Log.w(TAG, "Cannot load today statistics - currentTeacher is null");
            return;
        }

        Log.d(TAG, "Loading today's sessions for teacher: " + currentTeacher.getEmail());

        // Charger les sessions d'aujourd'hui pour cet enseignant
        firebaseManager.getTodaySessionsForTeacher(currentTeacher.getEmail(),
                new FirebaseManager.DataCallback<List<Session>>() {
                    @Override
                    public void onSuccess(List<Session> sessions) {
                        Log.d(TAG, "Today's sessions loaded successfully: " + sessions.size() + " sessions found");

                        if (sessions != null) {
                            for (int i = 0; i < sessions.size(); i++) {
                                Session s = sessions.get(i);
                                Log.d(TAG, "Session " + (i+1) + ": " + s.getCourseName() +
                                        " - Field: " + s.getField() +
                                        " - Time: " + s.getStartTime() +
                                        " - Status: " + s.getStatus());
                            }
                        }

                        updateTodaySessionsInfo(sessions);
                        loadNextSession();
                        loadAttendanceStatistics(sessions);
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error loading today's sessions: " + error);
                        Utils.showToast(TeacherDashboardActivity.this, "Erreur lors du chargement des sessions: " + error);
                        tvTodayCourses.setText("Erreur de chargement");
                        tvStudentsPresent.setText("Données non disponibles");
                    }
                });
    }

    private void updateTodaySessionsInfo(List<Session> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            tvTodayCourses.setText("Aucun cours aujourd'hui");
            Log.d(TAG, "No sessions today");
            return;
        }

        int totalSessions = sessions.size();
        int activeSessions = 0;
        int completedSessions = 0;

        for (Session session : sessions) {
            Log.d(TAG, "Processing session: " + session.getCourseName() + " - Status: " + session.getStatus());

            if (session.isActive()) {
                activeSessions++;
            } else if (session.isCompleted()) {
                completedSessions++;
            }
        }

        String statusText;
        if (activeSessions > 0) {
            statusText = "Session en cours - " + totalSessions + " cours aujourd'hui";
        } else if (completedSessions > 0) {
            statusText = completedSessions + "/" + totalSessions + " cours terminés aujourd'hui";
        } else {
            statusText = totalSessions + " cours programmés aujourd'hui";
        }

        tvTodayCourses.setText(statusText);
        Log.d(TAG, "Today sessions info updated: " + statusText);
    }

    private void loadNextSession() {
        if (currentTeacher == null) {
            Log.w(TAG, "Cannot load next session - currentTeacher is null");
            return;
        }

        Log.d(TAG, "Loading next session for teacher: " + currentTeacher.getEmail());

        // Charger la prochaine session pour cet enseignant
        firebaseManager.getNextSessionForTeacher(currentTeacher.getEmail(),
                new FirebaseManager.DataCallback<Session>() {
                    @Override
                    public void onSuccess(Session session) {
                        if (session != null) {
                            Log.d(TAG, "Next session found: " + session.getCourseName() +
                                    " - Field: " + session.getField() +
                                    " at " + session.getStartTime() +
                                    " in " + session.getRoom());

                            String timeStr = Utils.formatTime(session.getStartTime());
                            String sessionText = session.getCourseName() + " (" + session.getField() + ") - " +
                                    timeStr + " (" + session.getRoom() + ")";
                            tvUpcomingCourse.setText(sessionText);
                        } else {
                            Log.d(TAG, "No next session found");
                            tvUpcomingCourse.setText("Aucun cours programmé");
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error loading next session: " + error);
                        tvUpcomingCourse.setText("Erreur de chargement");
                    }
                });
    }

    private void loadAttendanceStatistics(List<Session> todaySessions) {
        if (todaySessions == null || todaySessions.isEmpty()) {
            tvStudentsPresent.setText("Aucune statistique disponible");
            Log.d(TAG, "No sessions for attendance statistics");
            return;
        }

        int totalStudents = 0;
        int totalPresent = 0;
        int sessionsWithData = 0;

        for (Session session : todaySessions) {
            if (session.isCompleted() && session.getStatistics() != null) {
                totalStudents += session.getStatistics().getTotalEnrolled();
                totalPresent += session.getStatistics().getTotalPresent();
                sessionsWithData++;

                Log.d(TAG, "Session " + session.getCourseName() + " stats: " +
                        session.getStatistics().getTotalPresent() + "/" +
                        session.getStatistics().getTotalEnrolled());
            }
        }

        String statsText;
        if (sessionsWithData > 0 && totalStudents > 0) {
            double attendanceRate = (double) totalPresent / totalStudents * 100;
            statsText = String.format("%.1f%% de présence moyenne", attendanceRate);
        } else {
            // Vérifier s'il y a une session active
            boolean hasActiveSession = false;
            for (Session session : todaySessions) {
                if (session.isActive()) {
                    hasActiveSession = true;
                    break;
                }
            }

            if (hasActiveSession) {
                statsText = "Session en cours...";
            } else {
                statsText = "Statistiques non disponibles";
            }
        }

        tvStudentsPresent.setText(statsText);
        Log.d(TAG, "Attendance statistics updated: " + statsText);
    }

    private void setupListeners() {
        cardManageSession.setOnClickListener(v -> {
            Intent intent = new Intent(this, SessionManagementActivity.class);
            if (currentTeacher != null) {
                intent.putExtra("teacherEmail", currentTeacher.getEmail());
            }
            startActivity(intent);
        });

        cardViewReports.setOnClickListener(v -> {
            // TODO: Créer l'activité de rapports
            Utils.showToast(this, "Rapports - Fonctionnalité à venir");
        });

        cardStudentList.setOnClickListener(v -> {
            // TODO: Créer l'activité de liste d'étudiants
            Utils.showToast(this, "Liste étudiants - Fonctionnalité à venir");
        });

        cardCourseManagement.setOnClickListener(v -> {
            Intent intent = new Intent(this, SessionManagementActivity.class);
            if (currentTeacher != null) {
                intent.putExtra("teacherEmail", currentTeacher.getEmail());
            }
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
            if (currentTeacher != null) {
                loadTodayStatistics();
            } else {
                loadUserData();
            }
            Utils.showToast(this, "Données actualisées");
            return true;
        } else if (id == R.id.action_test_system) {
            // Test system functionality
            Utils.showToast(this, "Test système - Fonctionnalité à venir");
            return true;
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        firebaseManager.signOut();
        Utils.clearUserData(this);

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Actualiser les données quand on revient sur l'activité
        if (currentTeacher != null) {
            Log.d(TAG, "onResume - refreshing data");
            loadTodayStatistics();
        }
    }
}