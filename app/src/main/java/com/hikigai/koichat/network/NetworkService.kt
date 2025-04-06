package com.hikigai.koichat.network

import com.hikigai.koichat.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import com.google.gson.Gson
import android.util.Log
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkService private constructor() {
    companion object {
        @Volatile
        private var instance: NetworkService? = null

        fun getInstance(): NetworkService {
            return instance ?: synchronized(this) {
                instance ?: NetworkService().also { instance = it }
            }
        }
    }

    private val gson = Gson()

    suspend fun getDiagnosis(notes: String): List<DiagnosisResponse> = withContext(Dispatchers.IO) {
        try {
            val url = URL(APIConfig.BASE_URL + APIConfig.DIAGNOSIS_ENDPOINT)
            Log.d("NetworkService", "Making request to: ${url.toString()}")
            
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 30000
                readTimeout = 30000
                
                APIConfig.HEADERS.forEach { (key, value) ->
                    setRequestProperty(key, value)
                }
            }

            Log.d("NetworkService", "Headers: ${connection.requestProperties}")

            try {
                // Create request body
                val body = JSONObject().apply {
                    put("notes", notes)
                }
                
                val bodyString = body.toString()
                Log.d("NetworkService", "Body: $bodyString")

                connection.outputStream.use { os ->
                    os.write(bodyString.toByteArray())
                }

                val responseCode = connection.responseCode
                Log.d("NetworkService", "Response status code: $responseCode")

                if (responseCode !in 200..299) {
                    val errorStream = connection.errorStream ?: connection.inputStream
                    val errorResponse = BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                    Log.e("NetworkService", "Error response: $errorResponse")
                    
                    try {
                        val errorJson = JSONObject(errorResponse)
                        val errorMessage = errorJson.optString("message", "Unknown error")
                        throw NetworkError.ServerError(errorMessage)
                    } catch (e: Exception) {
                        throw NetworkError.InvalidResponse(responseCode)
                    }
                }

                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                Log.d("NetworkService", "Response data: $response")

                if (response.isEmpty()) {
                    throw NetworkError.NoData
                }

                try {
                    val apiResponse = gson.fromJson(response, APIResponse::class.java)
                    if (apiResponse.status != "success") {
                        return@withContext emptyList()
                    }
                    return@withContext apiResponse.data
                } catch (e: Exception) {
                    Log.e("NetworkService", "Decoding error: ${e.message}")
                    throw NetworkError.DecodingError
                }
            } catch (e: Exception) {
                when (e) {
                    is SocketTimeoutException -> throw NetworkError.NetworkConnectionError
                    is UnknownHostException -> throw NetworkError.NetworkConnectionError
                    else -> throw e
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            when (e) {
                is NetworkError -> throw e
                else -> throw NetworkError.NetworkConnectionError
            }
        }
    }

    suspend fun sendFeedback(
        notes: String,
        response: List<DiagnosisResponse>,
        feedback: FeedbackType,
        doctorId: String,
        doctorName: String
    ) = withContext(Dispatchers.IO) {
        try {
            val url = URL(APIConfig.BASE_URL + APIConfig.FEEDBACK_ENDPOINT)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 30000
                readTimeout = 30000
                APIConfig.HEADERS.forEach { (key, value) ->
                    setRequestProperty(key, value)
                }
            }

            try {
                val body = JSONObject().apply {
                    put("notes", notes)
                    put("response", gson.toJson(response))
                    put("feedback", feedback.value)
                    put("doctor_id", doctorId)
                    put("doctor_name", doctorName)
                }

                connection.outputStream.use { os ->
                    os.write(body.toString().toByteArray())
                }

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw NetworkError.InvalidResponse(responseCode)
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            when (e) {
                is NetworkError -> throw e
                is SocketTimeoutException -> throw NetworkError.NetworkConnectionError
                is UnknownHostException -> throw NetworkError.NetworkConnectionError
                else -> throw NetworkError.NetworkConnectionError
            }
        }
    }
} 