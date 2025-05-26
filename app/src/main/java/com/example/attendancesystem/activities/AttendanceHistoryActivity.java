package com.example.attendancesystem.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
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
import java.util.List;

public class AttendanceHistoryActivity extends AppCompatActivity {

    private static final String TAG = "AttendanceHistory";

    // Views
    private Spinner spinnerCourseFilter, spinnerStatusFilter;
    private TextView tvTotalSessions, tvPresentCount, tvAbsentCount;
    private RecyclerView rvAttendanceHistory;
    private LinearLayout layoutEmptyState;

    // Data
    private FirebaseManager firebaseManager;
    private Student currentStudent;
    private List<Attendance> allAttendanceList;
    private List<Attendance> filteredAttendanceList;
    private AttendanceHistoryAdapter adapter;

    // Filtres
    private String selectedCourseFilter = "Tous les cours";
    private String selectedStatusFilter = "Tous les statuts";

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
        if (userEmail != null) {
            firebaseManager.getStudentByEmail(userEmail, new FirebaseManager.DataCallback<Student>() {
                @Override
                public void onSuccess(Student student) {
                    currentStudent = student;
                    loadAttendanceHistory();
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Error loading student data: " + error);
                    Utils.showToast(AttendanceHistoryActivity.this, "Erreur: " + error);
                    showEmptyState(true);
                }
            });
        } else {
            Utils.showToast(this, "Erreur: Utilisateur non connecté");
            finish();
        }
    }

    private void loadAttendanceHistory() {
        if (currentStudent == null) return;

        firebaseManager.getStudentAttendanceHistory(currentStudent.getEmail(),
                new FirebaseManager.DataCallback<List<Attendance>>() {
                    @Override
                    public void onSuccess(List<Attendance> attendanceList) {
                        Log.d(TAG, "Attendance history loaded: " + attendanceList.size() + " records");
                        allAttendanceList.clear();
                        allAttendanceList.addAll(attendanceList);

                        setupCourseFilter();
                        applyFilters();
                        updateStatistics();
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error loading attendance history: " + error);
                        Utils.showToast(AttendanceHistoryActivity.this, "Erreur: " + error);
                        showEmptyState(true);
                    }
                });
    }

    private void setupCourseFilter() {
        // Extraire les cours uniques
        List<String> uniqueCourses = new ArrayList<>();
        uniqueCourses.add("Tous les cours");

        for (Attendance attendance : allAttendanceList) {
            String courseName = attendance.getCourseName();
            if (courseName != null && !uniqueCourses.contains(courseName)) {
                uniqueCourses.add(courseName);
            }
        }

        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, uniqueCourses);
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourseFilter.setAdapter(courseAdapter);
    }

    private void applyFilters() {
        filteredAttendanceList.clear();

        for (Attendance attendance : allAttendanceList) {
            boolean matchesCourse = selectedCourseFilter.equals("Tous les cours") ||
                    selectedCourseFilter.equals(attendance.getCourseName());

            boolean matchesStatus = selectedStatusFilter.equals("Tous les statuts") ||
                    getStatusDisplayName(attendance.getStatus()).equals(selectedStatusFilter);

            if (matchesCourse && matchesStatus) {
                filteredAttendanceList.add(attendance);
            }
        }

        adapter.notifyDataSetChanged();
        showEmptyState(filteredAttendanceList.isEmpty());
        updateStatistics();
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