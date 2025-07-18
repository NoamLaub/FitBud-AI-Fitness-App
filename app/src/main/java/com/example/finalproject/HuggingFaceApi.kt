package com.example.finalproject

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object HuggingFaceApi {
    private const val API_URL = "https://api-inference.huggingface.co/models/facebook/blenderbot-400M-distill"
    private val client = OkHttpClient()

    fun askBot(message: String, onResult: (String) -> Unit, onError: (Exception) -> Unit) {
        // Prompt engineering: keep bot focused on health/fitness/nutrition, friendly and professional
        val prompt = "You are a friendly, professional assistant. Only answer questions about health, fitness, and nutrition. If the question is not about these topics, politely say you can only answer health, fitness, or nutrition questions.\nUser: $message"
        val json = JSONObject().apply {
            put("inputs", prompt)
        }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(API_URL)
            .post(body)
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .build()
        Thread {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody == null) {
                    Log.e("HuggingFaceApi", "API error: ${response.code} body: $responseBody")
                    onError(Exception("API call failed: ${response.code} ${responseBody ?: ""}"))
                    return@Thread
                }
                // DEBUG: Log the full response body for troubleshooting
                Log.d("HuggingFaceApi", "raw response: $responseBody")
                val jsonResponse = JSONArray(responseBody).optJSONObject(0)
                val generatedText = jsonResponse?.optString("generated_text") ?: "(No response)"
                onResult(generatedText)
            } catch (e: Exception) {
                onError(e)
            }
        }.start()
    }
}
