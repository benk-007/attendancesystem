package com.example.attendancesystem.config;

import android.content.Context;
import android.content.pm.ApplicationInfo;

/**
 * Configuration centrale de l'application
 * Version robuste qui ne dépend pas de BuildConfig
 */
public class AppConfig {

    // Variable statique pour stocker l'état debug (calculé une seule fois)
    private static Boolean isDebugMode = null;

    // =================== MÉTHODES UTILITAIRES ROBUSTES ===================

    /**
     * Déterminer si l'application est en mode debug
     * MÉTHODE ROBUSTE qui fonctionne même si BuildConfig n'est pas disponible
     */
    public static boolean isDebugMode(Context context) {
        if (isDebugMode == null) {
            isDebugMode = calculateDebugMode(context);
        }
        return isDebugMode;
    }

    /**
     * Méthode surchargée sans Context (pour compatibilité)
     * Utilise plusieurs techniques pour détecter le mode debug
     */
    public static boolean isDebugMode() {
        if (isDebugMode != null) {
            return isDebugMode;
        }

        // Tentative 1 : Via BuildConfig avec réflexion
        try {
            Class<?> buildConfigClass = Class.forName("com.example.attendancesystem.BuildConfig");
            java.lang.reflect.Field debugField = buildConfigClass.getField("DEBUG");
            isDebugMode = debugField.getBoolean(null);
            return isDebugMode;
        } catch (Exception e) {
            // BuildConfig non disponible, continuer avec d'autres méthodes
        }

        // Tentative 2 : Via les propriétés système
        try {
            String vmName = System.getProperty("java.vm.name");
            if (vmName != null && vmName.toLowerCase().contains("dalvik")) {
                // Nous sommes sur Android, vérifier d'autres indicateurs
                String debuggable = System.getProperty("ro.debuggable");
                if ("1".equals(debuggable)) {
                    isDebugMode = true;
                    return isDebugMode;
                }
            }
        } catch (Exception e) {
            // Ignorer les erreurs
        }

        // Tentative 3 : Via les assertions (activées seulement en debug)
        boolean assertionsEnabled = false;
        try {
            assert false;
        } catch (AssertionError e) {
            assertionsEnabled = true;
        }

        if (assertionsEnabled) {
            isDebugMode = true;
            return isDebugMode;
        }

        // Par défaut, considérer comme production
        isDebugMode = false;
        return isDebugMode;
    }

    /**
     * Calculer le mode debug en utilisant le Context Android
     */
    private static boolean calculateDebugMode(Context context) {
        try {
            ApplicationInfo appInfo = context.getApplicationInfo();
            return (appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception e) {
            // En cas d'erreur, utiliser la méthode de fallback
            return isDebugMode();
        }
    }

    /**
     * Réinitialiser le cache du mode debug (utile pour les tests)
     */
    public static void resetDebugModeCache() {
        isDebugMode = null;
    }

    /**
     * Forcer le mode debug (utile pour les tests)
     */
    public static void setDebugModeForTesting(boolean debugMode) {
        isDebugMode = debugMode;
    }

    // =================== VERSIONS ===================
    public static final String APP_VERSION = "1.0.0";
    public static final int DATABASE_VERSION = 1;

    // =================== FIREBASE ===================
    public static final String FIREBASE_PROJECT_ID = "projetiot-e854b";

    // [... Reste du code existant inchangé ...]

    // =================== MÉTHODES UTILITAIRES MISES À JOUR ===================

    /**
     * Obtenir l'URL de base selon l'environnement
     */
    public static String getBaseUrl() {
        return isDebugMode() ?
                "https://dev-api.attendance.com" :
                "https://api.attendance.com";
    }

    /**
     * Obtenir la configuration de debug
     */
    public static boolean shouldLogVerbose() {
        return isDebugMode();
    }

    /**
     * Méthodes d'aide pour le logging en mode debug
     */
    public static void debugLog(String tag, String message) {
        if (isDebugMode()) {
            android.util.Log.d(tag, message);
        }
    }

    public static void debugLogError(String tag, String message, Throwable throwable) {
        if (isDebugMode()) {
            android.util.Log.e(tag, message, throwable);
        }
    }

    /**
     * Obtenir des informations de debug sur l'environnement
     */
    public static String getDebugInfo(Context context) {
        if (!isDebugMode(context)) {
            return "Mode Production";
        }

        StringBuilder info = new StringBuilder();
        info.append("Mode Debug\n");
        info.append("Version: ").append(APP_VERSION).append("\n");

        try {
            ApplicationInfo appInfo = context.getApplicationInfo();
            info.append("Package: ").append(context.getPackageName()).append("\n");
            info.append("Debuggable: ").append((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0).append("\n");
        } catch (Exception e) {
            info.append("Erreur récupération info: ").append(e.getMessage()).append("\n");
        }

        return info.toString();
    }
}