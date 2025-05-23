package com.example.attendancesystem.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Justification;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.Utils;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class JustificationActivity extends AppCompatActivity {

    private static final String TAG = "JustificationActivity";

    // Views
    private TextInputLayout tilCourse, tilReason;
    private AutoCompleteTextView spinnerCourse;
    private EditText etDescription;
    private AutoCompleteTextView etReason;
    private Button btnSubmit;
    private ProgressBar progressBar;
    private RecyclerView rvJustifications;

    // Data
    private FirebaseManager firebaseManager;
    private String currentUserEmail;
    private List<Justification> justificationsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_justification);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Justification d'Absence");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        firebaseManager = FirebaseManager.getInstance();
        currentUserEmail = Utils.getSavedUserEmail(this);
        justificationsList = new ArrayList<>();

        initViews();
        setupSpinners();
        setupListeners();
        loadJustifications();
    }

    private void initViews() {
        tilCourse = findViewById(R.id.til_course);
        tilReason = findViewById(R.id.til_reason);
        spinnerCourse = findViewById(R.id.spinner_course);
        etReason = findViewById(R.id.et_reason);
        etDescription = findViewById(R.id.et_description);
        btnSubmit = findViewById(R.id.btn_submit);
        progressBar = findViewById(R.id.progress_bar);
        rvJustifications = findViewById(R.id.rv_justifications);

        // Setup RecyclerView
        rvJustifications.setLayoutManager(new LinearLayoutManager(this));

        progressBar.setVisibility(View.GONE);
    }

    private void setupSpinners() {
        // TODO: Charger les cours de l'étudiant depuis Firebase
        String[] courses = {"Mathématiques", "Physique", "Informatique", "Chimie"};
        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, courses);
        spinnerCourse.setAdapter(courseAdapter);

        // Raisons prédéfinies
        String[] reasons = {
                "Maladie", "Rendez-vous médical", "Urgence familiale",
                "Transport", "Problème technique", "Autre"
        };
        ArrayAdapter<String> reasonAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, reasons);
        etReason.setAdapter(reasonAdapter);
    }

    private void setupListeners() {
        btnSubmit.setOnClickListener(v -> submitJustification());
    }

    private void submitJustification() {
        String course = spinnerCourse.getText().toString().trim();
        String reason = etReason.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (!validateInputs(course, reason, description)) {
            return;
        }

        showLoading(true);

        // Créer la justification
        Justification justification = new Justification(
                currentUserEmail,
                Utils.getSavedUserName(this),
                getStudentId(), // TODO: Récupérer depuis le profil
                "COURSE_ID", // TODO: Récupérer l'ID du cours
                course,
                reason,
                description
        );

        // TODO: Sauvegarder dans Firebase
        // firebaseManager.saveJustification(justification, callback);

        // Simulation pour les tests
        showLoading(false);
        Utils.showToast(this, "Justification soumise avec succès");
        clearForm();
        loadJustifications();
    }

    private boolean validateInputs(String course, String reason, String description) {
        if (course.isEmpty()) {
            tilCourse.setError("Veuillez sélectionner un cours");
            return false;
        }
        tilCourse.setError(null);

        if (reason.isEmpty()) {
            tilReason.setError("Veuillez indiquer la raison");
            return false;
        }
        tilReason.setError(null);

        if (description.isEmpty()) {
            etDescription.setError("Veuillez fournir une description");
            return false;
        }
        etDescription.setError(null);

        return true;
    }

    private void clearForm() {
        spinnerCourse.setText("");
        etReason.setText("");
        etDescription.setText("");
    }

    private void loadJustifications() {
        // TODO: Charger les justifications de l'étudiant depuis Firebase
        // Pour l'instant, liste vide
        justificationsList.clear();
        // Adapter à implémenter
    }

    private String getStudentId() {
        // TODO: Récupérer depuis le profil utilisateur
        return "STU001";
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnSubmit.setEnabled(false);
            btnSubmit.setText("Envoi en cours...");
        } else {
            progressBar.setVisibility(View.GONE);
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Soumettre");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}