package com.example.attendancesystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendancesystem.R;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.Utils;

public class SessionManagementActivity extends AppCompatActivity {

    private static final String TAG = "SessionManagement";

    // Views
    private TextView tvCurrentSession, tvSessionStatus, tvStudentCount;
    private Button btnStartSession, btnEndSession, btnManualAttendance;
    private CardView cardFaceRecognition;
    private RecyclerView rvCurrentStudents;

    // Data
    private FirebaseManager firebaseManager;
    private boolean isSessionActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_management);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Gestion de Session");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        firebaseManager = FirebaseManager.getInstance();

        initViews();
        setupListeners();
        updateUI();
    }

    private void initViews() {
        tvCurrentSession = findViewById(R.id.tv_current_session);
        tvSessionStatus = findViewById(R.id.tv_session_status);
        tvStudentCount = findViewById(R.id.tv_student_count);
        btnStartSession = findViewById(R.id.btn_start_session);
        btnEndSession = findViewById(R.id.btn_end_session);
        btnManualAttendance = findViewById(R.id.btn_manual_attendance);
        cardFaceRecognition = findViewById(R.id.card_face_recognition);
        rvCurrentStudents = findViewById(R.id.rv_current_students);

        // Setup RecyclerView
        rvCurrentStudents.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupListeners() {
        btnStartSession.setOnClickListener(v -> startSession());
        btnEndSession.setOnClickListener(v -> endSession());
        btnManualAttendance.setOnClickListener(v -> openManualAttendance());
        cardFaceRecognition.setOnClickListener(v -> openFaceRecognition());
    }

    private void startSession() {
        // TODO: Créer une nouvelle session dans Firebase
        isSessionActive = true;
        updateUI();
        Utils.showToast(this, "Session démarrée");
    }

    private void endSession() {
        // TODO: Terminer la session dans Firebase
        isSessionActive = false;
        updateUI();
        Utils.showToast(this, "Session terminée");
    }

    private void openManualAttendance() {
        // TODO: Ouvrir l'activité de pointage manuel
        Utils.showToast(this, "Pointage manuel - À implémenter");
    }

    private void openFaceRecognition() {
        ///////
    }

    private void updateUI() {
        if (isSessionActive) {
            tvCurrentSession.setText("Mathématiques L2 - Salle A101");
            tvSessionStatus.setText("Session Active");
            tvSessionStatus.setTextColor(getColor(R.color.success_color));
            tvStudentCount.setText("15 étudiants présents / 25 inscrits");

            btnStartSession.setVisibility(View.GONE);
            btnEndSession.setVisibility(View.VISIBLE);
            btnManualAttendance.setEnabled(true);
            cardFaceRecognition.setAlpha(1.0f);
        } else {
            tvCurrentSession.setText("Aucune session active");
            tvSessionStatus.setText("Session Inactive");
            tvSessionStatus.setTextColor(getColor(R.color.text_secondary));
            tvStudentCount.setText("0 étudiants présents");

            btnStartSession.setVisibility(View.VISIBLE);
            btnEndSession.setVisibility(View.GONE);
            btnManualAttendance.setEnabled(false);
            cardFaceRecognition.setAlpha(0.5f);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}