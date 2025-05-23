package com.example.attendancesystem.services;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.attendancesystem.models.Student;
import com.example.attendancesystem.models.Teacher;
import com.example.attendancesystem.models.Admin;
import com.example.attendancesystem.models.Attendance;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";

    // Instances Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // Collections Firestore selon la nouvelle architecture
    private static final String STUDENTS_COLLECTION = "students";
    private static final String TEACHERS_COLLECTION = "teachers";
    private static final String ADMINS_COLLECTION = "admins";
    private static final String ATTENDANCE_COLLECTION = "attendance";
    private static final String COURSES_COLLECTION = "courses";
    private static final String SESSIONS_COLLECTION = "sessions";
    private static final String JUSTIFICATIONS_COLLECTION = "justifications";
    private static final String REPORTS_COLLECTION = "reports";

    // Singleton pattern
    private static FirebaseManager instance;

    private FirebaseManager() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    // =================== AUTHENTIFICATION ===================

    /**
     * Interface pour les callbacks d'authentification
     */
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(String error);
    }

    /**
     * Connexion avec email/mot de passe
     */
    public void signInWithEmailPassword(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.d(TAG, "Connexion réussie: " + user.getEmail());
                            callback.onSuccess(user);
                        } else {
                            String error = task.getException() != null ?
                                    task.getException().getMessage() : "Erreur de connexion";
                            Log.w(TAG, "Échec de connexion", task.getException());
                            callback.onFailure(error);
                        }
                    }
                });
    }

    /**
     * Inscription avec email/mot de passe
     */
    public void createUserWithEmailPassword(String email, String password, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.d(TAG, "Inscription réussie: " + user.getEmail());
                            callback.onSuccess(user);
                        } else {
                            String error = task.getException() != null ?
                                    task.getException().getMessage() : "Erreur d'inscription";
                            Log.w(TAG, "Échec d'inscription", task.getException());
                            callback.onFailure(error);
                        }
                    }
                });
    }

    /**
     * Déconnexion
     */
    public void signOut() {
        mAuth.signOut();
        Log.d(TAG, "Utilisateur déconnecté");
    }

    /**
     * Obtenir l'utilisateur actuellement connecté
     */
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    /**
     * Vérifier si un utilisateur est connecté
     */
    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }

    // =================== INTERFACE GÉNÉRIQUE POUR LES CALLBACKS ===================

    /**
     * Interface pour les callbacks de données
     */
    public interface DataCallback<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    // =================== GESTION DES ÉTUDIANTS ===================

    /**
     * Sauvegarder un étudiant dans Firestore
     */
    public void saveStudent(Student student, DataCallback<Void> callback) {
        db.collection(STUDENTS_COLLECTION)
                .document(student.getEmail())
                .set(student.toMap())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Étudiant sauvegardé: " + student.getEmail());
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Erreur sauvegarde étudiant", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    /**
     * Récupérer un étudiant par son email
     */
    public void getStudentByEmail(String email, DataCallback<Student> callback) {
        db.collection(STUDENTS_COLLECTION)
                .document(email)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Student student = document.toObject(Student.class);
                                if (student != null) {
                                    callback.onSuccess(student);
                                } else {
                                    callback.onFailure("Erreur de conversion des données");
                                }
                            } else {
                                callback.onFailure("Étudiant non trouvé");
                            }
                        } else {
                            callback.onFailure(task.getException().getMessage());
                        }
                    }
                });
    }

    /**
     * Mettre à jour le profil étudiant
     */
    public void updateStudent(Student student, DataCallback<Void> callback) {
        db.collection(STUDENTS_COLLECTION)
                .document(student.getEmail())
                .update(student.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Étudiant mis à jour: " + student.getEmail());
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erreur mise à jour étudiant", e);
                    callback.onFailure(e.getMessage());
                });
    }

    // =================== GESTION DES ENSEIGNANTS ===================

    /**
     * Sauvegarder un enseignant dans Firestore
     */
    public void saveTeacher(Teacher teacher, DataCallback<Void> callback) {
        db.collection(TEACHERS_COLLECTION)
                .document(teacher.getEmail())
                .set(teacher.toMap())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Enseignant sauvegardé: " + teacher.getEmail());
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Erreur sauvegarde enseignant", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    /**
     * Récupérer un enseignant par son email
     */
    public void getTeacherByEmail(String email, DataCallback<Teacher> callback) {
        db.collection(TEACHERS_COLLECTION)
                .document(email)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Teacher teacher = document.toObject(Teacher.class);
                                if (teacher != null) {
                                    callback.onSuccess(teacher);
                                } else {
                                    callback.onFailure("Erreur de conversion des données");
                                }
                            } else {
                                callback.onFailure("Enseignant non trouvé");
                            }
                        } else {
                            callback.onFailure(task.getException().getMessage());
                        }
                    }
                });
    }

    /**
     * Mettre à jour le profil enseignant
     */
    public void updateTeacher(Teacher teacher, DataCallback<Void> callback) {
        db.collection(TEACHERS_COLLECTION)
                .document(teacher.getEmail())
                .update(teacher.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Enseignant mis à jour: " + teacher.getEmail());
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erreur mise à jour enseignant", e);
                    callback.onFailure(e.getMessage());
                });
    }

    // =================== GESTION DES ADMINISTRATEURS ===================

    /**
     * Sauvegarder un administrateur dans Firestore
     */
    public void saveAdmin(Admin admin, DataCallback<Void> callback) {
        db.collection(ADMINS_COLLECTION)
                .document(admin.getEmail())
                .set(admin.toMap())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Administrateur sauvegardé: " + admin.getEmail());
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Erreur sauvegarde administrateur", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    /**
     * Récupérer un administrateur par son email
     */
    public void getAdminByEmail(String email, DataCallback<Admin> callback) {
        db.collection(ADMINS_COLLECTION)
                .document(email)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Admin admin = document.toObject(Admin.class);
                                if (admin != null) {
                                    callback.onSuccess(admin);
                                } else {
                                    callback.onFailure("Erreur de conversion des données");
                                }
                            } else {
                                callback.onFailure("Administrateur non trouvé");
                            }
                        } else {
                            callback.onFailure(task.getException().getMessage());
                        }
                    }
                });
    }

    /**
     * Mettre à jour le profil administrateur
     */
    public void updateAdmin(Admin admin, DataCallback<Void> callback) {
        db.collection(ADMINS_COLLECTION)
                .document(admin.getEmail())
                .update(admin.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Administrateur mis à jour: " + admin.getEmail());
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erreur mise à jour administrateur", e);
                    callback.onFailure(e.getMessage());
                });
    }

    // =================== DÉTECTION AUTOMATIQUE DU TYPE D'UTILISATEUR ===================

    /**
     * Déterminer le type d'utilisateur par email et récupérer ses données
     */
    public void getUserByEmail(String email, DataCallback<Object> callback) {
        // Essayer d'abord dans la collection students
        getStudentByEmail(email, new DataCallback<Student>() {
            @Override
            public void onSuccess(Student student) {
                callback.onSuccess(student);
            }

            @Override
            public void onFailure(String error) {
                // Si pas trouvé dans students, essayer teachers
                getTeacherByEmail(email, new DataCallback<Teacher>() {
                    @Override
                    public void onSuccess(Teacher teacher) {
                        callback.onSuccess(teacher);
                    }

                    @Override
                    public void onFailure(String error) {
                        // Si pas trouvé dans teachers, essayer admins
                        getAdminByEmail(email, new DataCallback<Admin>() {
                            @Override
                            public void onSuccess(Admin admin) {
                                callback.onSuccess(admin);
                            }

                            @Override
                            public void onFailure(String error) {
                                callback.onFailure("Utilisateur non trouvé dans aucune collection");
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * Obtenir le rôle d'un utilisateur par email
     */
    public void getUserRole(String email, DataCallback<String> callback) {
        getUserByEmail(email, new DataCallback<Object>() {
            @Override
            public void onSuccess(Object user) {
                if (user instanceof Student) {
                    callback.onSuccess("student");
                } else if (user instanceof Teacher) {
                    callback.onSuccess("teacher");
                } else if (user instanceof Admin) {
                    callback.onSuccess("admin");
                } else {
                    callback.onFailure("Type d'utilisateur non reconnu");
                }
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    // =================== GESTION DES PRÉSENCES ===================

    /**
     * Enregistrer une présence
     */
    public void saveAttendance(Attendance attendance, DataCallback<String> callback) {
        db.collection(ATTENDANCE_COLLECTION)
                .add(attendance.toMap())
                .addOnSuccessListener(documentReference -> {
                    String attendanceId = documentReference.getId();
                    attendance.setAttendanceId(attendanceId);

                    // Mettre à jour avec l'ID généré
                    documentReference.update("attendanceId", attendanceId);

                    Log.d(TAG, "Présence enregistrée: " + attendanceId);
                    callback.onSuccess(attendanceId);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erreur enregistrement présence", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Récupérer l'historique de présence d'un étudiant
     */
    public void getStudentAttendanceHistory(String studentEmail, DataCallback<List<Attendance>> callback) {
        db.collection(ATTENDANCE_COLLECTION)
                .whereEqualTo("studentEmail", studentEmail)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Attendance> attendanceList = new ArrayList<>();
                            for (DocumentSnapshot document : task.getResult()) {
                                Attendance attendance = document.toObject(Attendance.class);
                                if (attendance != null) {
                                    attendance.setAttendanceId(document.getId());
                                    attendanceList.add(attendance);
                                }
                            }
                            Log.d(TAG, "Historique récupéré: " + attendanceList.size() + " entrées");
                            callback.onSuccess(attendanceList);
                        } else {
                            callback.onFailure(task.getException().getMessage());
                        }
                    }
                });
    }

    /**
     * Récupérer les présences d'un cours
     */
    public void getCourseAttendance(String courseId, DataCallback<List<Attendance>> callback) {
        db.collection(ATTENDANCE_COLLECTION)
                .whereEqualTo("courseId", courseId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Attendance> attendanceList = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            Attendance attendance = document.toObject(Attendance.class);
                            if (attendance != null) {
                                attendance.setAttendanceId(document.getId());
                                attendanceList.add(attendance);
                            }
                        }
                        callback.onSuccess(attendanceList);
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // =================== GESTION DES FICHIERS AVEC GOOGLE DRIVE ===================

    /**
     * Interface pour l'upload de fichiers vers Google Drive
     */
    public interface GoogleDriveUploadCallback {
        void onProgress(int progress);
        void onSuccess(String fileUrl);
        void onFailure(String error);
    }

    /**
     * Upload d'une photo de profil vers Google Drive et mise à jour du profil utilisateur
     */
    public void uploadProfileImageToGoogleDrive(String userEmail, String userType,
                                                android.net.Uri imageUri, Context context,
                                                GoogleDriveUploadCallback callback) {

        GoogleDriveService driveService = GoogleDriveService.getInstance(context);

        if (!driveService.isSignedIn()) {
            callback.onFailure("Non connecté à Google Drive. Veuillez vous connecter d'abord.");
            return;
        }

        driveService.uploadProfilePhoto(userEmail, userType, imageUri,
                new GoogleDriveService.UploadCallback() {
                    @Override
                    public void onProgress(int progress) {
                        callback.onProgress(progress);
                    }

                    @Override
                    public void onSuccess(String fileUrl) {
                        // Mettre à jour le profil utilisateur avec l'URL de la photo
                        updateUserProfileImage(userEmail, userType, fileUrl, new DataCallback<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                callback.onSuccess(fileUrl);
                            }

                            @Override
                            public void onFailure(String error) {
                                callback.onFailure("Photo uploadée mais erreur de mise à jour du profil: " + error);
                            }
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onFailure(error);
                    }
                });
    }

    /**
     * Mettre à jour l'URL de l'image de profil dans Firestore
     */
    private void updateUserProfileImage(String userEmail, String userType, String imageUrl, DataCallback<Void> callback) {
        String collection = getCollectionForUserType(userType);
        if (collection == null) {
            callback.onFailure("Type d'utilisateur non reconnu: " + userType);
            return;
        }

        db.collection(collection)
                .document(userEmail)
                .update("profileImageUrl", imageUrl, "lastUpdatedAt", com.google.firebase.Timestamp.now())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "URL de l'image mise à jour pour " + userEmail);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erreur mise à jour URL image", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Obtenir le nom de collection selon le type d'utilisateur
     */
    private String getCollectionForUserType(String userType) {
        switch (userType) {
            case "student": return STUDENTS_COLLECTION;
            case "teacher": return TEACHERS_COLLECTION;
            case "admin": return ADMINS_COLLECTION;
            default: return null;
        }
    }

    /**
     * Supprimer une photo de profil de Google Drive
     */
    public void deleteProfileImageFromGoogleDrive(String userEmail, String userType,
                                                  Context context, DataCallback<Void> callback) {

        GoogleDriveService driveService = GoogleDriveService.getInstance(context);

        if (!driveService.isSignedIn()) {
            callback.onFailure("Non connecté à Google Drive");
            return;
        }

        driveService.deleteProfilePhoto(userEmail, userType, new GoogleDriveService.DriveCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Mettre à jour le profil utilisateur (vider l'URL)
                updateUserProfileImage(userEmail, userType, "", callback);
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    // =================== MÉTHODES UTILITAIRES ===================

    /**
     * Vérifier la connexion à Firebase
     */
    public boolean isFirebaseConnected() {
        return mAuth != null && db != null && storage != null;
    }

    /**
     * Obtenir l'email de l'utilisateur connecté
     */
    public String getCurrentUserEmail() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getEmail() : null;
    }
}