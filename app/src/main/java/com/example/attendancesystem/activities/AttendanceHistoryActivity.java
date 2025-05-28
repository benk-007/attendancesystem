package com.example.attendancesystem.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Attendance;
import com.example.attendancesystem.models.Student;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.AttendanceHistoryAdapter;
import com.example.attendancesystem.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Collections;

public class AttendanceHistoryActivity extends AppCompatActivity {

    private static final String TAG = "AttendanceHistory";

    // Views
    private Spinner spinnerCourseFilter, spinnerStatusFilter;
    private TextView tvTotalSessions, tvPresentCount, tvAbsentCount;
    private RecyclerView rvAttendanceHistory;
    private LinearLayout layoutEmptyState;
    private ProgressBar progressBar; // <--- ADD THIS LINE


    // Data
    private FirebaseManager firebaseManager;
    private Student currentStudent;
    private List<Attendance> allAttendanceList;
    private List<Attendance> filteredAttendanceList;
    private AttendanceHistoryAdapter adapter;

    // Filtres
    private String selectedCourseFilter = "Tous les cours";
    private String selectedStatusFilter = "Tous les statuts";
    // Ajouter ces variables à la classe
    private String selectedTeacherFilter = "Tous les enseignants";
    private String selectedPeriodFilter = "Toutes les périodes";
    private Calendar selectedStartDate;
    private Calendar selectedEndDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_history);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Historique des Présences");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        firebaseManager = FirebaseManager.getInstance();
        allAttendanceList = new ArrayList<>();
        filteredAttendanceList = new ArrayList<>();

        initViews();
        setupFilters();
        loadStudentData();
    }

    private void initViews() {
        spinnerCourseFilter = findViewById(R.id.spinner_course_filter);
        spinnerStatusFilter = findViewById(R.id.spinner_status_filter);
        tvTotalSessions = findViewById(R.id.tv_total_sessions);
        tvPresentCount = findViewById(R.id.tv_present_count);
        tvAbsentCount = findViewById(R.id.tv_absent_count);
        rvAttendanceHistory = findViewById(R.id.rv_attendance_history);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        progressBar = findViewById(R.id.progressBar); // <--- ADD THIS LINE (assuming you have a ProgressBar with this ID in your XML)


        // Setup RecyclerView
        adapter = new AttendanceHistoryAdapter(filteredAttendanceList);
        rvAttendanceHistory.setLayoutManager(new LinearLayoutManager(this));
        rvAttendanceHistory.setAdapter(adapter);
    }

    private void setupFilters() {
        // Filtre par statut
        String[] statusFilters = {"Tous les statuts", "Présent", "Absent", "Justifié"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statusFilters);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatusFilter.setAdapter(statusAdapter);
        // NOUVEAU: Filtre par période
        String[] periodFilters = {"Toutes les périodes", "Aujourd'hui", "Cette semaine", "Ce mois", "Personnalisé"};
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, periodFilters);
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Supposons que vous avez ajouté un Spinner pour la période
        if (findViewById(R.id.spinner_period_filter) != null) {
            Spinner spinnerPeriodFilter = findViewById(R.id.spinner_period_filter);
            spinnerPeriodFilter.setAdapter(periodAdapter);

            spinnerPeriodFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedPeriodFilter = parent.getItemAtPosition(position).toString();
                    applyFilters();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        // Listeners pour les filtres
        spinnerCourseFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCourseFilter = parent.getItemAtPosition(position).toString();
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerStatusFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStatusFilter = parent.getItemAtPosition(position).toString();
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadStudentData() {
        String userEmail = Utils.getSavedUserEmail(this);
        Log.d(TAG, "🔍 Saved user email: " + userEmail);

        if (userEmail != null) {
            firebaseManager.getStudentByEmail(userEmail, new FirebaseManager.DataCallback<Student>() {
                @Override
                public void onSuccess(Student student) {
                    currentStudent = student;
                    Log.d(TAG, "✅ Student loaded: " + student.getFullName() + " (" + student.getEmail() + ")");
                    loadAttendanceHistory();
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "❌ Error loading student data: " + error);
                    Utils.showToast(AttendanceHistoryActivity.this, "Erreur: " + error);
                    showEmptyState(true);
                }
            });
        } else {
            Log.e(TAG, "❌ No saved user email found");
            Utils.showToast(this, "Erreur: Utilisateur non connecté");
            finish();
        }
    }

    private void loadAttendanceHistory() {
        if (currentStudent == null) {
            Log.e(TAG, "❌ Cannot load attendance - currentStudent is null");
            return;
        }

        Log.d(TAG, "🔄 Loading attendance history for: " + currentStudent.getEmail());
        showLoading(true); // Afficher le chargement

        firebaseManager.getStudentAttendanceHistory(currentStudent.getEmail(),
                new FirebaseManager.DataCallback<List<Attendance>>() {
                    @Override
                    public void onSuccess(List<Attendance> attendanceList) {
                        Log.d(TAG, "🎉 Attendance history received: " + attendanceList.size() + " records");

                        showLoading(false);

                        allAttendanceList.clear();
                        allAttendanceList.addAll(attendanceList);

                        setupCourseFilter();
                        applyFilters();
                        updateStatistics();

                        // Afficher un message de succès temporaire
                        Utils.showToast(AttendanceHistoryActivity.this,
                                attendanceList.size() + " présences chargées ✅");
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "❌ Error loading attendance history: " + error);
                        showLoading(false);
                        Utils.showToast(AttendanceHistoryActivity.this, "Erreur: " + error);
                        showEmptyState(true);
                    }
                });
    }

    // Ajouter cette méthode pour afficher/masquer le chargement
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        if (rvAttendanceHistory != null) {
            rvAttendanceHistory.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void setupCourseFilter() {
        // Extraire les cours uniques
        List<String> uniqueCourses = new ArrayList<>();
        Set<String> uniqueTeachers = new HashSet<>();

        uniqueCourses.add("Tous les cours");
        uniqueTeachers.add("Tous les enseignants");


        for (Attendance attendance : allAttendanceList) {
            String courseName = attendance.getCourseName();
            if (courseName != null && !uniqueCourses.contains(courseName)) {
                uniqueCourses.add(courseName);
            }
            // Extraire les enseignants (si disponible via une jointure ou données dénormalisées)
            String teacherName = getTeacherNameFromCourse(attendance.getCourseId());
            if (teacherName != null && !teacherName.isEmpty()) {
                uniqueTeachers.add(teacherName);
            }
        }

        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, uniqueCourses);
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourseFilter.setAdapter(courseAdapter);
        // Setup enseignants (si vous avez un spinner pour ça)
        if (findViewById(R.id.spinner_teacher_filter) != null) {
            Spinner spinnerTeacherFilter = findViewById(R.id.spinner_teacher_filter);
            ArrayAdapter<String> teacherAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, new ArrayList<>(uniqueTeachers));
            teacherAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerTeacherFilter.setAdapter(teacherAdapter);
        }
    }
    // Méthode helper pour récupérer le nom de l'enseignant
    private String getTeacherNameFromCourse(String courseId) {
        // Dans une vraie app, vous feriez une requête ou utiliseriez des données dénormalisées
        // Pour l'instant, mapping basique
        switch (courseId) {
            case "3ZPc700i9U5IClIIaVxf":
                return "Teacher Anass";
            case "Math_Advanced_001":
                return "Andrew Joe";
            default:
                return "Enseignant Inconnu";
        }
    }
    private void applyFilters() {
        filteredAttendanceList.clear();

        for (Attendance attendance : allAttendanceList) {
            boolean matchesCourse = selectedCourseFilter.equals("Tous les cours") ||
                    selectedCourseFilter.equals(attendance.getCourseName());

            boolean matchesStatus = selectedStatusFilter.equals("Tous les statuts") ||
                    getStatusDisplayName(attendance.getStatus()).equals(selectedStatusFilter);

            // NOUVEAU: Filtre par période
            boolean matchesPeriod = matchesPeriodFilter(attendance);

            if (matchesCourse && matchesStatus && matchesPeriod) {
                filteredAttendanceList.add(attendance);
            }
        }
        // Trier par date (plus récent en premier)
        Collections.sort(filteredAttendanceList, (a1, a2) -> {
            if (a1.getTimestamp() == null || a2.getTimestamp() == null) return 0;
            return a2.getTimestamp().compareTo(a1.getTimestamp());
        });

        adapter.notifyDataSetChanged();
        showEmptyState(filteredAttendanceList.isEmpty());
        updateStatistics();
    }

    // NOUVELLE méthode pour filtrer par période
    private boolean matchesPeriodFilter(Attendance attendance) {
        if (selectedPeriodFilter.equals("Toutes les périodes")) {
            return true;
        }

        if (attendance.getTimestamp() == null) {
            return false;
        }

        Date attendanceDate = attendance.getTimestamp().toDate();
        Calendar attendanceCal = Calendar.getInstance();
        attendanceCal.setTime(attendanceDate);

        Calendar now = Calendar.getInstance();

        switch (selectedPeriodFilter) {
            case "Aujourd'hui":
                return isSameDay(attendanceCal, now);

            case "Cette semaine":
                return isSameWeek(attendanceCal, now);

            case "Ce mois":
                return isSameMonth(attendanceCal, now);

            case "Personnalisé":
                if (selectedStartDate != null && selectedEndDate != null) {
                    return ((Date) attendanceDate).after(selectedStartDate.getTime()) &&
                            attendanceDate.before(selectedEndDate.getTime());
                }
                return true;

            default:
                return true;
        }
    }

    // Méthodes utilitaires pour les dates
    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private boolean isSameWeek(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR);
    }

    private boolean isSameMonth(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH);
    }

    private String getStatusDisplayName(String status) {
        switch (status) {
            case "present": return "Présent";
            case "absent": return "Absent";
            case "justified": return "Justifié";
            default: return status;
        }
    }

    private void updateStatistics() {
        int totalSessions = filteredAttendanceList.size();
        int presentCount = 0;
        int absentCount = 0;

        for (Attendance attendance : filteredAttendanceList) {
            if (attendance.isPresent()) {
                presentCount++;
            } else if (attendance.isAbsent()) {
                absentCount++;
            }
        }

        tvTotalSessions.setText(String.valueOf(totalSessions));
        tvPresentCount.setText(String.valueOf(presentCount));
        tvAbsentCount.setText(String.valueOf(absentCount));
    }

    private void showEmptyState(boolean show) {
        if (show) {
            rvAttendanceHistory.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvAttendanceHistory.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}