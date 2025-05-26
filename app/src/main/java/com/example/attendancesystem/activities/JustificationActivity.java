package com.example.attendancesystem.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView; // Added for date selection

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendancesystem.R;
import com.example.attendancesystem.utils.JustificationAdapter; // Assuming you have this adapter
import com.example.attendancesystem.models.Justification;
import com.example.attendancesystem.models.Student; // Import Student model
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.Utils;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date; // For justification date
import java.util.List;
import java.util.Locale;
import java.util.Map; // For studentCourses data

public class JustificationActivity extends AppCompatActivity {

    private static final String TAG = "JustificationActivity";

    // Views
    private TextInputLayout tilCourse, tilReason, tilJustificationDate;
    private AutoCompleteTextView spinnerCourse;
    private EditText etDescription;
    private AutoCompleteTextView etReason;
    private Button btnSubmit;
    private ProgressBar progressBar;
    private RecyclerView rvJustifications;
    private TextView tvJustificationDate; // TextView to display selected date

    // Data
    private FirebaseManager firebaseManager;
    private String currentUserEmail;
    private Student currentStudent; // To hold current student data
    private List<Justification> justificationsList;
    private List<Map<String, String>> studentCourses; // Stores courseId and courseName
    private JustificationAdapter justificationAdapter;

    // Calendar to hold the selected justification date
    private Calendar selectedDateCalendar;

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
        studentCourses = new ArrayList<>();
        selectedDateCalendar = Calendar.getInstance(); // Initialize with current date

        initViews();
        setupSpinners();
        setupListeners();
        loadStudentData(); // Load student details and courses
    }

    private void initViews() {
        tilCourse = findViewById(R.id.til_course);
        tilReason = findViewById(R.id.til_reason);
        tilJustificationDate = findViewById(R.id.til_justification_date); // NEW: TextInputLayout for date
        tvJustificationDate = findViewById(R.id.tv_justification_date);   // NEW: TextView to show date
        spinnerCourse = findViewById(R.id.spinner_course);
        etReason = findViewById(R.id.et_reason);
        etDescription = findViewById(R.id.et_description);
        btnSubmit = findViewById(R.id.btn_submit);
        progressBar = findViewById(R.id.progress_bar);
        rvJustifications = findViewById(R.id.rv_justifications);

        // Setup RecyclerView
        justificationAdapter = new JustificationAdapter(justificationsList, false); // false for student view
        rvJustifications.setLayoutManager(new LinearLayoutManager(this));
        rvJustifications.setAdapter(justificationAdapter);

        progressBar.setVisibility(View.GONE);

        // Set initial date display
        updateDateTextView();
    }

    private void setupSpinners() {
        // Raisons prédéfinies
        String[] reasons = {
                "Maladie", "Rendez-vous médical", "Urgence familiale",
                "Transport", "Problème technique", "Convocation administrative", "Autre"
        };
        ArrayAdapter<String> reasonAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, reasons);
        etReason.setAdapter(reasonAdapter);
    }

    private void setupListeners() {
        btnSubmit.setOnClickListener(v -> submitJustification());
        tvJustificationDate.setOnClickListener(v -> showDatePickerDialog()); // Listen for date TextView click
    }

    private void showDatePickerDialog() {
        int year = selectedDateCalendar.get(Calendar.YEAR);
        int month = selectedDateCalendar.get(Calendar.MONTH);
        int day = selectedDateCalendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    selectedDateCalendar.set(selectedYear, selectedMonth, selectedDay);
                    updateDateTextView(); // Update the TextView with the new date
                },
                year, month, day);

        // Optional: Set max date to today if justifications cannot be for future dates
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void updateDateTextView() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvJustificationDate.setText(sdf.format(selectedDateCalendar.getTime()));
    }

    private void loadStudentData() {
        if (currentUserEmail == null) {
            Utils.showToast(this, "Erreur: Utilisateur non connecté");
            finish();
            return;
        }

        showLoading(true);
        firebaseManager.getStudentByEmail(currentUserEmail, new FirebaseManager.DataCallback<Student>() {
            @Override
            public void onSuccess(Student student) {
                currentStudent = student;
                Log.d(TAG, "Student loaded: " + student.toString());
                loadStudentCourses(); // Load courses only after student data is available
                loadJustifications(); // Load existing justifications for the student
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Error loading student data: " + error);
                Utils.showToast(JustificationActivity.this, "Erreur de chargement des données étudiant: " + error);
                showLoading(false);
            }
        });
    }

    private void loadStudentCourses() {
        if (currentStudent == null) {
            Utils.showToast(this, "Student data not available to load courses.");
            return;
        }

        // Use the FirebaseManager method to get student's courses based on their academic details
        firebaseManager.getStudentCourses(
                currentStudent.getEmail(), // You might not need email for course query, but passing for consistency
                currentStudent.getDepartment(),
                currentStudent.getField(),
                currentStudent.getYear(),
                new FirebaseManager.DataCallback<List<Map<String, String>>>() {
                    @Override
                    public void onSuccess(List<Map<String, String>> courses) {
                        studentCourses = courses;
                        setupCourseSpinner();
                        Log.d(TAG, "Loaded " + courses.size() + " courses for student.");
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error loading student courses: " + error);
                        Utils.showToast(JustificationActivity.this, "Erreur de chargement des cours: " + error);
                    }
                });
    }

    private void setupCourseSpinner() {
        if (studentCourses.isEmpty()) {
            String[] defaultCourses = {"Aucun cours trouvé"};
            ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, defaultCourses);
            spinnerCourse.setAdapter(courseAdapter);
            return;
        }

        List<String> courseNames = new ArrayList<>();
        for (Map<String, String> course : studentCourses) {
            courseNames.add(course.get("name"));
        }

        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, courseNames);
        spinnerCourse.setAdapter(courseAdapter);
    }


    private void submitJustification() {
        String courseName = spinnerCourse.getText().toString().trim();
        String reason = etReason.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        Date justificationDate = selectedDateCalendar.getTime(); // Get the selected date

        if (!validateInputs(courseName, reason, description)) {
            return;
        }

        if (currentStudent == null) {
            Utils.showToast(this, "Erreur: Données étudiant non chargées. Veuillez réessayer.");
            return;
        }

        showLoading(true);

        // Find course ID from course name
        String courseId = findCourseId(courseName);
        if (courseId == null) {
            Utils.showToast(this, "Erreur: Cours sélectionné non trouvé.");
            showLoading(false);
            return;
        }

        // Create the Justification object with the chosen date
        Justification justification = new Justification(
                currentUserEmail,
                currentStudent.getFullName(),
                currentStudent.getStudentId(),
                courseId,
                courseName,
                justificationDate, // Pass the selected date here
                reason,
                description
        );

        // Save to Firebase
        firebaseManager.saveJustification(justification, new FirebaseManager.DataCallback<String>() {
            @Override
            public void onSuccess(String justificationId) {
                Log.d(TAG, "Justification saved with ID: " + justificationId);
                showLoading(false);
                Utils.showToast(JustificationActivity.this, "Justification soumise avec succès !");
                clearForm();
                loadJustifications(); // Refresh the list of submitted justifications
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Error saving justification: " + error);
                showLoading(false);
                Utils.showToast(JustificationActivity.this, "Erreur lors de la soumission: " + error);
            }
        });
    }

    private String findCourseId(String courseName) {
        for (Map<String, String> course : studentCourses) {
            if (courseName.equals(course.get("name"))) {
                return course.get("id");
            }
        }
        return null;
    }

    private boolean validateInputs(String course, String reason, String description) {
        if (course.isEmpty() || "Aucun cours trouvé".equals(course)) {
            tilCourse.setError("Veuillez sélectionner un cours.");
            return false;
        }
        tilCourse.setError(null);

        // Check if the date is current or past (future dates are typically not justifiable absences)
        if (selectedDateCalendar.getTime().after(new Date())) {
            tilJustificationDate.setError("La date de justification ne peut pas être dans le futur.");
            return false;
        }
        tilJustificationDate.setError(null);


        if (reason.isEmpty()) {
            tilReason.setError("Veuillez indiquer la raison.");
            return false;
        }
        tilReason.setError(null);

        if (description.isEmpty()) {
            etDescription.setError("Veuillez fournir une description.");
            return false;
        }
        etDescription.setError(null);

        if (description.length() < 10) { // Add a minimum length for description
            etDescription.setError("Description trop courte (minimum 10 caractères).");
            return false;
        }
        etDescription.setError(null);

        return true;
    }

    private void clearForm() {
        spinnerCourse.setText("", false); // Use false to not trigger a dropdown
        etReason.setText("", false);      // Use false to not trigger a dropdown
        etDescription.setText("");
        selectedDateCalendar = Calendar.getInstance(); // Reset date to current
        updateDateTextView();
    }

    private void loadJustifications() {
        if (currentUserEmail == null) {
            Log.e(TAG, "Cannot load justifications: currentUserEmail is null.");
            return;
        }
        showLoading(true); // Show loading while fetching justifications

        firebaseManager.getStudentJustifications(currentUserEmail, new FirebaseManager.DataCallback<List<Justification>>() {
            @Override
            public void onSuccess(List<Justification> justifications) {
                Log.d(TAG, "Loaded " + justifications.size() + " justifications for " + currentUserEmail);
                justificationsList.clear();
                justificationsList.addAll(justifications);
                justificationAdapter.notifyDataSetChanged();
                showLoading(false); // Hide loading
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Error loading justifications: " + error);
                Utils.showToast(JustificationActivity.this, "Erreur de chargement des justifications: " + error);
                showLoading(false); // Hide loading
            }
        });
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
        // Also disable/enable other input fields when loading
        tilCourse.setEnabled(!show);
        tilReason.setEnabled(!show);
        tilJustificationDate.setEnabled(!show);
        etDescription.setEnabled(!show);
        spinnerCourse.setEnabled(!show);
        etReason.setEnabled(!show);
        tvJustificationDate.setEnabled(!show);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload justifications when the activity resumes, in case a justification was processed elsewhere
        if (currentStudent != null) { // Only load if student data is already loaded
            loadJustifications();
        } else {
            loadStudentData(); // Otherwise, load student data and then justifications
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
