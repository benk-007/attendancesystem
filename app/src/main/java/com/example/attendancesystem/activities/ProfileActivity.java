package com.example.attendancesystem.activities;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Student;
import com.example.attendancesystem.models.Teacher;
import com.example.attendancesystem.models.Admin;
import com.example.attendancesystem.models.NotificationPreferences;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.Utils;
import com.google.firebase.Timestamp;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import de.hdodenhof.circleimageview.CircleImageView;
import android.widget.LinearLayout;

import java.util.Arrays;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    // Views
    private CircleImageView ivProfilePhoto;
    private TextView tvUserName, tvUserEmail, tvDepartment, tvYear, tvUserId, tvPhone, tvField; // ← AJOUTER tvField
    private LinearLayout layoutYear, layoutField; // ← AJOUTER layoutField
    private Button btnChangePhoto, btnEditProfile, btnChangePassword;
    private Switch switchAttendanceAlerts, switchCourseReminders, switchAbsenceAlerts;
    private ProgressBar progressBar;

    // Data
    private FirebaseManager firebaseManager;
    private Object currentUser; // Can be Student, Teacher, or Admin
    private String userRole;
    private String userEmail;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mon Profil");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        firebaseManager = FirebaseManager.getInstance();

        // Get user info from intent or saved preferences
        userEmail = getIntent().getStringExtra("userEmail");
        userRole = getIntent().getStringExtra("userRole");

        if (userEmail == null) {
            userEmail = Utils.getSavedUserEmail(this);
        }
        if (userRole == null) {
            userRole = Utils.getSavedUserRole(this);
        }

        initViews();
        setupListeners();
        loadUserProfile();
    }

    private void initViews() {
        ivProfilePhoto = findViewById(R.id.iv_profile_photo);
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserEmail = findViewById(R.id.tv_user_email);
        tvDepartment = findViewById(R.id.tv_department);
        tvYear = findViewById(R.id.tv_year);
        tvUserId = findViewById(R.id.tv_user_id);
        tvPhone = findViewById(R.id.tv_phone);
        tvField = findViewById(R.id.tv_field);  // ← AJOUTER cette ligne
        layoutYear = findViewById(R.id.layout_year);
        layoutField = findViewById(R.id.layout_field);  // ← AJOUTER cette ligne
        btnChangePhoto = findViewById(R.id.btn_change_photo);
        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnChangePassword = findViewById(R.id.btn_change_password);

        // Notification switches
        switchAttendanceAlerts = findViewById(R.id.switch_attendance_alerts);
        switchCourseReminders = findViewById(R.id.switch_course_reminders);
        switchAbsenceAlerts = findViewById(R.id.switch_absence_alerts);

        progressBar = new ProgressBar(this); // We'll add this programmatically if needed
    }

    private void setupListeners() {
        btnChangePhoto.setOnClickListener(v -> changeProfilePhoto());
        btnEditProfile.setOnClickListener(v -> toggleEditMode());
        btnChangePassword.setOnClickListener(v -> changePassword());

        // Notification preferences listeners
        switchAttendanceAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> updateNotificationPreference("attendanceAlerts", isChecked));
        switchCourseReminders.setOnCheckedChangeListener((buttonView, isChecked) -> updateNotificationPreference("courseReminders", isChecked));
        switchAbsenceAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> updateNotificationPreference("absenceThresholdAlerts", isChecked));
    }

    private void loadUserProfile() {
        if (userEmail == null) {
            Utils.showToast(this, "Erreur: Impossible de charger le profil");
            finish();
            return;
        }

        showLoading(true);

        // Load user based on role
        switch (userRole) {
            case "student":
                firebaseManager.getStudentByEmail(userEmail, new FirebaseManager.DataCallback<Student>() {
                    @Override
                    public void onSuccess(Student student) {
                        currentUser = student;
                        updateUI();
                        showLoading(false);
                    }

                    @Override
                    public void onFailure(String error) {
                        showLoading(false);
                        Utils.showToast(ProfileActivity.this, "Erreur: " + error);
                    }
                });
                break;

            case "teacher":
                firebaseManager.getTeacherByEmail(userEmail, new FirebaseManager.DataCallback<Teacher>() {
                    @Override
                    public void onSuccess(Teacher teacher) {
                        currentUser = teacher;
                        updateUI();
                        showLoading(false);
                    }

                    @Override
                    public void onFailure(String error) {
                        showLoading(false);
                        Utils.showToast(ProfileActivity.this, "Erreur: " + error);
                    }
                });
                break;

            case "admin":
                firebaseManager.getAdminByEmail(userEmail, new FirebaseManager.DataCallback<Admin>() {
                    @Override
                    public void onSuccess(Admin admin) {
                        currentUser = admin;
                        updateUI();
                        showLoading(false);
                    }

                    @Override
                    public void onFailure(String error) {
                        showLoading(false);
                        Utils.showToast(ProfileActivity.this, "Erreur: " + error);
                    }
                });
                break;

            default:
                showLoading(false);
                Utils.showToast(this, "Type d'utilisateur non reconnu");
                finish();
                break;
        }
    }

    private void updateUI() {
        if (currentUser == null) return;

        if (currentUser instanceof Student) {
            Student student = (Student) currentUser;
            tvUserName.setText(student.getFullName());
            tvUserEmail.setText(student.getEmail());
            tvDepartment.setText(student.getDepartment());
            tvYear.setText(student.getYear());
            tvUserId.setText(student.getStudentId());
            tvPhone.setText(student.getPhoneNumber() != null ? student.getPhoneNumber() : "Non renseigné");
            // AJOUTER ces lignes pour le champ field
            tvField.setText(student.getField() != null ? student.getField() : "Non défini");
            layoutYear.setVisibility(View.VISIBLE);
            layoutField.setVisibility(View.VISIBLE);

            // AJOUTER la gestion de la photo de profil
            loadProfileImage(student.getProfileImageUrl());

            // Update notification preferences
            updateNotificationSwitches(student.getNotificationPreferences());

        } else if (currentUser instanceof Teacher) {
            Teacher teacher = (Teacher) currentUser;
            tvUserName.setText(teacher.getFullName());
            tvUserEmail.setText(teacher.getEmail());
            tvDepartment.setText(teacher.getDepartment());
            tvUserId.setText(teacher.getEmployeeId());
            tvPhone.setText(teacher.getPhoneNumber() != null ? teacher.getPhoneNumber() : "Non renseigné");

            layoutYear.setVisibility(View.GONE);
            layoutField.setVisibility(View.GONE);  // ← AJOUTER cette ligne

            // AJOUTER la gestion de la photo de profil
            loadProfileImage(teacher.getProfileImageUrl());

            // Update notification preferences
            updateNotificationSwitches(teacher.getNotificationPreferences());

        } else if (currentUser instanceof Admin) {
            Admin admin = (Admin) currentUser;
            tvUserName.setText(admin.getFullName());
            tvUserEmail.setText(admin.getEmail());
            tvDepartment.setText(admin.getDepartment());
            tvUserId.setText(admin.getAdminId());
            tvPhone.setText(admin.getPhoneNumber() != null ? admin.getPhoneNumber() : "Non renseigné");
            layoutYear.setVisibility(View.GONE);
            layoutField.setVisibility(View.GONE);  // ← AJOUTER cette ligne

            // AJOUTER la gestion de la photo de profil
            loadProfileImage(admin.getProfileImageUrl());

            // Update notification preferences
            updateNotificationSwitches(admin.getNotificationPreferences());
        }
    }

    // 5. AJOUTER cette nouvelle méthode pour charger la photo de profil
    private void loadProfileImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(ivProfilePhoto);
        } else {
            ivProfilePhoto.setImageResource(R.drawable.ic_person);
        }
    }

    private void updateNotificationSwitches(NotificationPreferences preferences) {
        if (preferences != null) {
            switchAttendanceAlerts.setChecked(preferences.isAttendanceAlerts());
            switchCourseReminders.setChecked(preferences.isCourseReminders());
            switchAbsenceAlerts.setChecked(preferences.isAbsenceThresholdAlerts());
        }
    }

    private void updateNotificationPreference(String preferenceType, boolean isEnabled) {
        if (currentUser == null) return;

        NotificationPreferences preferences = null;

        if (currentUser instanceof Student) {
            preferences = ((Student) currentUser).getNotificationPreferences();
        } else if (currentUser instanceof Teacher) {
            preferences = ((Teacher) currentUser).getNotificationPreferences();
        } else if (currentUser instanceof Admin) {
            preferences = ((Admin) currentUser).getNotificationPreferences();
        }

        if (preferences == null) {
            preferences = new NotificationPreferences();
        }

        // Update the specific preference
        switch (preferenceType) {
            case "attendanceAlerts":
                preferences.setAttendanceAlerts(isEnabled);
                break;
            case "courseReminders":
                preferences.setCourseReminders(isEnabled);
                break;
            case "absenceThresholdAlerts":
                preferences.setAbsenceThresholdAlerts(isEnabled);
                break;
        }

        // Save back to the user object
        if (currentUser instanceof Student) {
            ((Student) currentUser).setNotificationPreferences(preferences);
            ((Student) currentUser).setLastUpdatedAt(Timestamp.now());

            firebaseManager.updateStudent((Student) currentUser, new FirebaseManager.DataCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Utils.showToast(ProfileActivity.this, "Préférences mises à jour");
                }

                @Override
                public void onFailure(String error) {
                    Utils.showToast(ProfileActivity.this, "Erreur: " + error);
                    // Revert the switch state
                    loadUserProfile();
                }
            });

        } else if (currentUser instanceof Teacher) {
            ((Teacher) currentUser).setNotificationPreferences(preferences);
            ((Teacher) currentUser).setLastUpdatedAt(Timestamp.now());

            firebaseManager.updateTeacher((Teacher) currentUser, new FirebaseManager.DataCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Utils.showToast(ProfileActivity.this, "Préférences mises à jour");
                }

                @Override
                public void onFailure(String error) {
                    Utils.showToast(ProfileActivity.this, "Erreur: " + error);
                    loadUserProfile();
                }
            });

        } else if (currentUser instanceof Admin) {
            ((Admin) currentUser).setNotificationPreferences(preferences);
            ((Admin) currentUser).setLastUpdatedAt(Timestamp.now());

            firebaseManager.updateAdmin((Admin) currentUser, new FirebaseManager.DataCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Utils.showToast(ProfileActivity.this, "Préférences mises à jour");
                }

                @Override
                public void onFailure(String error) {
                    Utils.showToast(ProfileActivity.this, "Erreur: " + error);
                    loadUserProfile();
                }
            });
        }
    }

    private void toggleEditMode() {
        if (!isEditMode) {
            showEditDialog();
        }
    }

    private void showEditDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);

        // Initialize dialog views
        EditText etFullName = dialogView.findViewById(R.id.et_full_name);
        EditText etPhoneNumber = dialogView.findViewById(R.id.et_phone_number);
        EditText etField = dialogView.findViewById(R.id.et_field);  // ← AJOUTER cette ligne
        Spinner spinnerDepartment = dialogView.findViewById(R.id.spinner_department);
        Spinner spinnerYear = dialogView.findViewById(R.id.spinner_year);
        LinearLayout layoutYearDialog = dialogView.findViewById(R.id.layout_year_dialog);
        LinearLayout layoutFieldDialog = dialogView.findViewById(R.id.layout_field_dialog);  // ← AJOUTER cette ligne

        // Setup department spinner
        String[] departments = getResources().getStringArray(R.array.departments);
        ArrayAdapter<String> departmentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, departments);
        departmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(departmentAdapter);

        // Setup year spinner (only for students)
        String[] years = getResources().getStringArray(R.array.academic_years);
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        // Fill current values
        if (currentUser instanceof Student) {
            Student student = (Student) currentUser;
            etFullName.setText(student.getFullName());
            etPhoneNumber.setText(student.getPhoneNumber());
            etField.setText(student.getField());  // ← AJOUTER cette ligne

            // Set selected department
            String currentDept = student.getDepartment();
            for (int i = 0; i < departments.length; i++) {
                if (departments[i].equals(currentDept)) {
                    spinnerDepartment.setSelection(i);
                    break;
                }
            }

            // Set selected year
            String currentYear = student.getYear();
            for (int i = 0; i < years.length; i++) {
                if (years[i].equals(currentYear)) {
                    spinnerYear.setSelection(i);
                    break;
                }
            }

            layoutYearDialog.setVisibility(View.VISIBLE);
            layoutFieldDialog.setVisibility(View.VISIBLE);  // ← AJOUTER cette ligne

        } else if (currentUser instanceof Teacher) {
            Teacher teacher = (Teacher) currentUser;
            etFullName.setText(teacher.getFullName());
            etPhoneNumber.setText(teacher.getPhoneNumber());

            String currentDept = teacher.getDepartment();
            for (int i = 0; i < departments.length; i++) {
                if (departments[i].equals(currentDept)) {
                    spinnerDepartment.setSelection(i);
                    break;
                }
            }

            layoutYearDialog.setVisibility(View.GONE);
            layoutFieldDialog.setVisibility(View.GONE);  // ← AJOUTER cette ligne

        } else if (currentUser instanceof Admin) {
            Admin admin = (Admin) currentUser;
            etFullName.setText(admin.getFullName());
            etPhoneNumber.setText(admin.getPhoneNumber());

            String currentDept = admin.getDepartment();
            for (int i = 0; i < departments.length; i++) {
                if (departments[i].equals(currentDept)) {
                    spinnerDepartment.setSelection(i);
                    break;
                }
            }

            layoutYearDialog.setVisibility(View.GONE);
            layoutFieldDialog.setVisibility(View.GONE);  // ← AJOUTER cette ligne
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Modifier le profil")
                .setView(dialogView)
                .setPositiveButton("Sauvegarder", (d, which) -> {
                    saveProfileChanges(etFullName.getText().toString().trim(),
                            etPhoneNumber.getText().toString().trim(),
                            spinnerDepartment.getSelectedItem().toString(),
                            layoutYearDialog.getVisibility() == View.VISIBLE ?
                                    spinnerYear.getSelectedItem().toString() : null,
                            layoutFieldDialog.getVisibility() == View.VISIBLE ?  // ← AJOUTER ce paramètre
                                    etField.getText().toString().trim() : null);
                })
                .setNegativeButton("Annuler", null)
                .create();

        dialog.show();
    }

    private void saveProfileChanges(String fullName, String phoneNumber, String department, String year, String field) {
        if (fullName.isEmpty()) {
            Utils.showToast(this, "Le nom complet est requis");
            return;
        }

        showLoading(true);

        if (currentUser instanceof Student) {
            Student student = (Student) currentUser;
            student.setFullName(fullName);
            student.setPhoneNumber(phoneNumber);
            student.setDepartment(department);
            if (year != null) {
                student.setYear(year);
            }
            if (field != null) {  // ← AJOUTER cette condition
                student.setField(field);
            }
            student.setLastUpdatedAt(Timestamp.now());

            firebaseManager.updateStudent(student, new FirebaseManager.DataCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    showLoading(false);
                    Utils.showToast(ProfileActivity.this, "Profil mis à jour avec succès");
                    updateUI();

                    // Update saved user name
                    Utils.saveUserData(ProfileActivity.this, student.getEmail(), "student", student.getFullName());
                }

                @Override
                public void onFailure(String error) {
                    showLoading(false);
                    Utils.showToast(ProfileActivity.this, "Erreur: " + error);
                }
            });

        } else if (currentUser instanceof Teacher) {
            Teacher teacher = (Teacher) currentUser;
            teacher.setFullName(fullName);
            teacher.setPhoneNumber(phoneNumber);
            teacher.setDepartment(department);
            teacher.setLastUpdatedAt(Timestamp.now());

            firebaseManager.updateTeacher(teacher, new FirebaseManager.DataCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    showLoading(false);
                    Utils.showToast(ProfileActivity.this, "Profil mis à jour avec succès");
                    updateUI();

                    // Update saved user name
                    Utils.saveUserData(ProfileActivity.this, teacher.getEmail(), "teacher", teacher.getFullName());
                }

                @Override
                public void onFailure(String error) {
                    showLoading(false);
                    Utils.showToast(ProfileActivity.this, "Erreur: " + error);
                }
            });

        } else if (currentUser instanceof Admin) {
            Admin admin = (Admin) currentUser;
            admin.setFullName(fullName);
            admin.setPhoneNumber(phoneNumber);
            admin.setDepartment(department);
            admin.setLastUpdatedAt(Timestamp.now());

            firebaseManager.updateAdmin(admin, new FirebaseManager.DataCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    showLoading(false);
                    Utils.showToast(ProfileActivity.this, "Profil mis à jour avec succès");
                    updateUI();

                    // Update saved user name
                    Utils.saveUserData(ProfileActivity.this, admin.getEmail(), "admin", admin.getFullName());
                }

                @Override
                public void onFailure(String error) {
                    showLoading(false);
                    Utils.showToast(ProfileActivity.this, "Erreur: " + error);
                }
            });
        }
    }

    private void changeProfilePhoto() {
        // Navigate to ProfilePhotoActivity
        Intent intent = new Intent(this, ProfilePhotoActivity.class);
        intent.putExtra("isFirstTime", false);
        startActivity(intent);
    }

    private void changePassword() {
        showChangePasswordDialog();
    }

    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);

        EditText etCurrentPassword = dialogView.findViewById(R.id.et_current_password);
        EditText etNewPassword = dialogView.findViewById(R.id.et_new_password);
        EditText etConfirmPassword = dialogView.findViewById(R.id.et_confirm_password);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Changer le mot de passe")
                .setView(dialogView)
                .setPositiveButton("Changer", (d, which) -> {
                    String currentPassword = etCurrentPassword.getText().toString().trim();
                    String newPassword = etNewPassword.getText().toString().trim();
                    String confirmPassword = etConfirmPassword.getText().toString().trim();

                    if (validatePasswordChange(currentPassword, newPassword, confirmPassword)) {
                        performPasswordChange(currentPassword, newPassword);
                    }
                })
                .setNegativeButton("Annuler", null)
                .create();

        dialog.show();
    }

    private boolean validatePasswordChange(String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword.isEmpty()) {
            Utils.showToast(this, "Mot de passe actuel requis");
            return false;
        }

        if (newPassword.isEmpty()) {
            Utils.showToast(this, "Nouveau mot de passe requis");
            return false;
        }

        if (!Utils.isValidPassword(newPassword)) {
            Utils.showToast(this, "Le nouveau mot de passe doit contenir au moins 6 caractères avec lettres et chiffres");
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            Utils.showToast(this, "Les mots de passe ne correspondent pas");
            return false;
        }

        return true;
    }

    private void performPasswordChange(String currentPassword, String newPassword) {
        showLoading(true);

        // TODO: Implement password change with Firebase Auth
        // This requires re-authentication with current password first
        Utils.showToast(this, "Fonctionnalité de changement de mot de passe à implémenter");
        showLoading(false);

        /*
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                        showLoading(false);
                        if (updateTask.isSuccessful()) {
                            Utils.showToast(this, "Mot de passe changé avec succès");
                        } else {
                            Utils.showToast(this, "Erreur lors du changement de mot de passe");
                        }
                    });
                } else {
                    showLoading(false);
                    Utils.showToast(this, "Mot de passe actuel incorrect");
                }
            });
        }
        */
    }

    private void showLoading(boolean show) {
        // Simple loading indication - you could enhance this with a proper loading overlay
        btnEditProfile.setEnabled(!show);
        btnChangePassword.setEnabled(!show);
        btnChangePhoto.setEnabled(!show);

        if (show) {
            btnEditProfile.setText("Chargement...");
        } else {
            btnEditProfile.setText("Modifier le profil");
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
        // Reload profile when returning from ProfilePhotoActivity
        if (currentUser != null) {
            loadUserProfile();
        }
    }
}