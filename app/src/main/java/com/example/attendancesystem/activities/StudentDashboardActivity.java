package com.example.attendancesystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Student;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.Utils;


public class StudentDashboardActivity extends AppCompatActivity {

    private static final String TAG = "StudentDashboard";

    // Views
    private TextView tvWelcome, tvTodayStatus, tvAttendanceRate, tvNextCourse;
    private CardView cardProfile, cardHistory, cardJustification, cardSchedule;

    // Firebase
    private FirebaseManager firebaseManager;
    private Student currentStudent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        // Configurer la toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Dashboard Étudiant");
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
        tvTodayStatus = findViewById(R.id.tv_today_status);
        tvAttendanceRate = findViewById(R.id.tv_attendance_rate);
        tvNextCourse = findViewById(R.id.tv_next_course);

        cardProfile = findViewById(R.id.card_profile);
        cardHistory = findViewById(R.id.card_history);
        cardJustification = findViewById(R.id.card_justification);
        cardSchedule = findViewById(R.id.card_schedule);
    }

    private void loadUserData() {
        String userEmail = Utils.getSavedUserEmail(this);
        if (userEmail != null) {
            firebaseManager.getStudentByEmail(userEmail, new FirebaseManager.DataCallback<Student>() {
                @Override
                public void onSuccess(Student student) {
                    currentStudent = student;
                    updateUI();
                    loadTodayAttendance();
                }

                @Override
                public void onFailure(String error) {
                    Utils.showToast(StudentDashboardActivity.this, "Erreur de chargement: " + error);
                }
            });
        }
    }

    private void updateUI() {
        if (currentStudent != null) {
            String firstName = Utils.getFirstName(currentStudent.getFullName());
            String welcomeText = "Bonjour, " + firstName + " !";
            tvWelcome.setText(welcomeText);

            // Afficher les informations de l'étudiant
            String studentInfo = currentStudent.getDepartment() + " - " + currentStudent.getYear();

            // Mettre à jour les informations de base
            tvNextCourse.setText("Mathématiques - 10:00 (Salle A101)");
            tvAttendanceRate.setText("Calcul en cours...");
        }
    }

    private void loadTodayAttendance() {
        // TODO: Implémenter le chargement des présences du jour
        tvTodayStatus.setText("Aucun cours aujourd'hui");

        // Charger les statistiques de présence
        loadAttendanceStatistics();
    }

    private void loadAttendanceStatistics() {
        if (currentStudent != null) {
            // TODO: Implémenter le calcul des statistiques de présence
            // Pour l'instant, affichage de données de test
            tvAttendanceRate.setText("85.2% de présence");
        }
    }

    private void setupListeners() {
        cardProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("userEmail", currentStudent.getEmail());
            intent.putExtra("userRole", "student");
            startActivity(intent);
        });

        cardHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, AttendanceHistoryActivity.class);
            intent.putExtra("userEmail", currentStudent.getEmail());
            startActivity(intent);
        });

        cardJustification.setOnClickListener(v -> {
            // TODO: Créer l'activité de justification
            Utils.showToast(this, "Fonctionnalité de justification à venir");
        });

        cardSchedule.setOnClickListener(v -> {
            // TODO: Créer l'activité d'emploi du temps
            Utils.showToast(this, "Emploi du temps à venir");
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
            loadUserData();
            Utils.showToast(this, "Données actualisées");
            return true;
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
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
        if (currentStudent != null) {
            loadTodayAttendance();
        }
    }
}