package com.example.attendancesystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Student;
import com.example.attendancesystem.models.Teacher;
import com.example.attendancesystem.models.Admin;
import com.example.attendancesystem.models.Course;
import com.example.attendancesystem.models.Attendance;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.services.GoogleDriveService;
import com.example.attendancesystem.utils.Utils;

public class TestActivity extends AppCompatActivity {

    private static final String TAG = "TestActivity";

    // Views
    private TextView tvTestResults;
    private Button btnTestFirebase, btnTestGoogleDrive, btnTestModels, btnClearTests;

    // Services
    private FirebaseManager firebaseManager;
    private GoogleDriveService googleDriveService;

    // Résultats des tests
    private StringBuilder testResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Tests du Système");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialiser les services
        firebaseManager = FirebaseManager.getInstance();
        googleDriveService = GoogleDriveService.getInstance(this);

        // Initialiser les views
        initViews();

        // Configurer les listeners
        setupListeners();

        // Initialiser les résultats
        testResults = new StringBuilder();
        addTestResult("=== TESTS DU SYSTÈME FACE ATTENDANCE ===\n");
    }

    private void initViews() {
        tvTestResults = findViewById(R.id.tv_test_results);
        btnTestFirebase = findViewById(R.id.btn_test_firebase);
        btnTestGoogleDrive = findViewById(R.id.btn_test_google_drive);
        btnTestModels = findViewById(R.id.btn_test_models);
        btnClearTests = findViewById(R.id.btn_clear_tests);
    }

    private void setupListeners() {
        btnTestFirebase.setOnClickListener(v -> testFirebase());
        btnTestGoogleDrive.setOnClickListener(v -> testGoogleDrive());
        btnTestModels.setOnClickListener(v -> testModels());
        btnClearTests.setOnClickListener(v -> clearTests());
    }

    private void testFirebase() {
        addTestResult("\n--- TEST FIREBASE ---");

        // Test 1: Connexion Firebase
        boolean firebaseConnected = firebaseManager.isFirebaseConnected();
        addTestResult("✅ Connexion Firebase: " + (firebaseConnected ? "OK" : "ERREUR"));

        // Test 2: Utilisateur connecté
        boolean userLoggedIn = firebaseManager.isUserLoggedIn();
        String currentUserEmail = firebaseManager.getCurrentUserEmail();
        addTestResult("✅ Utilisateur connecté: " + (userLoggedIn ? "OUI (" + currentUserEmail + ")" : "NON"));

        // Test 3: Test de création d'un étudiant de test
        testCreateStudent();

        // Test 4: Test de création d'un cours de test
        testCreateCourse();

        // Test 5: Test de création d'une présence de test
        testCreateAttendance();
    }

    private void testCreateStudent() {
        Student testStudent = new Student(
                "test.student@example.com",
                "Étudiant Test",
                "TEST001",
                "Informatique",
                "L3"
        );
        testStudent.setPhoneNumber("+212600000000");

        firebaseManager.saveStudent(testStudent, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                addTestResult("✅ Création étudiant test: OK");

                firebaseManager.getStudentByEmail("test.student@example.com", new FirebaseManager.DataCallback<Student>() {
                    @Override
                    public void onSuccess(Student student) {
                        addTestResult("✅ Récupération étudiant test: OK");
                        addTestResult("   - Nom: " + student.getFullName());
                        addTestResult("   - Département: " + student.getDepartment());
                        addTestResult("   - Année: " + student.getYear());
                    }

                    @Override
                    public void onFailure(String error) {
                        addTestResult("❌ Récupération étudiant test: ERREUR - " + error);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                addTestResult("❌ Création étudiant test: ERREUR - " + error);
            }
        });
    }

    private void testCreateCourse() {
        Course testCourse = new Course(
                "Mathématiques Test",
                "test.teacher@example.com",
                "Professeur Test",
                "Mathématiques"
        );

        // Configurer le planning
        Course.Schedule schedule = new Course.Schedule();
        schedule.setDayOfWeek("monday");
        schedule.setStartTime("08:00");
        schedule.setEndTime("10:00");
        schedule.setRoom("Salle A101");
        schedule.setRecurring(true);
        testCourse.setSchedule(schedule);

        // Ajouter quelques étudiants
        testCourse.enrollStudent("test.student@example.com");
        testCourse.enrollStudent("student2@example.com");

        // Note: Pour un vrai test, il faudrait d'abord créer le cours dans Firestore
        // Pour l'instant, on teste juste la création de l'objet
        addTestResult("✅ Création objet cours test: OK");
        addTestResult("   - Cours: " + testCourse.getCourseName());
        addTestResult("   - Enseignant: " + testCourse.getTeacherName());
        addTestResult("   - Étudiants inscrits: " + testCourse.getEnrolledStudentsCount());
        addTestResult("   - Horaire: " + testCourse.getSchedule().getDayOfWeek() + " " +
                testCourse.getSchedule().getStartTime() + "-" + testCourse.getSchedule().getEndTime());
    }

    private void testCreateAttendance() {
        Attendance testAttendance = new Attendance(
                "test.student@example.com",
                "Étudiant Test",
                "TEST001",
                "COURSE001",
                "Mathématiques Test",
                0.95 // Confiance de 95%
        );

        addTestResult("✅ Création objet présence test: OK");
        addTestResult("   - Étudiant: " + testAttendance.getStudentName());
        addTestResult("   - Cours: " + testAttendance.getCourseName());
        addTestResult("   - Statut: " + testAttendance.getStatusDisplayName());
        addTestResult("   - Confiance: " + Utils.formatConfidenceScore(testAttendance.getConfidence()));
        addTestResult("   - Heure: " + Utils.formatDateTime(testAttendance.getTimestamp()));
    }

    private void testGoogleDrive() {
        addTestResult("\n--- TEST GOOGLE DRIVE ---");

        // Test 1: État de la connexion
        boolean isSignedIn = googleDriveService.isSignedIn();
        addTestResult("✅ Connexion Google Drive: " + (isSignedIn ? "CONNECTÉ" : "NON CONNECTÉ"));

        if (isSignedIn) {
            // Test 2: Compte connecté
            var account = googleDriveService.getCurrentAccount();
            if (account != null) {
                addTestResult("✅ Compte connecté: " + account.getEmail());
                addTestResult("   - Nom: " + account.getDisplayName());
            }

            // Test 3: Liste des photos (si connecté)
            googleDriveService.listProfilePhotos(new GoogleDriveService.DriveCallback<com.google.api.services.drive.model.FileList>() {
                @Override
                public void onSuccess(com.google.api.services.drive.model.FileList fileList) {
                    addTestResult("✅ Accès au dossier Drive: OK");
                    addTestResult("   - Photos trouvées: " + fileList.getFiles().size());

                    for (com.google.api.services.drive.model.File file : fileList.getFiles()) {
                        addTestResult("     • " + file.getName());
                    }
                }

                @Override
                public void onFailure(String error) {
                    addTestResult("❌ Accès au dossier Drive: ERREUR - " + error);
                }
            });
        } else {
            addTestResult("ℹ️ Pour tester Google Drive, connectez-vous d'abord");
            addTestResult("   Utilisez le bouton 'Connecter à Google Drive'");
        }
    }

    private void testModels() {
        addTestResult("\n--- TEST MODÈLES DE DONNÉES ---");

        // Test 1: Modèle Student
        testStudentModel();

        // Test 2: Modèle Teacher
        testTeacherModel();

        // Test 3: Modèle Admin
        testAdminModel();

        // Test 4: Modèle Course
        testCourseModel();

        // Test 5: Modèle Attendance
        testAttendanceModel();

        // Test 6: Utils
        testUtils();
    }

    private void testStudentModel() {
        try {
            Student student = new Student("student@test.com", "John Doe", "STU001", "Informatique", "L2");
            student.setPhoneNumber("+212600000000");

            // Test toMap()
            var map = student.toMap();
            boolean hasRequiredFields = map.containsKey("email") && map.containsKey("fullName") &&
                    map.containsKey("studentId") && map.containsKey("department");

            addTestResult("✅ Modèle Student: " + (hasRequiredFields ? "OK" : "ERREUR"));
            addTestResult("   - Email: " + student.getEmail());
            addTestResult("   - Champs requis: " + (hasRequiredFields ? "Présents" : "Manquants"));

        } catch (Exception e) {
            addTestResult("❌ Modèle Student: ERREUR - " + e.getMessage());
        }
    }

    private void testTeacherModel() {
        try {
            Teacher teacher = new Teacher("teacher@test.com", "Prof Smith", "TECH001", "Mathématiques");

            var map = teacher.toMap();
            boolean hasRequiredFields = map.containsKey("email") && map.containsKey("fullName") &&
                    map.containsKey("employeeId") && map.containsKey("department");

            addTestResult("✅ Modèle Teacher: " + (hasRequiredFields ? "OK" : "ERREUR"));

        } catch (Exception e) {
            addTestResult("❌ Modèle Teacher: ERREUR - " + e.getMessage());
        }
    }

    private void testAdminModel() {
        try {
            Admin admin = new Admin("admin@test.com", "Admin User", "ADM001", "Administration");

            var map = admin.toMap();
            boolean hasRequiredFields = map.containsKey("email") && map.containsKey("fullName") &&
                    map.containsKey("adminId") && map.containsKey("department");

            addTestResult("✅ Modèle Admin: " + (hasRequiredFields ? "OK" : "ERREUR"));

        } catch (Exception e) {
            addTestResult("❌ Modèle Admin: ERREUR - " + e.getMessage());
        }
    }

    private void testCourseModel() {
        try {
            Course course = new Course("Test Course", "teacher@test.com", "Prof Test", "Informatique");
            course.enrollStudent("student1@test.com");
            course.enrollStudent("student2@test.com");

            boolean enrollmentWorks = course.isStudentEnrolled("student1@test.com");
            boolean countCorrect = course.getEnrolledStudentsCount() == 2;

            addTestResult("✅ Modèle Course: " + (enrollmentWorks && countCorrect ? "OK" : "ERREUR"));
            addTestResult("   - Inscription étudiant: " + (enrollmentWorks ? "OK" : "ERREUR"));
            addTestResult("   - Comptage étudiants: " + (countCorrect ? "OK" : "ERREUR"));

        } catch (Exception e) {
            addTestResult("❌ Modèle Course: ERREUR - " + e.getMessage());
        }
    }

    private void testAttendanceModel() {
        try {
            Attendance attendance = new Attendance("student@test.com", "Test Student", "STU001",
                    "COURSE001", "Test Course", 0.85);

            boolean isPresent = attendance.isPresent();
            String displayName = attendance.getStatusDisplayName();
            var map = attendance.toMap();

            addTestResult("✅ Modèle Attendance: " + (isPresent && map.size() > 5 ? "OK" : "ERREUR"));
            addTestResult("   - Statut: " + displayName);
            addTestResult("   - Est présent: " + isPresent);

        } catch (Exception e) {
            addTestResult("❌ Modèle Attendance: ERREUR - " + e.getMessage());
        }
    }

    private void testUtils() {
        try {
            // Test validation email
            boolean validEmail = Utils.isValidEmail("test@example.com");
            boolean invalidEmail = !Utils.isValidEmail("invalid-email");

            // Test formatage date
            String currentTime = Utils.getCurrentTime();
            String today = Utils.getTodayDate();

            // Test extraction nom
            String firstName = Utils.getFirstName("John Doe Smith");
            String lastName = Utils.getLastName("John Doe Smith");

            boolean allTestsPass = validEmail && invalidEmail &&
                    currentTime != null && today != null &&
                    "John".equals(firstName) && "Doe Smith".equals(lastName);

            addTestResult("✅ Classe Utils: " + (allTestsPass ? "OK" : "ERREUR"));
            addTestResult("   - Validation email: " + (validEmail && invalidEmail ? "OK" : "ERREUR"));
            addTestResult("   - Formatage date: " + (currentTime != null && today != null ? "OK" : "ERREUR"));
            addTestResult("   - Extraction noms: " + ("John".equals(firstName) && "Doe Smith".equals(lastName) ? "OK" : "ERREUR"));

        } catch (Exception e) {
            addTestResult("❌ Classe Utils: ERREUR - " + e.getMessage());
        }
    }

    private void clearTests() {
        testResults = new StringBuilder();
        addTestResult("=== TESTS DU SYSTÈME FACE ATTENDANCE ===\n");
        addTestResult("Tests effacés. Prêt pour de nouveaux tests.\n");
    }

    private void addTestResult(String result) {
        testResults.append(result).append("\n");
        tvTestResults.setText(testResults.toString());

        // Scroll vers le bas automatiquement
        tvTestResults.post(() -> {
            if (tvTestResults.getLayout() != null) {
                int scrollAmount = tvTestResults.getLayout().getLineTop(tvTestResults.getLineCount()) - tvTestResults.getHeight();
                if (scrollAmount > 0) {
                    tvTestResults.scrollTo(0, scrollAmount);
                } else {
                    tvTestResults.scrollTo(0, 0);
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ajouter info utilisateur connecté
        String userEmail = Utils.getSavedUserEmail(this);
        String userRole = Utils.getSavedUserRole(this);

        if (userEmail != null) {
            addTestResult("ℹ️ Utilisateur connecté: " + userEmail + " (" + userRole + ")");
        }
    }
}