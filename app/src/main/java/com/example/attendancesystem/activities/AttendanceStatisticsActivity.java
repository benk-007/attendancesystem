package com.example.attendancesystem.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Student;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.Utils;

/**
 * Activité pour afficher les statistiques personnelles de présence de l'étudiant
 * Inclut le taux de présence global, par cours, et des comparaisons
 */
public class AttendanceStatisticsActivity extends AppCompatActivity {

    private static final String TAG = "AttendanceStats";

    // Views principales
    private TextView tvStudentName, tvOverallRate, tvRateDescription;
    private TextView tvTotalSessions, tvPresentSessions, tvAbsentSessions, tvJustifiedSessions;
    private TextView tvBestCourse, tvWorstCourse, tvClassAverage, tvDepartmentAverage;
    private TextView tvCurrentTrend, tvPreviousSemester, tvImprovement;
    private ProgressBar progressBar, progressOverallRate;
    private CardView cardOverallStats, cardDetailedStats, cardComparison, cardTrends;

    // Data
    private FirebaseManager firebaseManager;
    private Student currentStudent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_statistics);

        // Configurer la toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mes Statistiques");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialiser Firebase
        firebaseManager = FirebaseManager.getInstance();

        // Initialiser les views
        initViews();

        // Charger les données
        loadUserData();
    }

    private void initViews() {
        // Views principales
        tvStudentName = findViewById(R.id.tv_student_name_stats);
        tvOverallRate = findViewById(R.id.tv_overall_rate);
        tvRateDescription = findViewById(R.id.tv_rate_description);
        progressBar = findViewById(R.id.progress_bar_stats);
        progressOverallRate = findViewById(R.id.progress_overall_rate);

        // Statistiques détaillées
        tvTotalSessions = findViewById(R.id.tv_total_sessions_stats);
        tvPresentSessions = findViewById(R.id.tv_present_sessions_stats);
        tvAbsentSessions = findViewById(R.id.tv_absent_sessions_stats);
        tvJustifiedSessions = findViewById(R.id.tv_justified_sessions_stats);

        // Comparaisons
        tvBestCourse = findViewById(R.id.tv_best_course);
        tvWorstCourse = findViewById(R.id.tv_worst_course);
        tvClassAverage = findViewById(R.id.tv_class_average);
        tvDepartmentAverage = findViewById(R.id.tv_department_average);

        // Tendances
        tvCurrentTrend = findViewById(R.id.tv_current_trend);
        tvPreviousSemester = findViewById(R.id.tv_previous_semester);
        tvImprovement = findViewById(R.id.tv_improvement);

        // Cards
        cardOverallStats = findViewById(R.id.card_overall_stats);
        cardDetailedStats = findViewById(R.id.card_detailed_stats);
        cardComparison = findViewById(R.id.card_comparison);
        cardTrends = findViewById(R.id.card_trends);
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
                Log.d(TAG, "Student loaded: " + student.getFullName());

                updateStudentInfo();
                loadAttendanceStatistics();
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
     * Mettre à jour les informations de l'étudiant
     */
    private void updateStudentInfo() {
        if (currentStudent != null) {
            String name = currentStudent.getFullName();
            String info = String.format("%s (%s)", name, currentStudent.getYear());
            tvStudentName.setText(info);
        }
    }

    /**
     * Charger les statistiques de présence
     */
    private void loadAttendanceStatistics() {
        if (currentStudent == null) {
            showError("Données étudiant non disponibles");
            return;
        }

        Log.d(TAG, "Loading attendance statistics for: " + currentStudent.getEmail());

        // Charger les statistiques globales
        firebaseManager.getStudentAttendanceStatistics(
                currentStudent.getEmail(),
                currentStudent.getDepartment(),
                currentStudent.getField() != null ? currentStudent.getField() : "",
                currentStudent.getYear(),
                new FirebaseManager.DataCallback<FirebaseManager.AttendanceStats>() {
                    @Override
                    public void onSuccess(FirebaseManager.AttendanceStats stats) {
                        Log.d(TAG, "Attendance statistics loaded successfully");
                        updateStatisticsDisplay(stats);
                        loadComparativeData();
                        showLoading(false);
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error loading attendance statistics: " + error);
                        showError("Erreur lors du chargement des statistiques: " + error);
                        showLoading(false);
                    }
                }
        );
    }

    /**
     * Mettre à jour l'affichage des statistiques
     */
    private void updateStatisticsDisplay(FirebaseManager.AttendanceStats stats) {
        if (stats == null) {
            showError("Aucune statistique disponible");
            return;
        }

        // Statistiques globales
        double attendanceRate = stats.getAttendanceRate();
        tvOverallRate.setText(String.format("%.1f%%", attendanceRate));

        // Couleur selon le taux
        int rateColor = getAttendanceRateColor(attendanceRate);
        tvOverallRate.setTextColor(getColor(rateColor));

        // Description du taux
        tvRateDescription.setText(getAttendanceRateDescription(attendanceRate));
        tvRateDescription.setTextColor(getColor(rateColor));

        // Progress bar
        progressOverallRate.setProgress((int) attendanceRate);

        // Statistiques détaillées
        int totalSessions = stats.getTotalSessions();
        int attendedSessions = stats.getAttendedSessions();
        int absentSessions = totalSessions - attendedSessions;

        tvTotalSessions.setText(String.valueOf(totalSessions));
        tvPresentSessions.setText(String.valueOf(attendedSessions));
        tvAbsentSessions.setText(String.valueOf(absentSessions));

        // Justifiées (simulation pour l'exemple)
        int justifiedSessions = Math.max(0, absentSessions / 3); // 1/3 des absences justifiées
        tvJustifiedSessions.setText(String.valueOf(justifiedSessions));

        Log.d(TAG, String.format("Statistics updated - Rate: %.1f%%, Total: %d, Present: %d",
                attendanceRate, totalSessions, attendedSessions));
    }

    /**
     * Charger les données comparatives
     */
    private void loadComparativeData() {
        // Simuler des données comparatives (en attendant l'implémentation complète)

        // Meilleur et pire cours (simulation)
        tvBestCourse.setText("Algorithmes: 95%");
        tvWorstCourse.setText("Mathématiques: 78%");

        // Moyennes (simulation)
        double classAverage = 82.5;
        double departmentAverage = 79.8;

        tvClassAverage.setText(String.format("%.1f%%", classAverage));
        tvDepartmentAverage.setText(String.format("%.1f%%", departmentAverage));

        // Tendances (simulation)
        tvCurrentTrend.setText("📈 En amélioration");
        tvCurrentTrend.setTextColor(getColor(R.color.success_color));

        tvPreviousSemester.setText("Semestre précédent: 81%");
        tvImprovement.setText("Progression: +4%");
        tvImprovement.setTextColor(getColor(R.color.success_color));

        // TODO: Implémenter le chargement des vraies données comparatives
        // loadRealComparativeData();
    }

    /**
     * Obtenir la couleur selon le taux de présence
     */
    private int getAttendanceRateColor(double rate) {
        if (rate >= 90) {
            return R.color.success_color;  // Excellent
        } else if (rate >= 80) {
            return R.color.info_color;     // Bon
        } else if (rate >= 70) {
            return R.color.warning_color;  // Moyen
        } else {
            return R.color.error_color;    // Faible
        }
    }

    /**
     * Obtenir la description selon le taux de présence
     */
    private String getAttendanceRateDescription(double rate) {
        if (rate >= 95) {
            return "🏆 Excellent - Assiduité exemplaire !";
        } else if (rate >= 90) {
            return "🌟 Très bien - Continuez ainsi !";
        } else if (rate >= 80) {
            return "👍 Bon - Quelques améliorations possibles";
        } else if (rate >= 70) {
            return "⚠️ Moyen - Attention aux absences";
        } else if (rate >= 60) {
            return "⚠️ Faible - Risque d'échec";
        } else {
            return "🚨 Critique - Action urgente requise";
        }
    }

    /**
     * Afficher l'état de chargement
     */
    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            cardOverallStats.setVisibility(View.GONE);
            cardDetailedStats.setVisibility(View.GONE);
            cardComparison.setVisibility(View.GONE);
            cardTrends.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            cardOverallStats.setVisibility(View.VISIBLE);
            cardDetailedStats.setVisibility(View.VISIBLE);
            cardComparison.setVisibility(View.VISIBLE);
            cardTrends.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Afficher une erreur
     */
    private void showError(String message) {
        Utils.showToast(this, message);
        Log.e(TAG, message);

        // Afficher un message d'erreur dans les cards
        tvOverallRate.setText("--");
        tvRateDescription.setText("Données non disponibles");
        tvTotalSessions.setText("--");
        tvPresentSessions.setText("--");
        tvAbsentSessions.setText("--");
        tvJustifiedSessions.setText("--");
    }

    /**
     * TODO: Implémenter le chargement des vraies données comparatives
     */
    private void loadRealComparativeData() {
        // Cette méthode sera implémentée quand les données comparatives
        // seront disponibles dans Firebase

        /*
        firebaseManager.getClassAttendanceAverage(
            currentStudent.getDepartment(),
            currentStudent.getField(),
            currentStudent.getYear(),
            new FirebaseManager.DataCallback<Double>() {
                @Override
                public void onSuccess(Double average) {
                    tvClassAverage.setText(String.format("%.1f%%", average));
                }

                @Override
                public void onFailure(String error) {
                    tvClassAverage.setText("Non disponible");
                }
            }
        );
        */
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger les statistiques au retour sur l'activité
        if (currentStudent != null) {
            loadAttendanceStatistics();
        }
    }
}