package com.example.attendancesystem;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.attendancesystem.activities.LoginActivity;
import com.example.attendancesystem.utils.Utils;
import com.example.attendancesystem.services.FirebaseManager;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Vérifier si l'utilisateur est déjà connecté
        FirebaseManager firebaseManager = FirebaseManager.getInstance();

        if (firebaseManager.isUserLoggedIn() && Utils.isUserLoggedIn(this)) {
            // Utilisateur déjà connecté, rediriger vers le dashboard approprié
            redirectToDashboard();
        } else {
            // Utilisateur non connecté, rediriger vers l'écran de connexion
            redirectToLogin();
        }

        finish(); // Fermer MainActivity après redirection
    }

    /**
     * Rediriger vers le dashboard approprié selon le rôle
     */
    private void redirectToDashboard() {
        String userRole = Utils.getSavedUserRole(this);
        Intent intent;

        switch (userRole) {
            case "student":
                intent = new Intent(this, com.example.attendancesystem.activities.StudentDashboardActivity.class);
                break;
            case "teacher":
                intent = new Intent(this, com.example.attendancesystem.activities.TeacherDashboardActivity.class);
                break;
            case "admin":
                intent = new Intent(this, com.example.attendancesystem.activities.AdminDashboardActivity.class);
                break;
            default:
                // Rôle non reconnu, aller à la connexion
                redirectToLogin();
                return;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Rediriger vers l'écran de connexion
     */
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}