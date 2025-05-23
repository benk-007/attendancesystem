package com.example.attendancesystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Teacher;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.Utils;

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
            firebaseManager.getTeacherByEmail(userEmail, new FirebaseManager.DataCallback<Teacher>() {
                @Override
                public void onSuccess(Teacher teacher) {
                    currentTeacher = teacher;
                    updateUI();
                    loadTodayStatistics();
                }

                @Override
                public void onFailure(String error) {
                    Utils.showToast(TeacherDashboardActivity.this, "Erreur de chargement: " + error);
                }
            });
        }
    }

    private void updateUI() {
        if (currentTeacher != null) {
            String firstName = Utils.getFirstName(currentTeacher.getFullName());
            String welcomeText = "Bonjour, Prof. " + firstName + " !";
            tvWelcome.setText(welcomeText);

            // Afficher les informations de l'enseignant
            String teacherInfo = currentTeacher.getDepartment();
        }
    }

    private void loadTodayStatistics() {
        // TODO: Implémenter le chargement des vraies statistiques
        tvTodayCourses.setText("3 cours aujourd'hui");
        tvStudentsPresent.setText("Calcul en cours...");
        tvUpcomingCourse.setText("Mathématiques L2 - 14:00 (Amphi B)");

        // Charger les statistiques des cours de l'enseignant
        loadCourseStatistics();
    }

    private void loadCourseStatistics() {
        if (currentTeacher != null) {
            // TODO: Implémenter le calcul des statistiques des cours
            // Pour l'instant, affichage de données de test
            tvStudentsPresent.setText("78% de présence moyenne");
        }
    }

    private void setupListeners() {
        cardManageSession.setOnClickListener(v -> {
            // TODO: Créer l'activité de gestion de session
            Utils.showToast(this, "Gestion de session - Fonctionnalité à venir");
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
            Intent intent = new Intent(this, CourseManagementActivity.class);
            intent.putExtra("teacherEmail", currentTeacher.getEmail());
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
        if (currentTeacher != null) {
            loadTodayStatistics();
        }
    }
}