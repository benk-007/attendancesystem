package com.example.attendancesystem.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class Utils {

    // Constantes modifiées pour utiliser les emails
    public static final String PREFS_NAME = "FaceAttendancePrefs";
    public static final String PREF_USER_EMAIL = "user_email"; // Changé de user_id à user_email
    public static final String PREF_USER_ROLE = "user_role";
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_IS_LOGGED_IN = "is_logged_in";

    // Formats de date
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.FRENCH);
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH);

    // =================== VALIDATION ===================

    /**
     * Valider un email
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        Pattern pattern = Pattern.compile(emailPattern);
        return pattern.matcher(email.trim()).matches();
    }

    /**
     * Valider un mot de passe
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }

        // Au moins une lettre et un chiffre
        boolean hasLetter = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }

        return hasLetter && hasDigit;
    }

    /**
     * Valider un nom complet
     */
    public static boolean isValidFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return false;
        }

        String trimmedName = fullName.trim();
        return trimmedName.length() >= 2 && trimmedName.contains(" ");
    }

    /**
     * Valider un ID étudiant/employé
     */
    public static boolean isValidStudentId(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return false;
        }

        String trimmedId = studentId.trim();
        return trimmedId.length() >= 3 && trimmedId.length() <= 20;
    }

    // =================== FORMATAGE DES DATES ===================

    /**
     * Convertir Timestamp Firebase en string de date
     */
    public static String formatDate(Timestamp timestamp) {
        if (timestamp == null) return "N/A";
        return DATE_FORMAT.format(timestamp.toDate());
    }

    /**
     * Convertir Timestamp Firebase en string d'heure
     */
    public static String formatTime(Timestamp timestamp) {
        if (timestamp == null) return "N/A";
        return TIME_FORMAT.format(timestamp.toDate());
    }

    /**
     * Convertir Timestamp Firebase en string de date et heure
     */
    public static String formatDateTime(Timestamp timestamp) {
        if (timestamp == null) return "N/A";
        return DATETIME_FORMAT.format(timestamp.toDate());
    }

    /**
     * Obtenir la date du jour
     */
    public static String getTodayDate() {
        return DATE_FORMAT.format(new Date());
    }

    /**
     * Obtenir l'heure actuelle
     */
    public static String getCurrentTime() {
        return TIME_FORMAT.format(new Date());
    }

    // =================== SHARED PREFERENCES MODIFIÉES ===================

    /**
     * Sauvegarder les données utilisateur (utilise email comme identifiant)
     */
    public static void saveUserData(Context context, String userEmail, String role, String name) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_USER_EMAIL, userEmail);
        editor.putString(PREF_USER_ROLE, role);
        editor.putString(PREF_USER_NAME, name);
        editor.putBoolean(PREF_IS_LOGGED_IN, true);
        editor.apply();
    }

    /**
     * Récupérer l'email utilisateur sauvegardé
     */
    public static String getSavedUserEmail(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_USER_EMAIL, null);
    }

    /**
     * Récupérer le rôle utilisateur sauvegardé
     */
    public static String getSavedUserRole(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_USER_ROLE, null);
    }

    /**
     * Récupérer le nom utilisateur sauvegardé
     */
    public static String getSavedUserName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_USER_NAME, null);
    }

    /**
     * Vérifier si l'utilisateur est connecté
     */
    public static boolean isUserLoggedIn(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_IS_LOGGED_IN, false);
    }

    /**
     * Effacer les données utilisateur (déconnexion)
     */
    public static void clearUserData(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    // =================== MÉTHODES POUR DÉTERMINER LE TYPE D'UTILISATEUR ===================

    /**
     * Déterminer le type d'utilisateur basé sur le rôle
     */
    public static String getUserType(String role) {
        switch (role) {
            case "student": return "student";
            case "teacher": return "teacher";
            case "admin": return "admin";
            default: return "unknown";
        }
    }

    /**
     * Obtenir le nom de collection Firebase selon le rôle
     */
    public static String getCollectionName(String role) {
        switch (role) {
            case "student": return "students";
            case "teacher": return "teachers";
            case "admin": return "admins";
            default: return null;
        }
    }

    // =================== RÉSEAU ===================

    /**
     * Vérifier la connexion Internet
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }

        return false;
    }

    // =================== UI HELPERS ===================

    /**
     * Afficher un toast court
     */
    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Afficher un toast long
     */
    public static void showLongToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    // =================== MÉTHODES UTILITAIRES ===================

    /**
     * Capitaliser la première lettre
     */
    public static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    /**
     * Générer un ID unique basé sur le timestamp
     */
    public static String generateUniqueId() {
        return "ID_" + System.currentTimeMillis();
    }

    /**
     * Convertir un score de confiance en pourcentage
     */
    public static String formatConfidenceScore(double confidence) {
        int percentage = (int) Math.round(confidence * 100);
        return percentage + "%";
    }

    /**
     * Extraire le prénom depuis un nom complet
     */
    public static String getFirstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "";
        }

        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : "";
    }

    /**
     * Extraire le nom de famille depuis un nom complet
     */
    public static String getLastName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "";
        }

        String[] parts = fullName.trim().split("\\s+");
        if (parts.length > 1) {
            StringBuilder lastName = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                if (i > 1) lastName.append(" ");
                lastName.append(parts[i]);
            }
            return lastName.toString();
        }
        return "";
    }

    /**
     * Obtenir le message d'erreur d'authentification en français
     */
    public static String getAuthErrorMessage(String errorCode) {
        if (errorCode == null) return "Erreur inconnue";

        if (errorCode.contains("invalid-email") || errorCode.contains("ERROR_INVALID_EMAIL")) {
            return "Adresse email invalide";
        } else if (errorCode.contains("wrong-password") || errorCode.contains("ERROR_WRONG_PASSWORD")) {
            return "Mot de passe incorrect";
        } else if (errorCode.contains("user-not-found") || errorCode.contains("ERROR_USER_NOT_FOUND")) {
            return "Aucun compte trouvé avec cette adresse email";
        } else if (errorCode.contains("user-disabled") || errorCode.contains("ERROR_USER_DISABLED")) {
            return "Ce compte a été désactivé";
        } else if (errorCode.contains("too-many-requests") || errorCode.contains("ERROR_TOO_MANY_REQUESTS")) {
            return "Trop de tentatives. Réessayez plus tard";
        } else if (errorCode.contains("email-already-in-use") || errorCode.contains("ERROR_EMAIL_ALREADY_IN_USE")) {
            return "Cette adresse email est déjà utilisée";
        } else if (errorCode.contains("weak-password") || errorCode.contains("ERROR_WEAK_PASSWORD")) {
            return "Le mot de passe est trop faible";
        } else if (errorCode.contains("network-request-failed") || errorCode.contains("ERROR_NETWORK_REQUEST_FAILED")) {
            return "Erreur de connexion réseau";
        } else {
            return "Erreur d'authentification: " + errorCode;
        }
    }

    /**
     * Valider les données d'inscription selon le rôle
     */
    public static boolean validateRegistrationData(String email, String fullName, String id, String role) {
        if (!isValidEmail(email)) {
            return false;
        }

        if (!isValidFullName(fullName)) {
            return false;
        }

        if (!isValidStudentId(id)) {
            return false;
        }

        return "student".equals(role) || "teacher".equals(role) || "admin".equals(role);
    }

    /**
     * Formatter l'affichage d'un statut de présence
     */
    public static String getStatusDisplayText(String status) {
        switch (status) {
            case "present": return "Présent";
            case "absent": return "Absent";
            case "justified": return "Justifié";
            default: return status;
        }
    }

    /**
     * Obtenir la couleur associée à un statut
     */
    public static int getStatusColor(String status) {
        switch (status) {
            case "present": return android.R.color.holo_green_dark;
            case "absent": return android.R.color.holo_red_dark;
            case "justified": return android.R.color.holo_purple;
            default: return android.R.color.darker_gray;
        }
    }
}