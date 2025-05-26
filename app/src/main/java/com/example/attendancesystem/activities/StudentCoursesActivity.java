package com.example.attendancesystem.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Student;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.StudentCoursesAdapter;
import com.example.attendancesystem.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Activité pour afficher les cours auxquels l'étudiant est rattaché
 * Basé sur le département, la filière et l'année de l'étudiant
 */
public class StudentCoursesActivity extends AppCompatActivity {

    private static final String TAG = "StudentCourses";

    // Views
    private TextView tvStudentInfo, tvCoursesCount;
    private RecyclerView rvCourses;
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyState;

    // Data
    private FirebaseManager firebaseManager;
    private Student currentStudent;
    private List<Map<String, String>> studentCourses;
    private StudentCoursesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_courses);

        // Configurer la toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mes Cours");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialiser Firebase
        firebaseManager = FirebaseManager.getInstance();
        studentCourses = new ArrayList<>();

        // Initialiser les views
        initViews();
        setupRecyclerView();

        // Charger les données utilisateur
        loadUserData();
    }

    private void initViews() {
        tvStudentInfo = findViewById(R.id.tv_student_info);
        tvCoursesCount = findViewById(R.id.tv_courses_count);
        rvCourses = findViewById(R.id.rv_student_courses);
        progressBar = findViewById(R.id.progress_bar_courses);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
    }

    private void setupRecyclerView() {
        adapter = new StudentCoursesAdapter(studentCourses);
        rvCourses.setLayoutManager(new LinearLayoutManager(this));
        rvCourses.setAdapter(adapter);
    }

    private void loadUserData() {
        String userEmail = Utils.getSavedUserEmail(this);
        if (userEmail == null) {
            Utils.showToast(this, "Erreur: Utilisateur non connecté");
            finish();
            return;
        }

        showLoading(true);

        firebaseManager.getStudentByEmail(userEmail, new FirebaseManager.DataCallback<Student>() {
            @Override
            public void onSuccess(Student student) {
                currentStudent = student;
                Log.d(TAG, "Student loaded: " + student.getFullName() +
                        " - Dept: " + student.getDepartment() +
                        " - Field: " + student.getField() +
                        " - Year: " + student.getYear());

                updateStudentInfo();
                loadStudentCourses();
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Error loading student data: " + error);
                showError("Erreur lors du chargement des données: " + error);
                showLoading(false);
            }
        });
    }

    /**
     * Mettre à jour les informations de l'étudiant affichées
     */
    private void updateStudentInfo() {
        if (currentStudent != null) {
            String info = String.format("%s - %s %s (%s)",
                    currentStudent.getFullName(),
                    currentStudent.getField() != null ? currentStudent.getField() : "Filière non définie",
                    currentStudent.getYear(),
                    currentStudent.getDepartment()
            );
            tvStudentInfo.setText(info);
        }
    }

    /**
     * Charger les cours auxquels l'étudiant est rattaché
     */
    private void loadStudentCourses() {
        if (currentStudent == null) {
            showError("Données étudiant non disponibles");
            return;
        }

        Log.d(TAG, "Loading courses for student: " +
                "Department=" + currentStudent.getDepartment() +
                ", Field=" + currentStudent.getField() +
                ", Year=" + currentStudent.getYear());

        firebaseManager.getStudentCourses(
                currentStudent.getEmail(),
                currentStudent.getDepartment(),
                currentStudent.getField() != null ? currentStudent.getField() : "",
                currentStudent.getYear(),
                new FirebaseManager.DataCallback<List<Map<String, String>>>() {
                    @Override
                    public void onSuccess(List<Map<String, String>> courses) {
                        Log.d(TAG, "Courses loaded successfully: " + courses.size() + " courses found");

                        studentCourses.clear();
                        studentCourses.addAll(courses);

                        updateCoursesDisplay();
                        showLoading(false);
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error loading student courses: " + error);
                        showError("Erreur lors du chargement des cours: " + error);
                        showLoading(false);
                    }
                }
        );
    }

    /**
     * Mettre à jour l'affichage des cours
     */
    private void updateCoursesDisplay() {
        if (studentCourses.isEmpty()) {
            showEmptyState(true);
            tvCoursesCount.setText("Aucun cours trouvé");
        } else {
            showEmptyState(false);

            // Mettre à jour le compteur
            String countText = studentCourses.size() == 1 ?
                    "1 cours trouvé" :
                    studentCourses.size() + " cours trouvés";
            tvCoursesCount.setText(countText);

            // Mettre à jour l'adapter
            adapter.notifyDataSetChanged();

            Log.d(TAG, "Courses display updated with " + studentCourses.size() + " courses");
        }
    }

    /**
     * Afficher/masquer l'état vide
     */
    private void showEmptyState(boolean show) {
        if (show) {
            rvCourses.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvCourses.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    /**
     * Afficher l'état de chargement
     */
    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            rvCourses.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            // Les autres vues seront gérées par updateCoursesDisplay()
        }
    }

    /**
     * Afficher une erreur
     */
    private void showError(String message) {
        Utils.showToast(this, message);
        Log.e(TAG, message);

        // Afficher l'état vide avec message d'erreur
        showEmptyState(true);
        tvCoursesCount.setText("Erreur de chargement");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger les cours au retour sur l'activité
        if (currentStudent != null) {
            loadStudentCourses();
        }
    }
}