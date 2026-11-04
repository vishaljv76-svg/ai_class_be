package com.example.Simtas.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ApiResponse(
    val isSuccess: Boolean,
    val statusCode: Int,
    val jsonObject: JSONObject?,
    val errorMessage: String? = null
)

/**
 * Lightweight, zero-dependency HTTP client for Android communicating with PHP REST APIs.
 */
object ApiClient {

    private const val CONNECT_TIMEOUT_MS = 10000
    private const val READ_TIMEOUT_MS = 15000

    suspend fun get(path: String, token: String? = SessionManager.getToken()): ApiResponse =
        withContext(Dispatchers.IO) {
            executeRequest(method = "GET", path = path, jsonPayload = null, token = token)
        }

    suspend fun post(path: String, jsonPayload: String, token: String? = SessionManager.getToken()): ApiResponse =
        withContext(Dispatchers.IO) {
            executeRequest(method = "POST", path = path, jsonPayload = jsonPayload, token = token)
        }

    suspend fun delete(path: String, token: String? = SessionManager.getToken()): ApiResponse =
        withContext(Dispatchers.IO) {
            executeRequest(method = "DELETE", path = path, jsonPayload = null, token = token)
        }

    suspend fun uploadMultipart(
        path: String,
        inputStream: java.io.InputStream,
        fileName: String,
        fileSize: Long,
        formFields: Map<String, String>,
        token: String? = SessionManager.getToken(),
        onProgress: ((bytesSent: Long, totalBytes: Long) -> Unit)? = null
    ): ApiResponse = withContext(Dispatchers.IO) {
        val fullUrl = if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            val base = ApiConfig.BASE_URL.trimEnd('/')
            val endpoint = path.trimStart('/')
            "$base/$endpoint"
        }

        var connection: HttpURLConnection? = null
        val boundary = "===SmartNotesAudio" + System.currentTimeMillis() + "==="
        val lineEnd = "\r\n"
        val twoHyphens = "--"

        try {
            val url = URL(fullUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = 60000 // 60s timeout for audio uploads
            connection.doInput = true
            connection.doOutput = true
            connection.useCaches = false
            connection.setRequestProperty("Connection", "Keep-Alive")
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("ngrok-skip-browser-warning", "true")

            if (!token.isNullOrEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }

            val outputStream = connection.outputStream

            // 1. Write text form fields
            for ((key, value) in formFields) {
                val fieldHeader = "$twoHyphens$boundary$lineEnd" +
                        "Content-Disposition: form-data; name=\"$key\"$lineEnd$lineEnd" +
                        value + lineEnd
                outputStream.write(fieldHeader.toByteArray(Charsets.UTF_8))
            }

            // 2. Write file header
            val mime = when {
                fileName.endsWith(".mp3", true) -> "audio/mpeg"
                fileName.endsWith(".wav", true) -> "audio/wav"
                fileName.endsWith(".m4a", true) -> "audio/mp4"
                fileName.endsWith(".aac", true) -> "audio/aac"
                fileName.endsWith(".ogg", true) -> "audio/ogg"
                fileName.endsWith(".flac", true) -> "audio/flac"
                else -> "application/octet-stream"
            }
            val fileHeader = "$twoHyphens$boundary$lineEnd" +
                    "Content-Disposition: form-data; name=\"audio\"; filename=\"$fileName\"$lineEnd" +
                    "Content-Type: $mime$lineEnd" +
                    "Content-Transfer-Encoding: binary$lineEnd$lineEnd"
            outputStream.write(fileHeader.toByteArray(Charsets.UTF_8))

            // 3. Stream file bytes in 8KB chunks with progress callback
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesSent = 0L

            inputStream.use { inStream ->
                while (inStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesSent += bytesRead
                    onProgress?.invoke(totalBytesSent, if (fileSize > 0) fileSize else totalBytesSent)
                }
                outputStream.flush()
            }

            // 4. Closing boundary
            val closingBoundary = "$lineEnd$twoHyphens$boundary$twoHyphens$lineEnd"
            outputStream.write(closingBoundary.toByteArray(Charsets.UTF_8))
            outputStream.flush()
            outputStream.close()

            val statusCode = connection.responseCode
            val responseStream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val responseString = BufferedReader(InputStreamReader(responseStream, "UTF-8")).use { reader ->
                reader.readText()
            }

            val json = try {
                if (responseString.trim().startsWith("{")) {
                    JSONObject(responseString)
                } else null
            } catch (_: Exception) {
                null
            }

            val isSuccess = statusCode in 200..299 && (json?.optBoolean("success", true) ?: true)
            val message = if (json != null && json.has("message")) json.optString("message") else if (!isSuccess) "Server returned code $statusCode" else null

            ApiResponse(
                isSuccess = isSuccess,
                statusCode = statusCode,
                jsonObject = json,
                errorMessage = if (!isSuccess) message else null
            )

        } catch (e: Exception) {
            ApiResponse(
                isSuccess = false,
                statusCode = 0,
                jsonObject = null,
                errorMessage = "Upload network error: ${e.localizedMessage ?: "Unable to upload audio"}"
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun executeRequest(
        method: String,
        path: String,
        jsonPayload: String?,
        token: String?
    ): ApiResponse {
        val fullUrl = if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            val base = ApiConfig.BASE_URL.trimEnd('/')
            val endpoint = path.trimStart('/')
            "$base/$endpoint"
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(fullUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("ngrok-skip-browser-warning", "true")

            if (!token.isNullOrEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }

            if (jsonPayload != null && (method == "POST" || method == "PUT" || method == "PATCH")) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                    writer.write(jsonPayload)
                    writer.flush()
                }
            }

            val statusCode = connection.responseCode
            val inputStream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val responseString = BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                reader.readText()
            }

            val json = try {
                if (responseString.trim().startsWith("{")) {
                    JSONObject(responseString)
                } else null
            } catch (e: Exception) {
                null
            }

            val isSuccess = statusCode in 200..299 && (json?.optBoolean("success", true) ?: true)
            val message = if (json != null && json.has("message")) json.optString("message") else if (!isSuccess) "Server returned code $statusCode" else null

            return ApiResponse(
                isSuccess = isSuccess,
                statusCode = statusCode,
                jsonObject = json,
                errorMessage = if (!isSuccess) message else null
            )

        } catch (e: Exception) {
            return ApiResponse(
                isSuccess = false,
                statusCode = 0,
                jsonObject = null,
                errorMessage = "Network error: ${e.localizedMessage ?: "Unable to connect to server"}"
            )
        } finally {
            connection?.disconnect()
        }
    }
}
