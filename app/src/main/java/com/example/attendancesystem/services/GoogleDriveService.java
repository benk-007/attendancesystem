package com.example.attendancesystem.services;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;

import com.google.api.client.http.ByteArrayContent;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GoogleDriveService {
    private static final String TAG = "GoogleDriveService";
    private static final String APPLICATION_NAME = "Face Attendance System";
    private static final String FOLDER_NAME = "FaceAttendancePhotos";

    private Context context;
    private Drive driveService;
    private GoogleSignInClient googleSignInClient;
    private String folderId;
    private Executor executor;

    // Singleton pattern
    private static GoogleDriveService instance;

    private GoogleDriveService(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newFixedThreadPool(4);
        initializeGoogleSignIn();
    }

    public static synchronized GoogleDriveService getInstance(Context context) {
        if (instance == null) {
            instance = new GoogleDriveService(context);
        }
        return instance;
    }

    /**
     * Interface pour les callbacks
     */
    public interface DriveCallback<T> {
        void onSuccess(T result);
        void onFailure(String error);
    }

    /**
     * Interface pour les callbacks d'upload avec progression
     */
    public interface UploadCallback {
        void onProgress(int progress);
        void onSuccess(String fileUrl);
        void onFailure(String error);
    }

    /**
     * Initialiser Google Sign-In
     */
    private void initializeGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();

        googleSignInClient = GoogleSignIn.getClient(context, gso);
    }

    /**
     * Obtenir l'intent de connexion Google
     */
    public Intent getSignInIntent() {
        return googleSignInClient.getSignInIntent();
    }

    /**
     * Configurer le service Drive après connexion réussie
     */
    public void setupDriveService(GoogleSignInAccount account, DriveCallback<Void> callback) {
        executor.execute(() -> {
            try {
                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                        context, Collections.singleton(DriveScopes.DRIVE_FILE));
                credential.setSelectedAccount(account.getAccount());

                driveService = new Drive.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        new GsonFactory(),
                        credential)
                        .setApplicationName(APPLICATION_NAME)
                        .build();

                // Créer ou obtenir le dossier de stockage
                createOrGetFolder(new DriveCallback<String>() {
                    @Override
                    public void onSuccess(String folderId) {
                        GoogleDriveService.this.folderId = folderId;
                        callback.onSuccess(null);
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onFailure(error);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la configuration du service Drive", e);
                callback.onFailure(e.getMessage());
            }
        });
    }

    /**
     * Créer ou obtenir le dossier de stockage des photos
     */
    private void createOrGetFolder(DriveCallback<String> callback) {
        executor.execute(() -> {
            try {
                // Chercher si le dossier existe déjà
                String query = "name='" + FOLDER_NAME + "' and mimeType='application/vnd.google-apps.folder' and trashed=false";
                FileList result = driveService.files().list().setQ(query).execute();

                if (result.getFiles().size() > 0) {
                    // Dossier existe déjà
                    String existingFolderId = result.getFiles().get(0).getId();
                    Log.d(TAG, "Dossier existant trouvé: " + existingFolderId);
                    callback.onSuccess(existingFolderId);
                } else {
                    // Créer un nouveau dossier
                    File folderMetadata = new File();
                    folderMetadata.setName(FOLDER_NAME);
                    folderMetadata.setMimeType("application/vnd.google-apps.folder");

                    File folder = driveService.files().create(folderMetadata).execute();
                    String newFolderId = folder.getId();

                    Log.d(TAG, "Nouveau dossier créé: " + newFolderId);
                    callback.onSuccess(newFolderId);
                }

            } catch (IOException e) {
                Log.e(TAG, "Erreur lors de la création/recherche du dossier", e);
                callback.onFailure(e.getMessage());
            }
        });
    }

    /**
     * Upload une photo de profil
     */
    public void uploadProfilePhoto(String userEmail, String userType, Uri imageUri, UploadCallback callback) {
        if (driveService == null || folderId == null) {
            callback.onFailure("Service Drive non initialisé");
            return;
        }

        executor.execute(() -> {
            try {
                // Simuler la progression
                callback.onProgress(10);

                // Lire le fichier
                InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
                byte[] imageBytes = readInputStream(inputStream);

                callback.onProgress(30);

                // Préparer les métadonnées du fichier
                String fileName = userType + "_" + userEmail.replace("@", "_") + "_profile.jpg";
                File fileMetadata = new File();
                fileMetadata.setName(fileName);
                fileMetadata.setParents(Collections.singletonList(folderId));
                fileMetadata.setDescription("Photo de profil pour " + userEmail);

                callback.onProgress(50);

                // Upload du fichier
                ByteArrayContent mediaContent = new ByteArrayContent("image/jpeg", imageBytes);
                File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                        .setFields("id,webViewLink,webContentLink")
                        .execute();

                callback.onProgress(80);

                // Rendre le fichier public (lecture seule)
                Permission permission = new Permission();
                permission.setType("anyone");
                permission.setRole("reader");
                driveService.permissions().create(uploadedFile.getId(), permission).execute();

                callback.onProgress(100);

                // Construire l'URL publique
                String publicUrl = "https://drive.google.com/uc?id=" + uploadedFile.getId();

                Log.d(TAG, "Photo uploadée avec succès: " + publicUrl);
                callback.onSuccess(publicUrl);

            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'upload", e);
                callback.onFailure(e.getMessage());
            }
        });
    }

    /**
     * Supprimer une photo de profil existante
     */
    public void deleteProfilePhoto(String userEmail, String userType, DriveCallback<Void> callback) {
        if (driveService == null || folderId == null) {
            callback.onFailure("Service Drive non initialisé");
            return;
        }

        executor.execute(() -> {
            try {
                String fileName = userType + "_" + userEmail.replace("@", "_") + "_profile.jpg";
                String query = "name='" + fileName + "' and parents in '" + folderId + "' and trashed=false";

                FileList result = driveService.files().list().setQ(query).execute();

                if (result.getFiles().size() > 0) {
                    String fileId = result.getFiles().get(0).getId();
                    driveService.files().delete(fileId).execute();

                    Log.d(TAG, "Photo supprimée: " + fileName);
                    callback.onSuccess(null);
                } else {
                    Log.d(TAG, "Aucune photo à supprimer pour: " + fileName);
                    callback.onSuccess(null);
                }

            } catch (IOException e) {
                Log.e(TAG, "Erreur lors de la suppression", e);
                callback.onFailure(e.getMessage());
            }
        });
    }

    /**
     * Vérifier si l'utilisateur est connecté à Google Drive
     */
    public boolean isSignedIn() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        return account != null && driveService != null;
    }

    /**
     * Se déconnecter de Google Drive
     */
    public void signOut(DriveCallback<Void> callback) {
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            driveService = null;
            folderId = null;
            callback.onSuccess(null);
        });
    }

    /**
     * Obtenir les informations du compte connecté
     */
    public GoogleSignInAccount getCurrentAccount() {
        return GoogleSignIn.getLastSignedInAccount(context);
    }

    /**
     * Lire complètement un InputStream
     */
    private byte[] readInputStream(InputStream inputStream) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    /**
     * Obtenir la liste des photos dans le dossier
     */
    public void listProfilePhotos(DriveCallback<FileList> callback) {
        if (driveService == null || folderId == null) {
            callback.onFailure("Service Drive non initialisé");
            return;
        }

        executor.execute(() -> {
            try {
                String query = "parents in '" + folderId + "' and trashed=false and mimeType contains 'image/'";
                FileList result = driveService.files().list()
                        .setQ(query)
                        .setFields("files(id,name,createdTime,size)")
                        .execute();

                callback.onSuccess(result);

            } catch (IOException e) {
                Log.e(TAG, "Erreur lors de la liste des fichiers", e);
                callback.onFailure(e.getMessage());
            }
        });
    }
}