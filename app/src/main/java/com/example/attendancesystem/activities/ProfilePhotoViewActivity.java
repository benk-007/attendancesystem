package com.example.attendancesystem.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Student;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.Utils;

/**
 * Activité pour consulter la photo utilisée pour la reconnaissance faciale
 * Affiche la photo de profil stockée sur Google Drive
 */
public class ProfilePhotoViewActivity extends AppCompatActivity {

    private static final String TAG = "ProfilePhotoView";

    // Views
    private ImageView ivProfilePhoto;
    private TextView tvPhotoStatus, tvInstructions, tvPhotoPath;
    private ProgressBar progressBar;

    // Data
    private FirebaseManager firebaseManager;
    private Student currentStudent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_photo_view);

        // Configurer la toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Photo de Reconnaissance");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialiser Firebase
        firebaseManager = FirebaseManager.getInstance();

        // Initialiser les views
        initViews();

        // Charger les données utilisateur
        loadUserData();
    }

    private void initViews() {
        ivProfilePhoto = findViewById(R.id.iv_profile_photo_large);
        tvPhotoStatus = findViewById(R.id.tv_photo_status);
        tvInstructions = findViewById(R.id.tv_photo_instructions);
        tvPhotoPath = findViewById(R.id.tv_photo_path);
        progressBar = findViewById(R.id.progress_bar);
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
                displayProfilePhoto();
                showLoading(false);
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
     * Afficher la photo de profil utilisée pour la reconnaissance faciale
     */
    private void displayProfilePhoto() {
        if (currentStudent == null) {
            showError("Données étudiant non disponibles");
            return;
        }

        String photoUrl = currentStudent.getProfileImageUrl();

        if (photoUrl != null && !photoUrl.isEmpty()) {
            // Photo disponible - l'afficher
            tvPhotoStatus.setText("✅ Photo active pour la reconnaissance faciale");
            tvPhotoStatus.setTextColor(getColor(R.color.success_color));

            tvInstructions.setText(
                    "Cette photo est utilisée par le système de reconnaissance faciale pour votre identification automatique lors des pointages.\n\n" +
                            "📝 Conseils pour une meilleure reconnaissance :\n" +
                            "• Visage bien éclairé et visible\n" +
                            "• Expression neutre, regard vers la caméra\n" +
                            "• Éviter les accessoires cachant le visage\n" +
                            "• Photo récente et de bonne qualité"
            );

            // Afficher le chemin de stockage (sécurisé)
            String safePath = extractSafePathInfo(photoUrl);
            tvPhotoPath.setText("Stockage : " + safePath);
            tvPhotoPath.setVisibility(View.VISIBLE);

            // Charger et afficher la photo avec Glide
            Glide.with(this)
                    .load(photoUrl)
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(ivProfilePhoto);

            Log.d(TAG, "Profile photo loaded successfully");

        } else {
            // Aucune photo configurée
            tvPhotoStatus.setText("⚠️ Aucune photo configurée");
            tvPhotoStatus.setTextColor(getColor(R.color.warning_color));

            tvInstructions.setText(
                    "Vous n'avez pas encore configuré de photo pour la reconnaissance faciale.\n\n" +
                            "📸 Pour activer la reconnaissance faciale :\n" +
                            "1. Allez dans 'Mon Profil'\n" +
                            "2. Cliquez sur 'Changer la photo'\n" +
                            "3. Prenez une photo de bonne qualité\n" +
                            "4. La photo sera automatiquement utilisée pour les pointages\n\n" +
                            "⚡ Sans photo, vous devrez utiliser d'autres méthodes de pointage disponibles."
            );

            tvPhotoPath.setVisibility(View.GONE);

            // Afficher l'icône par défaut
            ivProfilePhoto.setImageResource(R.drawable.ic_person);

            Log.d(TAG, "No profile photo configured");
        }
    }

    /**
     * Extraire des informations sécurisées sur le chemin de stockage
     */
    private String extractSafePathInfo(String photoUrl) {
        if (photoUrl == null || photoUrl.isEmpty()) {
            return "Non défini";
        }

        try {
            // Pour Google Drive, extraire l'ID du fichier de manière sécurisée
            if (photoUrl.contains("drive.google.com")) {
                return "Google Drive (Sécurisé)";
            } else if (photoUrl.contains("firebase")) {
                return "Firebase Storage";
            } else {
                return "Stockage Cloud";
            }
        } catch (Exception e) {
            Log.w(TAG, "Error extracting path info: " + e.getMessage());
            return "Stockage Cloud";
        }
    }

    /**
     * Afficher l'état de chargement
     */
    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            ivProfilePhoto.setVisibility(View.GONE);
            tvPhotoStatus.setVisibility(View.GONE);
            tvInstructions.setVisibility(View.GONE);
            tvPhotoPath.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            ivProfilePhoto.setVisibility(View.VISIBLE);
            tvPhotoStatus.setVisibility(View.VISIBLE);
            tvInstructions.setVisibility(View.VISIBLE);
            // tvPhotoPath sera géré dans displayProfilePhoto()
        }
    }

    /**
     * Afficher une erreur
     */
    private void showError(String message) {
        tvPhotoStatus.setText("❌ " + message);
        tvPhotoStatus.setTextColor(getColor(R.color.error_color));
        tvPhotoStatus.setVisibility(View.VISIBLE);

        tvInstructions.setText("Une erreur s'est produite. Veuillez réessayer plus tard.");
        tvInstructions.setVisibility(View.VISIBLE);

        ivProfilePhoto.setImageResource(R.drawable.ic_person);
        ivProfilePhoto.setVisibility(View.VISIBLE);

        tvPhotoPath.setVisibility(View.GONE);

        Utils.showToast(this, message);
        Log.e(TAG, message);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger les données au retour sur l'activité
        if (currentStudent != null) {
            displayProfilePhoto();
        }
    }
}