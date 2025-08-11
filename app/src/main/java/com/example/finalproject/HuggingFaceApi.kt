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
    // TODO: Replace with your actual HuggingFace API token or load securely from BuildConfig
    private const val API_TOKEN = "YOUR_HUGGINGFACE_TOKEN_HERE" // Replace with your actual token
    private val client = OkHttpClient()

    fun askBot(
        message: String,
        chatHistory: List<Pair<String, String>> = emptyList(),
        systemPrompt: String = "You are a friendly, professional fitness and nutrition assistant. Be supportive, empathetic, and helpful.",
        onResult: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Build context from chat history and system prompt
        val contextBuilder = StringBuilder()
        contextBuilder.append("$systemPrompt\n\n")
        
        // Add recent chat history for context (last 5 messages to keep it manageable)
        chatHistory.takeLast(5).forEach { (role, content) ->
            when (role) {
                "user" -> contextBuilder.append("User: $content\n")
                "assistant" -> contextBuilder.append("Assistant: $content\n")
            }
        }
        
        contextBuilder.append("User: $message\nAssistant:")
        val prompt = contextBuilder.toString()
        
        val json = JSONObject().apply {
            put("inputs", prompt)
            put("parameters", JSONObject().apply {
                put("max_length", 200)
                put("temperature", 0.7)
                put("do_sample", true)
            })
        }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(API_URL)
            .post(body)
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $API_TOKEN")
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
