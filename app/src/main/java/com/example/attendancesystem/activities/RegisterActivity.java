package com.example.attendancesystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.attendancesystem.R;

import com.example.attendancesystem.models.Student;
import com.example.attendancesystem.models.Teacher;
import com.example.attendancesystem.models.Admin;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.Utils;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    // Views
    private EditText etEmail, etPassword, etConfirmPassword, etFullName, etStudentId, etPhoneNumber, etField; // ← NOUVELLE variable

    private Spinner spinnerRole, spinnerDepartment, spinnerYear;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialiser Firebase
        firebaseManager = FirebaseManager.getInstance();

        // Initialiser les views
        initViews();

        // Configurer les spinners
        setupSpinners();

        // Configurer les listeners
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etFullName = findViewById(R.id.et_full_name);
        etStudentId = findViewById(R.id.et_student_id);
        etPhoneNumber = findViewById(R.id.et_phone_number);
        etField = findViewById(R.id.et_field); // ← AJOUTER cette ligne
        spinnerRole = findViewById(R.id.spinner_role);
        spinnerDepartment = findViewById(R.id.spinner_department);
        spinnerYear = findViewById(R.id.spinner_year);
        btnRegister = findViewById(R.id.btn_register);
        tvLogin = findViewById(R.id.tv_login);
        progressBar = findViewById(R.id.progress_bar);

        // Cacher la progress bar
        progressBar.setVisibility(View.GONE);
    }

    private void setupSpinners() {
        // Spinner des rôles
        String[] roles = {"Étudiant", "Enseignant", "Administrateur"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(roleAdapter);

        // Spinner des départements
        String[] departments = {
                "Informatique", "Mathématiques", "Physique", "Chimie",
                "Biologie", "Génie Civil", "Génie Électrique", "Économie", "Gestion"
        };
        ArrayAdapter<String> departmentAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, departments);
        departmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(departmentAdapter);

        // Spinner des années (pour les étudiants)
        String[] years = {"L1", "L2", "L3", "M1", "M2", "Doctorat", "N/A"};
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> performRegistration());

        tvLogin.setOnClickListener(v -> {
            finish(); // Retour à l'écran de connexion
        });

        // Masquer/afficher les champs selon le rôle sélectionné
        spinnerRole.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                boolean isStudent = position == 0; // "Étudiant" est à l'index 0

                // Afficher l'année seulement pour les étudiants
                spinnerYear.setVisibility(isStudent ? View.VISIBLE : View.GONE);

                // Le student ID est requis pour les étudiants, optionnel pour les autres
                if (!isStudent && position != 2) { // Pas admin non plus
                    etStudentId.setHint("ID Employé *");
                } else if (isStudent) {
                    etStudentId.setHint("Numéro étudiant *");
                } else {
                    etStudentId.setHint("ID Administrateur *");
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void performRegistration() {
        // Récupérer les valeurs des champs
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String idNumber = etStudentId.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String field = etField.getText().toString().trim(); // ← AJOUTER cette ligne

        String role = getRoleFromSpinner();
        String department = spinnerDepartment.getSelectedItem().toString();
        String year = spinnerYear.getSelectedItem().toString();

        // Validation des champs
        if (!validateInputs(email, password, confirmPassword, fullName, idNumber, role)) {
            return;
        }

        // Vérifier la connexion réseau
        if (!Utils.isNetworkAvailable(this)) {
            Utils.showToast(this, "Aucune connexion Internet");
            return;
        }

        // Afficher le loading
        showLoading(true);

        // Créer le compte Firebase Auth
        firebaseManager.createUserWithEmailPassword(email, password, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser firebaseUser) {
                // Créer l'objet utilisateur selon son rôle
                saveUserToFirestore(email, fullName, idNumber, phoneNumber, role, department, year, field);
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                String friendlyError = Utils.getAuthErrorMessage(error);
                Utils.showToast(RegisterActivity.this, friendlyError);
            }
        });
    }

    private boolean validateInputs(String email, String password, String confirmPassword,
                                   String fullName, String idNumber, String role) {

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

        if (!Utils.isValidPassword(password)) {
            etPassword.setError("Le mot de passe doit contenir au moins 6 caractères avec lettres et chiffres");
            etPassword.requestFocus();
            return false;
        }

        // Vérifier confirmation mot de passe
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Les mots de passe ne correspondent pas");
            etConfirmPassword.requestFocus();
            return false;
        }

        // Vérifier nom complet
        if (fullName.isEmpty()) {
            etFullName.setError("Nom complet requis");
            etFullName.requestFocus();
            return false;
        }

        if (!Utils.isValidFullName(fullName)) {
            etFullName.setError("Veuillez entrer votre prénom et nom");
            etFullName.requestFocus();
            return false;
        }

        // Vérifier ID
        if (idNumber.isEmpty()) {
            etStudentId.setError("ID requis");
            etStudentId.requestFocus();
            return false;
        }

        if (!Utils.isValidStudentId(idNumber)) {
            etStudentId.setError("ID doit contenir entre 3 et 20 caractères");
            etStudentId.requestFocus();
            return false;
        }

        return true;
    }

    private String getRoleFromSpinner() {
        int position = spinnerRole.getSelectedItemPosition();
        switch (position) {
            case 0: return "student";
            case 1: return "teacher";
            case 2: return "admin";
            default: return "student";
        }
    }

    private void saveUserToFirestore(String email, String fullName, String idNumber,
                                     String phoneNumber, String role, String department, String year, String field) {

        switch (role) {
            case "student":
                Student student = new Student(email, fullName, idNumber, department, year, field);
                student.setPhoneNumber(phoneNumber);

                firebaseManager.saveStudent(student, new FirebaseManager.DataCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        handleRegistrationSuccess(email, role, fullName);
                    }

                    @Override
                    public void onFailure(String error) {
                        handleRegistrationError(error);
                    }
                });
                break;

            case "teacher":
                Teacher teacher = new Teacher(email, fullName, idNumber, department);
                teacher.setPhoneNumber(phoneNumber);

                firebaseManager.saveTeacher(teacher, new FirebaseManager.DataCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        handleRegistrationSuccess(email, role, fullName);
                    }

                    @Override
                    public void onFailure(String error) {
                        handleRegistrationError(error);
                    }
                });
                break;

            case "admin":
                Admin admin = new Admin(email, fullName, idNumber, department);
                admin.setPhoneNumber(phoneNumber);

                firebaseManager.saveAdmin(admin, new FirebaseManager.DataCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        handleRegistrationSuccess(email, role, fullName);
                    }

                    @Override
                    public void onFailure(String error) {
                        handleRegistrationError(error);
                    }
                });
                break;

            default:
                showLoading(false);
                Utils.showToast(this, "Rôle non reconnu");
                break;
        }
    }

    private void handleRegistrationSuccess(String email, String role, String fullName) {
        showLoading(false);

        // Sauvegarder localement (utilise email au lieu d'userId)
        Utils.saveUserData(RegisterActivity.this, email, role, fullName);

        Utils.showToast(RegisterActivity.this, "Inscription réussie !");

        // Rediriger vers la capture de photo de profil (seulement pour les étudiants car ils ont besoin de reconnaissance faciale)
        if ("student".equals(role)) {
            Intent intent = new Intent(RegisterActivity.this, ProfilePhotoActivity.class);
            intent.putExtra("isFirstTime", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            // Pour les enseignants et admins, aller directement au dashboard
            redirectToDashboard(role);
        }
        finish();
    }

    private void handleRegistrationError(String error) {
        showLoading(false);
        Utils.showToast(RegisterActivity.this, "Erreur lors de l'inscription: " + error);

        // Supprimer le compte Auth en cas d'erreur Firestore
        FirebaseUser currentUser = firebaseManager.getCurrentUser();
        if (currentUser != null) {
            currentUser.delete();
        }
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
                intent = new Intent(this, LoginActivity.class);
                break;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnRegister.setEnabled(false);
            btnRegister.setText("Inscription en cours...");
        } else {
            progressBar.setVisibility(View.GONE);
            btnRegister.setEnabled(true);
            btnRegister.setText("S'inscrire");
        }
    }
}