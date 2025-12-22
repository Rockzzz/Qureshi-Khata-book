package com.example.tabelahisabapp.data.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.example.tabelahisabapp.data.preferences.BackupPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Google Drive backup operations:
 * - Google Sign-In authentication
 * - Upload backups to Google Drive
 * - Download/restore from Google Drive
 * - Manage backup retention (30 days)
 */
@Singleton
class GoogleDriveBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupPreferences: BackupPreferences
) {
    companion object {
        private const val APP_FOLDER_NAME = "UdhaarLedger"
        private const val DAILY_FOLDER_NAME = "Daily"
        private const val MIME_TYPE_JSON = "application/json"
        private const val MIME_TYPE_FOLDER = "application/vnd.google-apps.folder"
    }

    private var driveService: Drive? = null
    private var currentAccount: GoogleSignInAccount? = null

    // ============ SIGN-IN ============

    /**
     * Creates the Google Sign-In client configured for Drive access
     */
    fun getSignInClient(): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        
        return GoogleSignIn.getClient(context, signInOptions)
    }

    /**
     * Returns the sign-in intent to launch
     */
    fun getSignInIntent(): Intent {
        return getSignInClient().signInIntent
    }

    /**
     * Check if user is already signed in
     */
    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))
    }

    /**
     * Get the signed-in account email
     */
    fun getSignedInEmail(): String? {
        return GoogleSignIn.getLastSignedInAccount(context)?.email
    }

    /**
     * Handle sign-in result from activity
     */
    suspend fun handleSignInResult(data: Intent?): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (data == null) {
                return@withContext Result.failure(Exception("Sign-in cancelled"))
            }
            
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            
            // Use addOnSuccessListener pattern or getResult with exception type
            val account = try {
                task.getResult(Exception::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext Result.failure(Exception("Sign-in failed: ${e.message}"))
            }
            
            if (account != null) {
                currentAccount = account
                initializeDriveService(account)
                backupPreferences.setGoogleDriveEnabled(true)
                backupPreferences.setGoogleDriveAccount(account.email)
                Result.success(account.email ?: "Connected")
            } else {
                Result.failure(Exception("Sign-in failed: No account returned"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Sign-in error: ${e.message}"))
        }
    }

    /**
     * Sign out from Google Drive
     */
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Use await to properly wait for sign out
            getSignInClient().signOut().await()
            
            driveService = null
            currentAccount = null
            backupPreferences.setGoogleDriveEnabled(false)
            backupPreferences.setGoogleDriveAccount(null)
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Initialize Drive service with the signed-in account
     * Called from IO thread only
     */
    private fun initializeDriveService(account: GoogleSignInAccount) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = account.account

            driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("UdhaarLedger")
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
            driveService = null
        }
    }

    /**
     * Ensure Drive service is initialized - must be called from IO thread
     */
    private suspend fun ensureDriveService(): Drive? = withContext(Dispatchers.IO) {
        if (driveService != null) return@withContext driveService
        
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null) {
                initializeDriveService(account)
                currentAccount = account
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        driveService
    }

    // ============ FOLDER MANAGEMENT ============

    /**
     * Get or create the app folder in Google Drive
     */
    private suspend fun getOrCreateAppFolder(): String? = withContext(Dispatchers.IO) {
        val drive = ensureDriveService() ?: return@withContext null
        
        try {
            // Check if folder exists
            val result = drive.files().list()
                .setQ("name = '$APP_FOLDER_NAME' and mimeType = '$MIME_TYPE_FOLDER' and trashed = false")
                .setSpaces("drive")
                .execute()

            if (result.files.isNotEmpty()) {
                return@withContext result.files[0].id
            }

            // Create folder
            val folderMetadata = com.google.api.services.drive.model.File()
            folderMetadata.name = APP_FOLDER_NAME
            folderMetadata.mimeType = MIME_TYPE_FOLDER

            val folder = drive.files().create(folderMetadata)
                .setFields("id")
                .execute()

            folder.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get or create Daily subfolder
     */
    private suspend fun getOrCreateDailyFolder(parentFolderId: String): String? = withContext(Dispatchers.IO) {
        val drive = ensureDriveService() ?: return@withContext null
        
        try {
            val result = drive.files().list()
                .setQ("name = '$DAILY_FOLDER_NAME' and '$parentFolderId' in parents and mimeType = '$MIME_TYPE_FOLDER' and trashed = false")
                .setSpaces("drive")
                .execute()

            if (result.files.isNotEmpty()) {
                return@withContext result.files[0].id
            }

            val folderMetadata = com.google.api.services.drive.model.File()
            folderMetadata.name = DAILY_FOLDER_NAME
            folderMetadata.mimeType = MIME_TYPE_FOLDER
            folderMetadata.parents = listOf(parentFolderId)

            val folder = drive.files().create(folderMetadata)
                .setFields("id")
                .execute()

            folder.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ============ UPLOAD ============

    /**
     * Upload a backup file to Google Drive
     */
    suspend fun uploadBackup(localFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val drive = ensureDriveService() 
                ?: return@withContext Result.failure(Exception("Not signed in to Google Drive"))

            val appFolderId = getOrCreateAppFolder()
                ?: return@withContext Result.failure(Exception("Failed to create app folder"))

            val dailyFolderId = getOrCreateDailyFolder(appFolderId)
                ?: return@withContext Result.failure(Exception("Failed to create daily folder"))

            // Check if file already exists (to update instead of create duplicate)
            val existingFiles = drive.files().list()
                .setQ("name = '${localFile.name}' and '$dailyFolderId' in parents and trashed = false")
                .setSpaces("drive")
                .execute()

            val fileMetadata = com.google.api.services.drive.model.File()
            fileMetadata.name = localFile.name

            val mediaContent = FileContent(MIME_TYPE_JSON, localFile)

            val driveFile = if (existingFiles.files.isNotEmpty()) {
                // Update existing file
                drive.files().update(existingFiles.files[0].id, fileMetadata, mediaContent)
                    .execute()
            } else {
                // Create new file
                fileMetadata.parents = listOf(dailyFolderId)
                drive.files().create(fileMetadata, mediaContent)
                    .setFields("id, name")
                    .execute()
            }

            // Update last sync time
            backupPreferences.setLastGoogleDriveSync(System.currentTimeMillis())

            Result.success(driveFile.id)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Upload the latest backup to Google Drive
     */
    suspend fun syncLatestBackup(backupManager: BackupManager): Result<String> = withContext(Dispatchers.IO) {
        try {
            val latestBackup = backupManager.getLatestBackup()
                ?: return@withContext Result.failure(Exception("No backup available to sync"))

            val file = File(latestBackup.filePath)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("Backup file not found"))
            }

            uploadBackup(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============ DOWNLOAD / RESTORE ============

    /**
     * List available backups from Google Drive
     */
    suspend fun listDriveBackups(): Result<List<DriveBackupInfo>> = withContext(Dispatchers.IO) {
        try {
            val drive = ensureDriveService()
                ?: return@withContext Result.failure(Exception("Not signed in to Google Drive"))

            val appFolderId = getOrCreateAppFolder()
                ?: return@withContext Result.failure(Exception("App folder not found"))

            val dailyFolderId = getOrCreateDailyFolder(appFolderId)
                ?: return@withContext Result.failure(Exception("Daily folder not found"))

            val result = drive.files().list()
                .setQ("'$dailyFolderId' in parents and mimeType = '$MIME_TYPE_JSON' and trashed = false")
                .setSpaces("drive")
                .setFields("files(id, name, createdTime, size)")
                .setOrderBy("createdTime desc")
                .setPageSize(30)
                .execute()

            val backups = result.files.map { file ->
                DriveBackupInfo(
                    id = file.id,
                    name = file.name,
                    createdTime = file.createdTime?.value ?: 0L,
                    sizeBytes = file.getSize()?.toLong() ?: 0L
                )
            }

            Result.success(backups)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Download a backup from Google Drive
     */
    suspend fun downloadBackup(driveFileId: String, destinationFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val drive = ensureDriveService()
                ?: return@withContext Result.failure(Exception("Not signed in to Google Drive"))

            FileOutputStream(destinationFile).use { outputStream ->
                drive.files().get(driveFileId)
                    .executeMediaAndDownloadTo(outputStream)
            }

            Result.success(destinationFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // ============ CLEANUP ============

    /**
     * Delete old backups from Google Drive (keep last 30 days)
     */
    suspend fun cleanupOldDriveBackups(keepDays: Int = 30): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val drive = ensureDriveService()
                ?: return@withContext Result.failure(Exception("Not signed in to Google Drive"))

            val cutoffTime = System.currentTimeMillis() - (keepDays * 24 * 60 * 60 * 1000L)
            val appFolderId = getOrCreateAppFolder() ?: return@withContext Result.success(0)
            val dailyFolderId = getOrCreateDailyFolder(appFolderId) ?: return@withContext Result.success(0)

            val result = drive.files().list()
                .setQ("'$dailyFolderId' in parents and trashed = false")
                .setSpaces("drive")
                .setFields("files(id, name, createdTime)")
                .execute()

            var deletedCount = 0
            result.files.forEach { file ->
                val createdTime = file.createdTime?.value ?: 0L
                if (createdTime < cutoffTime) {
                    drive.files().delete(file.id).execute()
                    deletedCount++
                }
            }

            Result.success(deletedCount)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}

/**
 * Info about a backup stored in Google Drive
 */
data class DriveBackupInfo(
    val id: String,
    val name: String,
    val createdTime: Long,
    val sizeBytes: Long
)
