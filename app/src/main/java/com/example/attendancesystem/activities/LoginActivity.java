package com.example.attendancesystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.attendancesystem.models.Student;
import com.example.attendancesystem.models.Teacher;
import com.example.attendancesystem.models.Admin;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.Utils;
import com.example.attendancesystem.R;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // Views
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.attendancesystem.R.layout.activity_login);

        // Initialiser Firebase
        firebaseManager = FirebaseManager.getInstance();

        // Vérifier si l'utilisateur est déjà connecté
        if (firebaseManager.isUserLoggedIn() && Utils.isUserLoggedIn(this)) {
            redirectToDashboard();
            return;
        }

        // Initialiser les views
        initViews();

        // Configurer les listeners
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        progressBar = findViewById(R.id.progress_bar);

        // Cacher la progress bar
        progressBar.setVisibility(View.GONE);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> performLogin());

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        tvForgotPassword.setOnClickListener(v -> {
            // TODO: Implémenter la récupération de mot de passe
            Utils.showToast(this, "Fonctionnalité à venir");
        });
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validation des champs
        if (!validateInputs(email, password)) {
            return;
        }

        // Vérifier la connexion réseau
        if (!Utils.isNetworkAvailable(this)) {
            Utils.showToast(this, "Aucune connexion Internet");
            return;
        }

        // Afficher le loading
        showLoading(true);

        // Authentification avec Firebase
        firebaseManager.signInWithEmailPassword(email, password, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                // Récupérer les données utilisateur depuis Firestore selon le nouveau système
                loadUserData(user.getEmail());
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                String friendlyError = Utils.getAuthErrorMessage(error);
                Utils.showToast(LoginActivity.this, friendlyError);
            }
        });
    }

    private boolean validateInputs(String email, String password) {
        // Vérifier email
        if (email.isEmpty()) {
            etEmail.setError("Email requis");
            etEmail.requestFocus();
            return false;
        }

        if (!Utils.isValidEmail(email)) {
            etEmail.setError("Format d'email invalide");
            etEmail.requestFocus();
            return false;
        }

        // Vérifier mot de passe
        if (password.isEmpty()) {
            etPassword.setError("Mot de passe requis");
            etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Le mot de passe doit contenir au moins 6 caractères");
            etPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void loadUserData(String email) {
        // Utiliser la nouvelle méthode pour détecter automatiquement le type d'utilisateur
        firebaseManager.getUserByEmail(email, new FirebaseManager.DataCallback<Object>() {
            @Override
            public void onSuccess(Object userData) {
                showLoading(false);

                String role;
                String fullName;

                // Déterminer le type d'utilisateur et extraire les données
                if (userData instanceof Student) {
                    Student student = (Student) userData;
                    role = "student";
                    fullName = student.getFullName();
                } else if (userData instanceof Teacher) {
                    Teacher teacher = (Teacher) userData;
                    role = "teacher";
                    fullName = teacher.getFullName();
                } else if (userData instanceof Admin) {
                    Admin admin = (Admin) userData;
                    role = "admin";
                    fullName = admin.getFullName();
                } else {
                    Utils.showToast(LoginActivity.this, "Type d'utilisateur non reconnu");
                    firebaseManager.signOut();
                    return;
                }

                // Sauvegarder les données utilisateur localement (utilise email au lieu d'userId)
                Utils.saveUserData(LoginActivity.this, email, role, fullName);

                // Rediriger vers le dashboard approprié
                redirectToDashboard(role);
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Utils.showToast(LoginActivity.this, "Erreur lors du chargement du profil: " + error);

                // Déconnecter l'utilisateur en cas d'erreur
                firebaseManager.signOut();
            }
        });
    }

    private void redirectToDashboard() {
        String role = Utils.getSavedUserRole(this);
        redirectToDashboard(role);
    }

    private void redirectToDashboard(String role) {
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
                Utils.showToast(this, "Rôle utilisateur non reconnu");
                return;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
            btnLogin.setText("Connexion...");
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            btnLogin.setText("Se connecter");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Vérifier si l'utilisateur est déjà connecté
        if (firebaseManager.isUserLoggedIn() && Utils.isUserLoggedIn(this)) {
            redirectToDashboard();
        }
    }
}