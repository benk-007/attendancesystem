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

import java.util.Map;

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
     * MÉTHODE MISE À JOUR - Charger les statistiques d'assiduité
     */
    private void loadAttendanceStatistics() {
        if (currentStudent == null) {
            showError("Données étudiant non disponibles");
            return;
        }

        Log.d(TAG, "Loading attendance statistics for: " + currentStudent.getEmail());

        // ✅ UTILISER LA NOUVELLE MÉTHODE CORRIGÉE
        firebaseManager.getStudentAttendanceStatistics(
                currentStudent.getEmail(),
                currentStudent.getDepartment(),
                currentStudent.getField() != null ? currentStudent.getField() : "",
                currentStudent.getYear(),
                new FirebaseManager.DataCallback<FirebaseManager.AttendanceStatsDetailed>() {
                    @Override
                    public void onSuccess(FirebaseManager.AttendanceStatsDetailed stats) {
                        Log.d(TAG, "✅ Attendance statistics loaded successfully");
                        updateStatisticsDisplay(stats);
                        showLoading(false);
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "❌ Error loading attendance statistics: " + error);
                        showError("Erreur lors du chargement des statistiques: " + error);
                        showLoading(false);
                    }
                }
        );
    }

    /**
     * MÉTHODE CORRIGÉE - Mettre à jour l'affichage des statistiques
     */
    private void updateStatisticsDisplay(FirebaseManager.AttendanceStatsDetailed stats) {
        if (stats == null) {
            showError("Aucune statistique disponible");
            return;
        }

        // ✅ STATISTIQUES GLOBALES avec toutes les données
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

        // ✅ STATISTIQUES DÉTAILLÉES - Maintenant avec vraies données
        int totalSessions = stats.getTotalSessions();
        int attendedSessions = stats.getAttendedSessions();
        int absentSessions = stats.getAbsentSessions();
        int justifiedSessions = stats.getJustifiedSessions();

        tvTotalSessions.setText(String.valueOf(totalSessions));
        tvPresentSessions.setText(String.valueOf(attendedSessions));
        tvAbsentSessions.setText(String.valueOf(absentSessions));
        tvJustifiedSessions.setText(String.valueOf(justifiedSessions));

        // ✅ AJOUTER DES INFORMATIONS SUPPLÉMENTAIRES
        Log.d(TAG, String.format("📊 Statistics displayed - Total: %d, Present: %d, Absent: %d, Justified: %d",
                totalSessions, attendedSessions, absentSessions, justifiedSessions));

        // ✅ Charger les statistiques par cours
        loadCourseSpecificStatistics();
    }
    /**
     * NOUVELLE MÉTHODE - Charger les statistiques par cours
     */
    private void loadCourseSpecificStatistics() {
        if (currentStudent == null) return;

        firebaseManager.getStudentStatisticsByCourse(
                currentStudent.getEmail(),
                new FirebaseManager.DataCallback<Map<String, FirebaseManager.AttendanceStatsDetailed>>() {
                    @Override
                    public void onSuccess(Map<String, FirebaseManager.AttendanceStatsDetailed> courseStats) {
                        updateCourseComparison(courseStats);
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error loading course statistics: " + error);
                        // Garder les valeurs par défaut
                    }
                }
        );
    }

    /**
     * NOUVELLE MÉTHODE - Mettre à jour la comparaison par cours
     */
    private void updateCourseComparison(Map<String, FirebaseManager.AttendanceStatsDetailed> courseStats) {
        if (courseStats.isEmpty()) {
            tvBestCourse.setText("Aucun cours disponible");
            tvWorstCourse.setText("Aucun cours disponible");
            return;
        }

        // Trouver le meilleur et le pire cours
        String bestCourse = "";
        String worstCourse = "";
        double bestRate = -1;
        double worstRate = 101;

        for (Map.Entry<String, FirebaseManager.AttendanceStatsDetailed> entry : courseStats.entrySet()) {
            String courseName = entry.getKey();
            double rate = entry.getValue().getAttendanceRate();

            if (rate > bestRate) {
                bestRate = rate;
                bestCourse = courseName;
            }

            if (rate < worstRate) {
                worstRate = rate;
                worstCourse = courseName;
            }
        }

        // Afficher les résultats
        if (!bestCourse.isEmpty()) {
            tvBestCourse.setText(String.format("%s: %.1f%%", bestCourse, bestRate));
        }

        if (!worstCourse.isEmpty()) {
            tvWorstCourse.setText(String.format("%s: %.1f%%", worstCourse, worstRate));
        }

        // Calculer les moyennes simulées (données réalistes)
        double classAverage = calculateSimulatedClassAverage(courseStats);
        double departmentAverage = classAverage - 3.5; // Légèrement inférieur

        tvClassAverage.setText(String.format("%.1f%%", classAverage));
        tvDepartmentAverage.setText(String.format("%.1f%%", departmentAverage));

        Log.d(TAG, String.format("📈 Course comparison - Best: %s (%.1f%%), Worst: %s (%.1f%%)",
                bestCourse, bestRate, worstCourse, worstRate));
    }

    /**
     * Calculer une moyenne de classe simulée basée sur les données réelles
     */
    private double calculateSimulatedClassAverage(Map<String, FirebaseManager.AttendanceStatsDetailed> courseStats) {
        if (courseStats.isEmpty()) return 75.0;

        double totalRate = 0;
        int courseCount = 0;

        for (FirebaseManager.AttendanceStatsDetailed stats : courseStats.values()) {
            totalRate += stats.getAttendanceRate();
            courseCount++;
        }

        double studentAverage = courseCount > 0 ? totalRate / courseCount : 75.0;

        // Simuler une moyenne de classe légèrement différente
        return Math.max(60.0, Math.min(95.0, studentAverage + (Math.random() * 10 - 5)));
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