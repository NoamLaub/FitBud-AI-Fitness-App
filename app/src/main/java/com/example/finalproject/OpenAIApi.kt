package com.example.finalproject

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object OpenAIApi {
    private const val API_URL = "https://api.openai.com/v1/chat/completions"
    // TODO: Replace with your actual OpenAI API key or load securely from BuildConfig
    private const val API_KEY = "YOUR_OPENAI_API_KEY_HERE"
    private val client = OkHttpClient()

    fun askBot(
        message: String,
        chatHistory: List<Pair<String, String>>,
        systemPrompt: String = "You are a realistic, friendly companion who can chat about anything, but you have special expertise in health, fitness, and nutrition. Be supportive, empathetic, and helpful.",
        onResult: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val messages = JSONArray()
        
        // Add system prompt
        val systemMessage = JSONObject()
        systemMessage.put("role", "system")
        systemMessage.put("content", systemPrompt)
        messages.put(systemMessage)
        
        // Add chat history
        for ((role, content) in chatHistory) {
            val historyMessage = JSONObject()
            historyMessage.put("role", role)
            historyMessage.put("content", content)
            messages.put(historyMessage)
        }
        
        // Add current user message
        val userMessage = JSONObject()
        userMessage.put("role", "user")
        userMessage.put("content", message)
        messages.put(userMessage)

        val requestBody = JSONObject()
        requestBody.put("model", "gpt-3.5-turbo")
        requestBody.put("messages", messages)
        requestBody.put("max_tokens", 500)
        requestBody.put("temperature", 0.7)

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            requestBody.toString()
        )

        val request = Request.Builder()
            .url(API_URL)
            .post(body)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val jsonResponse = JSONObject(responseBody)
                        val choices = jsonResponse.getJSONArray("choices")
                        if (choices.length() > 0) {
                            val choice = choices.getJSONObject(0)
                            val messageObj = choice.getJSONObject("message")
                            val content = messageObj.getString("content")
                            onResult(content.trim())
                        } else {
                            onError(Exception("No response from AI"))
                        }
                    } else {
                        onError(Exception("API Error: ${response.code} - $responseBody"))
                    }
                } catch (e: Exception) {
                    onError(e)
                }
            }
        })
    }
}
