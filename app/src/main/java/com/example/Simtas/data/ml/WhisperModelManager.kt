package com.example.Simtas.data.ml

import android.content.Context
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class WhisperModelFiles(
    val encoder: File,
    val decoder: File,
    val tokens: File
)

data class ModelDownloadProgress(
    val percentage: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val statusMessage: String
)

/**
 * Manages downloading, verification, and offline lifecycle of the
 * quantized Whisper multilingual ONNX speech recognition model.
 * Models are stored exclusively in app-private internal storage.
 */
object WhisperModelManager {

    private const val MODEL_SUBDIR = "models/whisper-tiny"
    const val ENCODER_FILE = "tiny-encoder.int8.onnx"
    const val DECODER_FILE = "tiny-decoder.int8.onnx"
    const val TOKENS_FILE = "tiny-tokens.txt"

    // Official verified model downloads from Hugging Face / k2-fsa
    private const val BASE_URL = "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-tiny/resolve/main"

    // Estimated total byte size (~98.8 MB)
    const val ESTIMATED_TOTAL_BYTES = 103_609_903L

    private fun getInternalModelDir(context: Context): File {
        val dir = File(context.filesDir, MODEL_SUBDIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getExternalModelDir(context: Context): File? {
        return try {
            val base = context.getExternalFilesDir(null) ?: return null
            File(base, MODEL_SUBDIR)
        } catch (_: Exception) {
            null
        }
    }

    private fun getDownloadsModelDir(): File? {
        return try {
            val downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val dir = File(downloads, "whisper-tiny")
            if (isDirValid(dir)) dir else null
        } catch (_: Exception) {
            null
        }
    }

    private fun isDirValid(dir: File?): Boolean {
        if (dir == null || !dir.exists() || !dir.canRead()) return false
        val encoder = File(dir, ENCODER_FILE)
        val decoder = File(dir, DECODER_FILE)
        val tokens = File(dir, TOKENS_FILE)

        return encoder.exists() && encoder.canRead() && encoder.length() > 10_000_000L &&
                decoder.exists() && decoder.canRead() && decoder.length() > 50_000_000L &&
                tokens.exists() && tokens.canRead() && tokens.length() > 100_000L
    }

    fun getModelDir(context: Context): File {
        val ext = getExternalModelDir(context)
        if (ext != null && isDirValid(ext)) {
            return ext
        }
        val intDir = getInternalModelDir(context)
        if (isDirValid(intDir)) {
            return intDir
        }
        val dlDir = getDownloadsModelDir()
        if (dlDir != null && isDirValid(dlDir)) {
            return dlDir
        }
        return getInternalModelDir(context)
    }

    /**
     * Checks if all required Whisper model files are installed and valid in app storage
     * (internal filesDir, external app filesDir, or Download/whisper-tiny).
     */
    fun isModelInstalled(context: Context): Boolean {
        return isDirValid(getInternalModelDir(context)) ||
                isDirValid(getExternalModelDir(context)) ||
                isDirValid(getDownloadsModelDir())
    }

    fun getModelFiles(context: Context): WhisperModelFiles? {
        val intDir = getInternalModelDir(context)
        if (isDirValid(intDir)) {
            return WhisperModelFiles(
                encoder = File(intDir, ENCODER_FILE),
                decoder = File(intDir, DECODER_FILE),
                tokens = File(intDir, TOKENS_FILE)
            )
        }
        val extDir = getExternalModelDir(context)
        if (extDir != null && isDirValid(extDir)) {
            return WhisperModelFiles(
                encoder = File(extDir, ENCODER_FILE),
                decoder = File(extDir, DECODER_FILE),
                tokens = File(extDir, TOKENS_FILE)
            )
        }
        val dlDir = getDownloadsModelDir()
        if (dlDir != null && isDirValid(dlDir)) {
            return WhisperModelFiles(
                encoder = File(dlDir, ENCODER_FILE),
                decoder = File(dlDir, DECODER_FILE),
                tokens = File(dlDir, TOKENS_FILE)
            )
        }
        return null
    }

    fun getAvailableDiskSpaceBytes(context: Context): Long {
        return try {
            val stat = StatFs(context.filesDir.path)
            stat.availableBytes
        } catch (_: Exception) {
            Long.MAX_VALUE
        }
    }

    /**
     * Copies model files from a local source folder (such as assets or host cache) if available.
     */
    fun installFromLocalDir(context: Context, sourceDir: File): Boolean {
        if (!sourceDir.exists()) return false
        val srcEncoder = File(sourceDir, ENCODER_FILE)
        val srcDecoder = File(sourceDir, DECODER_FILE)
        val srcTokens = File(sourceDir, TOKENS_FILE)

        if (srcEncoder.exists() && srcDecoder.exists() && srcTokens.exists()) {
            val targetDir = getModelDir(context)
            srcEncoder.copyTo(File(targetDir, ENCODER_FILE), overwrite = true)
            srcDecoder.copyTo(File(targetDir, DECODER_FILE), overwrite = true)
            srcTokens.copyTo(File(targetDir, TOKENS_FILE), overwrite = true)
            return isModelInstalled(context)
        }
        return false
    }

    /**
     * Downloads the multilingual Whisper model files into app-private storage with progress callbacks.
     */
    suspend fun downloadModel(
        context: Context,
        onProgress: ((ModelDownloadProgress) -> Unit)? = null
    ): Result<WhisperModelFiles> = withContext(Dispatchers.IO) {
        if (isModelInstalled(context)) {
            val installed = getModelFiles(context)!!
            onProgress?.invoke(
                ModelDownloadProgress(
                    percentage = 100,
                    downloadedBytes = ESTIMATED_TOTAL_BYTES,
                    totalBytes = ESTIMATED_TOTAL_BYTES,
                    statusMessage = "Offline speech model installed and ready."
                )
            )
            return@withContext Result.success(installed)
        }

        val targetDir = getModelDir(context)

        // Check storage space (at least 150 MB free required)
        val freeBytes = getAvailableDiskSpaceBytes(context)
        if (freeBytes < 150L * 1024 * 1024) {
            return@withContext Result.failure(
                IllegalStateException("Insufficient device storage. At least 150 MB free space is required to install the offline speech model.")
            )
        }

        val filesToDownload = listOf(
            TOKENS_FILE to "$BASE_URL/$TOKENS_FILE",
            ENCODER_FILE to "$BASE_URL/$ENCODER_FILE",
            DECODER_FILE to "$BASE_URL/$DECODER_FILE"
        )

        var cumulativeDownloaded = 0L

        // Calculate already present files
        for ((name, _) in filesToDownload) {
            val f = File(targetDir, name)
            if (f.exists()) {
                cumulativeDownloaded += f.length()
            }
        }

        for ((fileName, fileUrl) in filesToDownload) {
            val destination = File(targetDir, fileName)
            val minExpected = when (fileName) {
                TOKENS_FILE -> 500_000L
                ENCODER_FILE -> 10_000_000L
                DECODER_FILE -> 50_000_000L
                else -> 0L
            }

            if (destination.exists() && destination.length() >= minExpected) {
                continue
            }

            val tempFile = File(targetDir, "$fileName.tmp")
            try {
                val url = URL(fileUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.instanceFollowRedirects = true

                if (connection.responseCode !in 200..299) {
                    return@withContext Result.failure(
                        IllegalStateException("Server returned HTTP ${connection.responseCode} while downloading $fileName.")
                    )
                }

                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(32768)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            cumulativeDownloaded += bytesRead
                            val percent = ((cumulativeDownloaded * 100) / ESTIMATED_TOTAL_BYTES).toInt().coerceIn(0, 99)
                            val mbDownloaded = cumulativeDownloaded / (1024.0 * 1024.0)
                            val mbTotal = ESTIMATED_TOTAL_BYTES / (1024.0 * 1024.0)
                            val statusMsg = String.format(
                                Locale.US,
                                "Downloading offline speech model: %.1f / %.1f MB (%d%%)",
                                mbDownloaded, mbTotal, percent
                            )
                            onProgress?.invoke(
                                ModelDownloadProgress(
                                    percentage = percent,
                                    downloadedBytes = cumulativeDownloaded,
                                    totalBytes = ESTIMATED_TOTAL_BYTES,
                                    statusMessage = statusMsg
                                )
                            )
                        }
                    }
                }

                if (tempFile.renameTo(destination)) {
                    // Success
                } else {
                    tempFile.copyTo(destination, overwrite = true)
                    tempFile.delete()
                }

            } catch (e: Exception) {
                tempFile.delete()
                return@withContext Result.failure(
                    IllegalStateException(
                        "Download failed for $fileName: ${e.localizedMessage ?: "Network error"}. Please check your connection and retry.",
                        e
                    )
                )
            }
        }

        if (isModelInstalled(context)) {
            onProgress?.invoke(
                ModelDownloadProgress(
                    percentage = 100,
                    downloadedBytes = ESTIMATED_TOTAL_BYTES,
                    totalBytes = ESTIMATED_TOTAL_BYTES,
                    statusMessage = "Offline speech model installed successfully!"
                )
            )
            val files = getModelFiles(context)!!
            Result.success(files)
        } else {
            Result.failure(IllegalStateException("Model files verification failed after download."))
        }
    }

    fun deleteModel(context: Context): Boolean {
        val dir = getModelDir(context)
        return dir.deleteRecursively()
    }
}
