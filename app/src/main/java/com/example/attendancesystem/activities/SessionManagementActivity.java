package com.example.attendancesystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Session;
import com.example.attendancesystem.models.Teacher;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.Utils;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SessionManagementActivity extends AppCompatActivity {

    private static final String TAG = "SessionManagement";

    // Views
    private TextView tvCurrentSession, tvSessionStatus, tvStudentCount;
    private Button btnStartSession, btnEndSession, btnManualAttendance;
    private CardView cardFaceRecognition;
    private RecyclerView rvCurrentStudents;

    // Data
    private FirebaseManager firebaseManager;
    private Teacher currentTeacher;
    private Session currentSession;
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
        loadTeacherData();
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
        btnStartSession.setOnClickListener(v -> showStartSessionDialog());
        btnEndSession.setOnClickListener(v -> endSession());
        btnManualAttendance.setOnClickListener(v -> openManualAttendance());
        cardFaceRecognition.setOnClickListener(v -> openFaceRecognition());
    }

    private void loadTeacherData() {
        String userEmail = Utils.getSavedUserEmail(this);
        if (userEmail != null) {
            firebaseManager.getTeacherByEmail(userEmail, new FirebaseManager.DataCallback<Teacher>() {
                @Override
                public void onSuccess(Teacher teacher) {
                    currentTeacher = teacher;
                    loadCurrentSession();
                }

                @Override
                public void onFailure(String error) {
                    Utils.showToast(SessionManagementActivity.this, "Erreur: " + error);
                    finish();
                }
            });
        } else {
            Utils.showToast(this, "Erreur: Utilisateur non connecté");
            finish();
        }
    }

    private void loadCurrentSession() {
        if (currentTeacher == null) return;

        // Chercher une session active pour ce professeur
        firebaseManager.getActiveSessionForTeacher(currentTeacher.getEmail(), new FirebaseManager.DataCallback<Session>() {
            @Override
            public void onSuccess(Session session) {
                if (session != null) {
                    currentSession = session;
                    isSessionActive = session.isActive();
                } else {
                    currentSession = null;
                    isSessionActive = false;
                }
                updateUI();
            }

            @Override
            public void onFailure(String error) {
                Utils.showToast(SessionManagementActivity.this, "Erreur lors du chargement de la session: " + error);
                currentSession = null;
                isSessionActive = false;
                updateUI();
            }
        });
    }

    private void showStartSessionDialog() {
        if (currentTeacher == null) return;

        // Obtenir les sessions programmées pour aujourd'hui
        firebaseManager.getTodayScheduledSessionsForTeacher(currentTeacher.getEmail(), new FirebaseManager.DataCallback<List<Session>>() {
            @Override
            public void onSuccess(List<Session> sessions) {
                if (sessions.isEmpty()) {
                    Utils.showToast(SessionManagementActivity.this, "Aucune session programmée pour aujourd'hui");
                    return;
                }

                // Afficher une liste de sessions à démarrer
                String[] sessionNames = new String[sessions.size()];
                for (int i = 0; i < sessions.size(); i++) {
                    Session session = sessions.get(i);
                    sessionNames[i] = session.getCourseName() + " - " +
                            Utils.formatTime(session.getStartTime()) + " - " + session.getRoom();
                }

                new AlertDialog.Builder(SessionManagementActivity.this)
                        .setTitle("Sélectionner une session à démarrer")
                        .setItems(sessionNames, (dialog, which) -> {
                            startSession(sessions.get(which));
                        })
                        .setNegativeButton("Annuler", null)
                        .show();
            }

            @Override
            public void onFailure(String error) {
                Utils.showToast(SessionManagementActivity.this, "Erreur: " + error);
            }
        });
    }

    private void startSession(Session session) {
        if (session == null) return;

        // Démarrer la session
        session.startSession();

        // Sauvegarder dans Firebase
        firebaseManager.updateSession(session, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                currentSession = session;
                isSessionActive = true;
                updateUI();
                Utils.showToast(SessionManagementActivity.this, "Session démarrée: " + session.getCourseName());
            }

            @Override
            public void onFailure(String error) {
                Utils.showToast(SessionManagementActivity.this, "Erreur lors du démarrage: " + error);
            }
        });
    }

    private void endSession() {
        if (currentSession == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Terminer la session")
                .setMessage("Êtes-vous sûr de vouloir terminer la session '" + currentSession.getCourseName() + "' ?")
                .setPositiveButton("Terminer", (dialog, which) -> {
                    // Terminer la session
                    currentSession.endSession();

                    // Sauvegarder dans Firebase
                    firebaseManager.updateSession(currentSession, new FirebaseManager.DataCallback<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            isSessionActive = false;
                            updateUI();
                            Utils.showToast(SessionManagementActivity.this, "Session terminée");

                            // Optionnel: Revenir au dashboard après quelques secondes
                            findViewById(android.R.id.content).postDelayed(() -> finish(), 2000);
                        }

                        @Override
                        public void onFailure(String error) {
                            Utils.showToast(SessionManagementActivity.this, "Erreur lors de la fermeture: " + error);
                        }
                    });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void openManualAttendance() {
        if (currentSession == null) {
            Utils.showToast(this, "Aucune session active");
            return;
        }

        // TODO: Créer l'activité de pointage manuel

    }

    private void openFaceRecognition() {
        if (currentSession == null) {
            Utils.showToast(this, "Aucune session active");
            return;
        }

        // TODO: Implémenter la reconnaissance faciale
        Utils.showToast(this, "Reconnaissance faciale - À implémenter");
    }

    private void updateUI() {
        if (isSessionActive && currentSession != null) {
            // Session active
            tvCurrentSession.setText(currentSession.getCourseName() + " - " + currentSession.getRoom());
            tvSessionStatus.setText("Session Active");
            tvSessionStatus.setTextColor(getColor(R.color.success_color));

            int totalEnrolled = currentSession.getStatistics().getTotalEnrolled();
            int totalPresent = currentSession.getStatistics().getTotalPresent();
            tvStudentCount.setText(totalPresent + " étudiants présents / " + totalEnrolled + " inscrits");

            btnStartSession.setVisibility(View.GONE);
            btnEndSession.setVisibility(View.VISIBLE);
            btnManualAttendance.setEnabled(true);
            cardFaceRecognition.setAlpha(1.0f);
            cardFaceRecognition.setClickable(true);

        } else {
            // Aucune session active
            tvCurrentSession.setText("Aucune session active");
            tvSessionStatus.setText("Session Inactive");
            tvSessionStatus.setTextColor(getColor(R.color.text_secondary));
            tvStudentCount.setText("0 étudiants présents");

            btnStartSession.setVisibility(View.VISIBLE);
            btnEndSession.setVisibility(View.GONE);
            btnManualAttendance.setEnabled(false);
            cardFaceRecognition.setAlpha(0.5f);
            cardFaceRecognition.setClickable(false);
        }

        // Charger la liste des étudiants si une session est active
        if (currentSession != null) {
            loadSessionStudents();
        }
    }

    private void loadSessionStudents() {
        // TODO: Implémenter l'affichage de la liste des étudiants de la session
        // Pour l'instant, on peut simplement afficher le nombre d'étudiants
        if (currentSession != null && currentSession.getStatistics() != null) {
            int enrolled = currentSession.getStatistics().getTotalEnrolled();
            int present = currentSession.getStatistics().getTotalPresent();
            int absent = currentSession.getStatistics().getTotalAbsent();

            String statusText = String.format("Inscrits: %d | Présents: %d | Absents: %d",
                    enrolled, present, absent);

            // Mettre à jour l'affichage si nécessaire
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger la session courante quand on revient sur l'activité
        loadCurrentSession();
    }
}