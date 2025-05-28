package com.example.attendancesystem.services;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.attendancesystem.models.Justification;
import com.example.attendancesystem.models.Session;
import com.example.attendancesystem.models.Student;
import com.example.attendancesystem.models.Teacher;
import com.example.attendancesystem.models.Admin;
import com.example.attendancesystem.models.Attendance;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static class AttendanceStats {
        private int totalSessions;
        private int attendedSessions;
        private double attendanceRate;

        public AttendanceStats(int totalSessions, int attendedSessions) {
            this.totalSessions = totalSessions;
            this.attendedSessions = attendedSessions;
            this.attendanceRate = totalSessions > 0 ? (double) attendedSessions / totalSessions * 100 : 0.0;
        }

        // Getters
        public int getTotalSessions() { return totalSessions; }
        public int getAttendedSessions() { return attendedSessions; }
        public double getAttendanceRate() { return attendanceRate; }

        // Setters (add these)
        public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }
        public void setAttendedSessions(int attendedSessions) { this.attendedSessions = attendedSessions; }
        public void setAttendanceRate(double attendanceRate) { this.attendanceRate = attendanceRate; }
    }

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

    /*
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
    /**
     * Récupérer l'historique de présence d'un étudiant - VERSION SANS INDEX
     */
    public void getStudentAttendanceHistory(String studentEmail, DataCallback<List<Attendance>> callback) {
        Log.d(TAG, "🔍 Loading attendance history for: " + studentEmail);

        db.collection(ATTENDANCE_COLLECTION)
                .whereEqualTo("studentEmail", studentEmail)
                // ❌ SUPPRIMER cette ligne qui cause l'erreur d'index :
                // .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Attendance> attendanceList = new ArrayList<>();
                        QuerySnapshot querySnapshot = task.getResult();

                        Log.d(TAG, "📊 Firebase query returned: " + querySnapshot.size() + " documents");

                        if (!querySnapshot.isEmpty()) {
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                try {
                                    // Utiliser la conversion automatique de Firebase
                                    Attendance attendance = document.toObject(Attendance.class);
                                    if (attendance != null) {
                                        attendance.setAttendanceId(document.getId());
                                        attendanceList.add(attendance);
                                        Log.d(TAG, "✅ Added attendance: " + attendance.getCourseName() + " - " + attendance.getStatus());
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "❌ Error processing document: " + document.getId(), e);
                                }
                            }
                        }

                        // ✅ TRI MANUEL par timestamp (du plus récent au plus ancien)
                        Collections.sort(attendanceList, new Comparator<Attendance>() {
                            @Override
                            public int compare(Attendance a1, Attendance a2) {
                                if (a1.getTimestamp() == null && a2.getTimestamp() == null) return 0;
                                if (a1.getTimestamp() == null) return 1;
                                if (a2.getTimestamp() == null) return -1;
                                return a2.getTimestamp().compareTo(a1.getTimestamp()); // DESC
                            }
                        });

                        Log.d(TAG, "🎉 Final attendance list size: " + attendanceList.size());
                        callback.onSuccess(attendanceList);
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Erreur inconnue";
                        Log.e(TAG, "❌ Firebase query failed: " + error);
                        callback.onFailure("Erreur lors du chargement: " + error);
                    }
                });
    }

    /**
     * Convertir un DocumentSnapshot en objet Attendance - MÉTHODE ROBUSTE
     */
    private Attendance convertDocumentToAttendance(DocumentSnapshot document) {
        try {
            Attendance attendance = new Attendance();

            // Données de base
            attendance.setAttendanceId(document.getId());
            attendance.setStudentEmail(document.getString("studentEmail"));
            attendance.setStudentName(document.getString("studentName"));
            attendance.setStudentId(document.getString("studentId"));
            attendance.setCourseId(document.getString("courseId"));
            attendance.setCourseName(document.getString("courseName"));
            attendance.setSessionId(document.getString("sessionId"));
            attendance.setStatus(document.getString("status"));

            // Gestion du timestamp
            Object timestampObj = document.get("timestamp");
            if (timestampObj instanceof Timestamp) {
                attendance.setTimestamp((Timestamp) timestampObj);
            } else if (timestampObj instanceof Date) {
                attendance.setTimestamp(new Timestamp((Date) timestampObj));
            } else {
                // Fallback : timestamp actuel
                attendance.setTimestamp(Timestamp.now());
            }

            // Confiance
            Double confidence = document.getDouble("confidence");
            attendance.setConfidence(confidence != null ? confidence : 0.0);

            // Entrée manuelle
            Boolean isManual = document.getBoolean("isManualEntry");
            attendance.setManualEntry(isManual != null ? isManual : false);

            // Champs optionnels
            attendance.setModifiedBy(document.getString("modifiedBy"));
            attendance.setModificationReason(document.getString("modificationReason"));

            // Timestamps de création/modification
            Object createdAtObj = document.get("createdAt");
            if (createdAtObj instanceof Timestamp) {
                attendance.setCreatedAt((Timestamp) createdAtObj);
            }

            Object lastModifiedObj = document.get("lastModifiedAt");
            if (lastModifiedObj instanceof Timestamp) {
                attendance.setLastModifiedAt((Timestamp) lastModifiedObj);
            }

            // Détails d'attendance (optionnel)
            Map<String, Object> detailsMap = (Map<String, Object>) document.get("attendanceDetails");
            if (detailsMap != null) {
                Attendance.AttendanceDetails details = new Attendance.AttendanceDetails();

                Object captureTimeObj = detailsMap.get("captureTime");
                if (captureTimeObj instanceof Timestamp) {
                    details.setCaptureTime((Timestamp) captureTimeObj);
                }

                Object processingTimeObj = detailsMap.get("processingTime");
                if (processingTimeObj instanceof Number) {
                    details.setProcessingTime(((Number) processingTimeObj).longValue());
                }

                Object retryCountObj = detailsMap.get("retryCount");
                if (retryCountObj instanceof Number) {
                    details.setRetryCount(((Number) retryCountObj).intValue());
                }

                String location = (String) detailsMap.get("location");
                if (location != null) {
                    details.setLocation(location);
                }

                attendance.setAttendanceDetails(details);
            }

            Log.d(TAG, "✅ Successfully converted document to Attendance: " + attendance.getCourseName());
            return attendance;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error converting document to Attendance: " + document.getId(), e);
            return null;
        }
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

    public void saveSession(Session session, DataCallback<String> callback) {
        db.collection(SESSIONS_COLLECTION)
                .add(session.toMap())
                .addOnSuccessListener(documentReference -> {
                    String sessionId = documentReference.getId();
                    session.setSessionId(sessionId);

                    // Mettre à jour avec l'ID généré
                    documentReference.update("sessionId", sessionId);

                    Log.d(TAG, "Session sauvegardée: " + sessionId);
                    callback.onSuccess(sessionId);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erreur sauvegarde session", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Mettre à jour une session
     */
    public void updateSession(Session session, DataCallback<Void> callback) {
        if (session.getSessionId() == null) {
            callback.onFailure("ID de session manquant");
            return;
        }

        db.collection(SESSIONS_COLLECTION)
                .document(session.getSessionId())
                .update(session.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Session mise à jour: " + session.getSessionId());
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erreur mise à jour session", e);
                    callback.onFailure(e.getMessage());
                });
    }

    // Add these NEW methods to your FirebaseManager.java (replace the existing session methods)

// =================== FIELD-BASED SESSION MANAGEMENT ===================

    /**
     * Obtenir les sessions d'aujourd'hui pour un étudiant (par département, filière et année)
     */
    public void getTodaySessionsForStudent(String studentEmail, String department, String field, String year, DataCallback<List<Session>> callback) {
        Log.d(TAG, "Loading today's sessions for: " + studentEmail + " in " + department + "/" + field + "/" + year);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Timestamp startOfDay = new Timestamp(calendar.getTime());

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Timestamp startOfNextDay = new Timestamp(calendar.getTime());

        // Construire une requête flexible
        Query query = db.collection(SESSIONS_COLLECTION);

        if (department != null && !department.isEmpty()) {
            query = query.whereEqualTo("department", department);
        }

        query.whereGreaterThanOrEqualTo("startTime", startOfDay)
                .whereLessThan("startTime", startOfNextDay)
                .orderBy("startTime")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Session> sessions = new ArrayList<>();
                        QuerySnapshot querySnapshot = task.getResult();

                        if (querySnapshot != null) {
                            for (DocumentSnapshot document : querySnapshot) {
                                try {
                                    Session session = document.toObject(Session.class);
                                    if (session != null) {
                                        session.setSessionId(document.getId());

                                        // Vérifier si l'étudiant correspond aux critères
                                        boolean matches = true;
                                        if (field != null && !field.isEmpty() &&
                                                !field.equals(session.getField())) {
                                            matches = false;
                                        }
                                        if (year != null && !year.isEmpty() &&
                                                session.getTargetYears() != null &&
                                                !session.getTargetYears().contains(year)) {
                                            matches = false;
                                        }

                                        if (matches) {
                                            sessions.add(session);
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.w(TAG, "Error parsing session document: " + document.getId(), e);
                                }
                            }
                        }

                        Log.d(TAG, "Found " + sessions.size() + " sessions today");
                        callback.onSuccess(sessions);
                    } else {
                        Log.e(TAG, "Error getting today's sessions", task.getException());
                        callback.onSuccess(new ArrayList<>()); // Retourner liste vide
                    }
                });
    }

    /**
     * Obtenir la prochaine session pour un étudiant (par département, filière et année)
     */
    public void getNextSessionForStudent(String studentEmail, String department, String field, String year, DataCallback<Session> callback) {
        Timestamp now = Timestamp.now();

        db.collection(SESSIONS_COLLECTION)
                .whereEqualTo("department", department)
                .whereEqualTo("field", field)
                .whereArrayContains("targetYears", year)
                .whereIn("status", Arrays.asList("scheduled", "active"))
                .whereGreaterThanOrEqualTo("startTime", now)
                .orderBy("startTime")
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                            Session session = document.toObject(Session.class);
                            if (session != null) {
                                session.setSessionId(document.getId());
                            }
                            Log.d(TAG, "Next session found: " + (session != null ? session.getCourseName() : "null"));
                            callback.onSuccess(session);
                        } else {
                            Log.d(TAG, "No future sessions found for " + field + " " + year);
                            callback.onSuccess(null);
                        }
                    } else {
                        Log.e(TAG, "Error getting next session", task.getException());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    /**
     * Obtenir les sessions d'aujourd'hui pour un enseignant
     */
    public void getTodaySessionsForTeacher(String teacherEmail, DataCallback<List<Session>> callback) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Timestamp startOfDay = new Timestamp(calendar.getTime());

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Timestamp startOfNextDay = new Timestamp(calendar.getTime());

        db.collection(SESSIONS_COLLECTION)
                .whereEqualTo("teacherEmail", teacherEmail)
                .whereGreaterThanOrEqualTo("startTime", startOfDay)
                .whereLessThan("startTime", startOfNextDay)
                .orderBy("startTime")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Session> sessions = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            Session session = document.toObject(Session.class);
                            if (session != null) {
                                session.setSessionId(document.getId());
                                sessions.add(session);
                            }
                        }
                        Log.d(TAG, "Found " + sessions.size() + " sessions today for teacher: " + teacherEmail);
                        callback.onSuccess(sessions);
                    } else {
                        Log.e(TAG, "Error getting teacher's today sessions", task.getException());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    /**
     * Obtenir la prochaine session pour un enseignant
     */
    public void getNextSessionForTeacher(String teacherEmail, DataCallback<Session> callback) {
        Timestamp now = Timestamp.now();

        db.collection(SESSIONS_COLLECTION)
                .whereEqualTo("teacherEmail", teacherEmail)
                .whereIn("status", Arrays.asList("scheduled", "active"))
                .whereGreaterThanOrEqualTo("startTime", now)
                .orderBy("startTime")
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                            Session session = document.toObject(Session.class);
                            if (session != null) {
                                session.setSessionId(document.getId());
                            }
                            Log.d(TAG, "Next session found for teacher: " + (session != null ? session.getCourseName() : "null"));
                            callback.onSuccess(session);
                        } else {
                            Log.d(TAG, "No future sessions found for teacher: " + teacherEmail);
                            callback.onSuccess(null);
                        }
                    } else {
                        Log.e(TAG, "Error getting teacher's next session", task.getException());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    /**
     * Obtenir les sessions programmées aujourd'hui pour un enseignant
     */
    public void getTodayScheduledSessionsForTeacher(String teacherEmail, DataCallback<List<Session>> callback) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Timestamp startOfDay = new Timestamp(calendar.getTime());

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Timestamp startOfNextDay = new Timestamp(calendar.getTime());

        db.collection(SESSIONS_COLLECTION)
                .whereEqualTo("teacherEmail", teacherEmail)
                .whereEqualTo("status", "scheduled")
                .whereGreaterThanOrEqualTo("startTime", startOfDay)
                .whereLessThan("startTime", startOfNextDay)
                .orderBy("startTime")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Session> sessions = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            Session session = document.toObject(Session.class);
                            if (session != null) {
                                session.setSessionId(document.getId());
                                sessions.add(session);
                            }
                        }
                        Log.d(TAG, "Found " + sessions.size() + " scheduled sessions today for teacher: " + teacherEmail);
                        callback.onSuccess(sessions);
                    } else {
                        Log.e(TAG, "Error getting teacher's scheduled sessions", task.getException());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    /**
     * NOUVELLE VERSION - Obtenir les statistiques d'assiduité d'un étudiant
     * Cette version utilise directement la collection 'attendance' au lieu de 'sessions'
     */
    public void getStudentAttendanceStatistics(String studentEmail, String department, String field, String year, DataCallback<AttendanceStatsDetailed> callback) {
        Log.d(TAG, "🔍 Loading statistics for student: " + studentEmail + " in " + department + "/" + field + "/" + year);

        // Calculer les statistiques des 60 derniers jours
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -60);
        Timestamp sixtyDaysAgo = new Timestamp(calendar.getTime());

        // ✅ UTILISER LA COLLECTION ATTENDANCE au lieu de SESSIONS
        db.collection(ATTENDANCE_COLLECTION)
                .whereEqualTo("studentEmail", studentEmail)
                .whereGreaterThanOrEqualTo("timestamp", sixtyDaysAgo) // Filtrer par date
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int totalSessions = 0;
                        int attendedSessions = 0;
                        int absentSessions = 0;
                        int justifiedSessions = 0;

                        QuerySnapshot querySnapshot = task.getResult();
                        Log.d(TAG, "📊 Found " + querySnapshot.size() + " attendance records");

                        if (!querySnapshot.isEmpty()) {
                            // ✅ Compter chaque statut
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                try {
                                    String status = document.getString("status");

                                    if (status != null) {
                                        totalSessions++;

                                        switch (status.toLowerCase()) {
                                            case "present":
                                                attendedSessions++;
                                                break;
                                            case "absent":
                                                absentSessions++;
                                                break;
                                            case "justified":
                                                justifiedSessions++;
                                                break;
                                        }
                                    }

                                } catch (Exception e) {
                                    Log.w(TAG, "❌ Error parsing attendance document: " + document.getId(), e);
                                }
                            }
                        }

                        // ✅ Créer les statistiques avec tous les détails
                        AttendanceStatsDetailed stats = new AttendanceStatsDetailed(
                                totalSessions,
                                attendedSessions,
                                absentSessions,
                                justifiedSessions
                        );

                        Log.d(TAG, "📈 Statistics calculated - Rate: " + stats.getAttendanceRate() +
                                "%, Total: " + totalSessions +
                                ", Present: " + attendedSessions +
                                ", Absent: " + absentSessions +
                                ", Justified: " + justifiedSessions);

                        callback.onSuccess(stats);
                    } else {
                        Log.e(TAG, "❌ Error getting attendance statistics", task.getException());
                        // Retourner des stats vides plutôt qu'une erreur
                        callback.onSuccess(new AttendanceStatsDetailed(0, 0, 0, 0));
                    }
                });
    }

    /**
     * NOUVELLE CLASSE pour des statistiques plus détaillées
     */
    /**
     * CORRECTION 1: Ajouter des setters manquants dans AttendanceStatsDetailed
     */
    public static class AttendanceStatsDetailed extends AttendanceStats {
        private int absentSessions;
        private int justifiedSessions;

        public AttendanceStatsDetailed(int totalSessions, int attendedSessions, int absentSessions, int justifiedSessions) {
            super(totalSessions, attendedSessions);
            this.absentSessions = absentSessions;
            this.justifiedSessions = justifiedSessions;
        }

        // Getters supplémentaires
        public int getAbsentSessions() { return absentSessions; }
        public int getJustifiedSessions() { return justifiedSessions; }

        // ✅ AJOUTER CES SETTERS MANQUANTS
        public void setAbsentSessions(int absentSessions) {
            this.absentSessions = absentSessions;
        }

        public void setJustifiedSessions(int justifiedSessions) {
            this.justifiedSessions = justifiedSessions;
        }

        // Méthodes utilitaires
        public double getAbsentRate() {
            return getTotalSessions() > 0 ? (double) absentSessions / getTotalSessions() * 100 : 0.0;
        }

        public double getJustifiedRate() {
            return getTotalSessions() > 0 ? (double) justifiedSessions / getTotalSessions() * 100 : 0.0;
        }
    }

    /**
     * BONUS : Méthode pour calculer les statistiques par cours
     */
    public void getStudentStatisticsByCourse(String studentEmail, DataCallback<Map<String, AttendanceStatsDetailed>> callback) {
        Log.d(TAG, "📚 Loading course-specific statistics for: " + studentEmail);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -60);
        Timestamp sixtyDaysAgo = new Timestamp(calendar.getTime());

        db.collection(ATTENDANCE_COLLECTION)
                .whereEqualTo("studentEmail", studentEmail)
                .whereGreaterThanOrEqualTo("timestamp", sixtyDaysAgo)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Map<String, AttendanceStatsDetailed> courseStats = new HashMap<>();

                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            try {
                                String courseName = document.getString("courseName");
                                String status = document.getString("status");

                                if (courseName != null && status != null) {
                                    // Initialiser les stats du cours si nécessaire
                                    if (!courseStats.containsKey(courseName)) {
                                        courseStats.put(courseName, new AttendanceStatsDetailed(0, 0, 0, 0));
                                    }

                                    // Mettre à jour les compteurs
                                    AttendanceStatsDetailed stats = courseStats.get(courseName);
                                    stats.setTotalSessions(stats.getTotalSessions() + 1);

                                    switch (status.toLowerCase()) {
                                        case "present":
                                            stats.setAttendedSessions(stats.getAttendedSessions() + 1);
                                            break;
                                        case "absent":
                                            stats.setAbsentSessions(stats.getAbsentSessions() + 1);
                                            break;
                                        case "justified":
                                            stats.setJustifiedSessions(stats.getJustifiedSessions() + 1);
                                            break;
                                    }

                                    // Recalculer le taux d'assiduité
                                    double newRate = stats.getTotalSessions() > 0 ?
                                            (double) stats.getAttendedSessions() / stats.getTotalSessions() * 100 : 0.0;
                                    stats.setAttendanceRate(newRate);
                                }

                            } catch (Exception e) {
                                Log.w(TAG, "Error processing course stats: " + document.getId(), e);
                            }
                        }

                        Log.d(TAG, "📊 Course statistics calculated for " + courseStats.size() + " courses");
                        callback.onSuccess(courseStats);
                    } else {
                        callback.onFailure("Erreur lors du calcul des statistiques par cours");
                    }
                });
    }

    /**
     * Obtenir les sessions d'une filière pour une semaine donnée
     */
    public void getWeeklySessionsForField(String department, String field, String year, Timestamp weekStart, DataCallback<List<Session>> callback) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(weekStart.toDate());

        // End of week (7 days later)
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        Timestamp weekEnd = new Timestamp(calendar.getTime());

        db.collection(SESSIONS_COLLECTION)
                .whereEqualTo("department", department)
                .whereEqualTo("field", field)
                .whereArrayContains("targetYears", year)
                .whereGreaterThanOrEqualTo("startTime", weekStart)
                .whereLessThan("startTime", weekEnd)
                .orderBy("startTime")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Session> sessions = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            Session session = document.toObject(Session.class);
                            if (session != null) {
                                session.setSessionId(document.getId());
                                sessions.add(session);
                            }
                        }
                        callback.onSuccess(sessions);
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    /**
     * Auto-enroll students in sessions based on their field and year
     */
    public void autoEnrollStudentsInSession(Session session, DataCallback<Void> callback) {
        // Get all students matching the session's criteria
        db.collection(STUDENTS_COLLECTION)
                .whereEqualTo("department", session.getDepartment())
                .whereEqualTo("field", session.getField())
                .whereEqualTo("year", session.getTargetYears().get(0)) // For simplicity, taking first target year
                .whereEqualTo("isActive", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> studentEmails = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            Student student = document.toObject(Student.class);
                            if (student != null) {
                                studentEmails.add(student.getEmail());
                            }
                        }

                        // Update session with enrolled students
                        session.setEnrolledStudentEmails(studentEmails);

                        // Save updated session
                        updateSession(session, callback);

                        Log.d(TAG, "Auto-enrolled " + studentEmails.size() + " students in session: " + session.getCourseName());
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }
    // Add this method to your existing FirebaseManager.java class

    /**
     * Obtenir une session active pour un enseignant
     */
    public void getActiveSessionForTeacher(String teacherEmail, DataCallback<Session> callback) {
        Log.d(TAG, "Getting active session for teacher: " + teacherEmail);

        db.collection(SESSIONS_COLLECTION)
                .whereEqualTo("teacherEmail", teacherEmail)
                .whereEqualTo("status", "active")
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                            Session session = document.toObject(Session.class);
                            if (session != null) {
                                session.setSessionId(document.getId());
                            }
                            Log.d(TAG, "Active session found: " + (session != null ? session.getCourseName() : "null"));
                            callback.onSuccess(session);
                        } else {
                            Log.d(TAG, "No active session found for teacher: " + teacherEmail);
                            callback.onSuccess(null);
                        }
                    } else {
                        Log.e(TAG, "Error getting active session", task.getException());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }


    // ---------------------------------------------- JUSTIFS
    public void getStudentByEmail(String email, DataCallback<Student> callback) {
        db.collection(STUDENTS_COLLECTION)
                .document(email)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Student student = documentSnapshot.toObject(Student.class);
                        if (student != null) { // Added null check for robustness
                            callback.onSuccess(student);
                        } else {
                            callback.onFailure("Erreur de conversion des données"); // More specific error
                        }
                    } else {
                        callback.onFailure("Étudiant non trouvé");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Erreur de récupération de l'étudiant: " + e.getMessage()));
    }

    // This method needs to be implemented to fetch courses associated with the student
    // based on department, field, and year.
    public void getStudentCourses(String studentEmail, String department, String field, String year, DataCallback<List<Map<String, String>>> callback) {
        Log.d(TAG, "Searching courses - Dept: " + department + ", Field: " + field + ", Year: " + year);

        // STRATÉGIE 1 : Requête progressive (du plus spécifique au plus général)

        // Tentative 1 : Critères complets
        searchCoursesWithAllCriteria(department, field, year, new DataCallback<List<Map<String, String>>>() {
            @Override
            public void onSuccess(List<Map<String, String>> courses) {
                if (courses.size() >= 3) { // Minimum acceptable
                    callback.onSuccess(courses);
                } else {
                    // Tentative 2 : Sans filière spécifique
                    searchCoursesByFieldAndYear(field, year, callback);
                }
            }

            @Override
            public void onFailure(String error) {
                // Fallback immédiat
                searchCoursesByFieldAndYear(field, year, callback);
            }
        });
    }

    // Méthode avec tous les critères
    private void searchCoursesWithAllCriteria(String department, String field, String year, DataCallback<List<Map<String, String>>> callback) {
        db.collection("courses")
                .whereEqualTo("department", department)
                .whereEqualTo("field", field)
                .whereArrayContains("targetYears", year)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, String>> courses = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        courses.add(createCourseMap(document));
                    }
                    Log.d(TAG, "Found " + courses.size() + " courses with complete criteria");
                    callback.onSuccess(courses);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Méthode fallback par département et année
    private void searchCoursesByFieldAndYear(String field, String year, DataCallback<List<Map<String, String>>> callback) {
        db.collection("courses")
                .whereEqualTo("field", field)
                .whereArrayContains("targetYears", year)
                .whereEqualTo("isActive", true)
                .limit(10) // Limiter pour éviter trop de résultats
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, String>> courses = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        courses.add(createCourseMap(document));
                    }
                    Log.d(TAG, "Fallback found " + courses.size() + " courses");

                    if (courses.size() >= 3) {
                        callback.onSuccess(courses);
                    } else {
                        // Dernier recours : par field seulement
                        searchCoursesByFieldOnly(field, callback);
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Dernier recours : département seulement
    private void searchCoursesByFieldOnly(String field, DataCallback<List<Map<String, String>>> callback) {
        db.collection("courses")
                .whereEqualTo("field", field)
                .whereEqualTo("isActive", true)
                .limit(8)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, String>> courses = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        courses.add(createCourseMap(document));
                    }
                    Log.d(TAG, "Final fallback found " + courses.size() + " courses");
                    callback.onSuccess(courses);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Helper method pour créer la map de cours
    private Map<String, String> createCourseMap(QueryDocumentSnapshot document) {
        Map<String, String> course = new HashMap<>();
        course.put("id", document.getId());
        course.put("name", document.getString("courseName"));
        course.put("teacherName", document.getString("teacherName"));
        course.put("department", document.getString("department"));
        course.put("field", document.getString("field"));

        // Ajouter les détails du planning si disponibles
        Map<String, Object> schedule = (Map<String, Object>) document.get("courseScheduleEntry");
        if (schedule != null) {
            course.put("dayOfWeek", (String) schedule.get("dayOfWeek"));
            course.put("timeSlot", schedule.get("startTime") + "-" + schedule.get("endTime"));
            course.put("room", (String) schedule.get("room"));
        }

        return course;
    }

    // Méthode de fallback pour chercher seulement par département
    private void fallbackSearchByDepartment(String department, String year, DataCallback<List<Map<String, String>>> callback) {
        Log.d(TAG, "Fallback search - Department: " + department + ", Year: " + year);

        db.collection("courses")
                .whereEqualTo("department", department)
                .whereArrayContains("targetYears", year)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<Map<String, String>> courses = new ArrayList<>();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Map<String, String> course = new HashMap<>();
                            course.put("id", document.getId());
                            course.put("name", document.getString("courseName"));
                            course.put("teacherName", document.getString("teacherName"));
                            course.put("department", document.getString("department"));
                            course.put("field", document.getString("field"));
                            courses.add(course);
                        }
                        Log.d(TAG, "Found " + courses.size() + " courses with department fallback");
                        callback.onSuccess(courses);
                    } else {
                        // Dernière tentative : tous les cours actifs
                        Log.d(TAG, "No courses found with department, trying all active courses");
                        getAllActiveCourses(callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error in fallback search: " + e.getMessage());
                    getAllActiveCourses(callback);
                });
    }

    // Dernière méthode de fallback : tous les cours actifs
    private void getAllActiveCourses(DataCallback<List<Map<String, String>>> callback) {
        Log.d(TAG, "Final fallback: getting all active courses");

        db.collection("courses")
                .whereEqualTo("isActive", true)
                .limit(20) // Limiter pour éviter trop de résultats
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, String>> courses = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, String> course = new HashMap<>();
                        course.put("id", document.getId());
                        course.put("name", document.getString("courseName"));
                        course.put("teacherName", document.getString("teacherName"));
                        course.put("department", document.getString("department"));
                        course.put("field", document.getString("field"));
                        courses.add(course);
                    }
                    Log.d(TAG, "Final fallback found " + courses.size() + " courses");
                    callback.onSuccess(courses);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error in final fallback: " + e.getMessage());
                    callback.onFailure("Impossible de charger les cours: " + e.getMessage());
                });
    }

    // --- Justification Operations ---

    /**
     * Get all justifications from Firestore for admin review.
     * Can be extended with filtering if needed.
     */
    public void getAllJustifications(DataCallback<List<Justification>> callback) {
        db.collection("justifications")
                .orderBy("submittedAt", Query.Direction.DESCENDING) // Order by latest submitted
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Justification> justifications = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Justification justification = document.toObject(Justification.class);
                            // Assign the Firestore document ID to the Justification object
                            justification.setJustificationId(document.getId());
                            justifications.add(justification);
                        } catch (Exception e) {
                            Log.e(TAG, "Error deserializing justification document: " + document.getId(), e);
                            // Handle corrupted documents gracefully, perhaps skip them
                        }
                    }
                    callback.onSuccess(justifications);
                })
                .addOnFailureListener(e -> callback.onFailure("Error getting all justifications: " + e.getMessage()));
    }

    /**
     * Update an existing justification in Firestore.
     */
    public void updateJustification(Justification justification, DataCallback<Void> callback) {
        if (justification.getJustificationId() == null || justification.getJustificationId().isEmpty()) {
            callback.onFailure("Justification ID is missing for update.");
            return;
        }

        db.collection("justifications")
                .document(justification.getJustificationId())
                .set(justification.toMap()) // Use toMap() to ensure all fields are set correctly
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Justification updated: " + justification.getJustificationId());
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating justification", e);
                    callback.onFailure("Error updating justification: " + e.getMessage());
                });
    }

    // Save a new justification (modified to use Justification model with justificationDate)
    /**
     * Sauvegarder une nouvelle justification dans Firestore.
     * Définit également l'ID du document généré sur l'objet Justification.
     */
    public void saveJustification(Justification justification, DataCallback<String> callback) {
        db.collection("justifications")
                .add(justification.toMap()) // Use toMap() for explicit control
                .addOnSuccessListener(documentReference -> {
                    // IMPORTANT: Set the generated Firestore document ID back to the Justification object
                    justification.setJustificationId(documentReference.getId());
                    Log.d(TAG, "Justification saved with ID: " + documentReference.getId());

                    // Now, update the document to include its own ID as a field (optional but good practice)
                    // This makes it easier to query by ID directly from the document itself if needed.
                    db.collection("justifications")
                            .document(documentReference.getId())
                            .update("justificationId", documentReference.getId())
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Justification ID field updated in Firestore for: " + documentReference.getId());
                                callback.onSuccess(documentReference.getId());
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Error updating justificationId field in Firestore for: " + documentReference.getId(), e);
                                // Even if this nested update fails, the document was added,
                                // and the ID was set on the Java object, so we still call success.
                                callback.onSuccess(documentReference.getId());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding justification", e);
                    callback.onFailure("Error adding justification: " + e.getMessage());
                });
    }

    // Get all justifications for a specific student (modified to use studentEmail)
    public void getStudentJustifications(String studentEmail, DataCallback<List<Justification>> callback) {
        db.collection("justifications")
                .whereEqualTo("studentEmail", studentEmail)
                .orderBy("submittedAt", Query.Direction.DESCENDING) // Order by latest submitted
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Justification> justifications = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        justifications.add(document.toObject(Justification.class));
                    }
                    callback.onSuccess(justifications);
                })
                .addOnFailureListener(e -> callback.onFailure("Error getting student justifications: " + e.getMessage()));
    }

    // --- New/Modified: Attendance Operations (to be used by admin after justification approval) ---

    // This method will be used by an ADMIN to update attendance status
    // after a justification is approved.
    public void updateAttendanceStatusForJustifiedAbsence(
            String studentEmail, String courseId, Date justificationDate, DataCallback<Void> callback) {

        // To handle potential time differences and ensure date-based matching,
        // we'll query for attendance records within a specific day range.
        Calendar startOfDay = Calendar.getInstance();
        startOfDay.setTime(justificationDate);
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = Calendar.getInstance();
        endOfDay.setTime(justificationDate);
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);


        db.collection("attendance")
                .whereEqualTo("studentEmail", studentEmail)
                .whereEqualTo("courseId", courseId)
                .whereGreaterThanOrEqualTo("timestamp", startOfDay.getTime())
                .whereLessThanOrEqualTo("timestamp", endOfDay.getTime())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onFailure("No attendance record found for this student, course, and date.");
                        return;
                    }

                    // Batch update to ensure all matching records are updated
                    // This is crucial if a student had multiple sessions/attendances for the same course on one day
                    // and was absent from all.
                    FirebaseFirestore.getInstance().runBatch(batch -> {
                                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                    // Only update if current status is "absent". If it's already "justified" or "present",
                                    // it means it was handled or not an absence.
                                    if ("absent".equalsIgnoreCase(document.getString("status"))) {
                                        batch.update(document.getReference(), "status", "justified");
                                    }
                                }
                            })
                            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                            .addOnFailureListener(e -> callback.onFailure("Error updating attendance status in batch: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure("Error querying attendance for update: " + e.getMessage()));
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