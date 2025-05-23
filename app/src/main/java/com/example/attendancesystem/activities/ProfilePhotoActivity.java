package com.example.attendancesystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.attendancesystem.utils.Utils;
import com.example.attendancesystem.R;

public class ProfilePhotoActivity extends AppCompatActivity {

    private static final String TAG = "ProfilePhotoActivity";

    // Views
    private TextView tvInstructions;
    private Button btnCapturePhoto, btnSkipForNow;

    private boolean isFirstTime = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.attendancesystem.R.layout.activity_profile_photo);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Photo de Profil");
        }

        // Vérifier si c'est la première fois
        isFirstTime = getIntent().getBooleanExtra("isFirstTime", false);

        // Initialiser les views
        initViews();

        // Configurer les listeners
        setupListeners();
    }

    private void initViews() {
        tvInstructions = findViewById(R.id.tv_instructions);
        btnCapturePhoto = findViewById(R.id.btn_capture_photo);
        btnSkipForNow = findViewById(R.id.btn_skip_for_now);

        if (isFirstTime) {
            tvInstructions.setText("Pour utiliser la reconnaissance faciale, nous avons besoin de votre photo de profil.\n\n" +
                    "Cette photo sera stockée de manière sécurisée sur Google Drive et utilisée uniquement pour le pointage.");
            btnSkipForNow.setText("Passer pour l'instant");
        } else {
            tvInstructions.setText("Mettre à jour votre photo de profil pour améliorer la reconnaissance faciale.");
            btnSkipForNow.setText("Annuler");
        }
    }

    private void setupListeners() {
        btnCapturePhoto.setOnClickListener(v -> {
            // TODO: Implémenter la capture de photo avec caméra
            // La photo sera ensuite uploadée sur Google Drive
            Utils.showToast(this, "Fonctionnalité de capture photo à venir");

            // Pour l'instant, on simule le succès et on redirige
            handlePhotoSuccess();
        });

        btnSkipForNow.setOnClickListener(v -> {
            if (isFirstTime) {
                // Première inscription - rediriger vers le dashboard même sans photo
                Utils.showToast(this, "Vous pourrez ajouter votre photo plus tard dans votre profil");
                redirectToDashboard();
            } else {
                // Retour à l'activité précédente
                finish();
            }
        });
    }

    private void handlePhotoSuccess() {
        Utils.showToast(this, "Photo de profil mise à jour avec succès !");

        if (isFirstTime) {
            redirectToDashboard();
        } else {
            finish();
        }
    }

    private void redirectToDashboard() {
        String role = Utils.getSavedUserRole(this);
        Intent intent;

        switch (role) {
            case "student":
                intent = new Intent(this, StudentDashboardActivity.class);
                break;
            case "teacher":
                intent = new Intent(this, TeacherDashboardActivity.class);
                break;
            case "admin":
                intent = new Intent(this, AdminDashboardActivity.class);
                break;
            default:
                intent = new Intent(this, LoginActivity.class);
                break;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // TODO: Méthodes à implémenter pour Google Drive
    /*
    private void uploadPhotoToGoogleDrive(Uri photoUri) {
        // 1. Authentification Google Drive API
        // 2. Upload de l'image vers Google Drive
        // 3. Récupération de l'URL de partage
        // 4. Mise à jour du profil utilisateur avec l'URL
    }

    private void setupGoogleDriveAPI() {
        // Configuration de l'API Google Drive
        // Authentification OAuth 2.0
    }
    */
}