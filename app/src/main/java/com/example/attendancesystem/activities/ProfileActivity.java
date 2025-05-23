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

import java.util.Arrays;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    // Views
    private ImageView ivProfilePhoto;
    private TextView tvUserName, tvUserEmail, tvDepartment, tvYear, tvUserId, tvPhone;
    private LinearLayout layoutYear;
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
        layoutYear = findViewById(R.id.layout_year);
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
            layoutYear.setVisibility(View.VISIBLE);

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

            // Update notification preferences
            updateNotificationSwitches(admin.getNotificationPreferences());
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
        Spinner spinnerDepartment = dialogView.findViewById(R.id.spinner_department);
        Spinner spinnerYear = dialogView.findViewById(R.id.spinner_year);
        LinearLayout layoutYearDialog = dialogView.findViewById(R.id.layout_year_dialog);

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
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Modifier le profil")
                .setView(dialogView)
                .setPositiveButton("Sauvegarder", (d, which) -> {
                    saveProfileChanges(etFullName.getText().toString().trim(),
                            etPhoneNumber.getText().toString().trim(),
                            spinnerDepartment.getSelectedItem().toString(),
                            layoutYearDialog.getVisibility() == View.VISIBLE ?
                                    spinnerYear.getSelectedItem().toString() : null);
                })
                .setNegativeButton("Annuler", null)
                .create();

        dialog.show();
    }

    private void saveProfileChanges(String fullName, String phoneNumber, String department, String year) {
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