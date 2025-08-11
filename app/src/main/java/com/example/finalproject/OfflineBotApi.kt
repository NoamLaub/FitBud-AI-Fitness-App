package com.example.finalproject

import android.util.Log
import kotlin.random.Random

object OfflineBotApi {
    
    fun askBot(
        message: String,
        chatHistory: List<Pair<String, String>> = emptyList(),
        systemPrompt: String = "You are a friendly, professional fitness and nutrition assistant.",
        onResult: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Simulate a small delay to make it feel more natural
        Thread {
            try {
                Thread.sleep(500 + Random.nextLong(1000)) // 0.5-1.5 second delay
                
                val response = generateResponse(message.lowercase(), systemPrompt)
                onResult(response)
            } catch (e: Exception) {
                Log.e("OfflineBotApi", "Error generating response: ${e.message}")
                onError(e)
            }
        }.start()
    }
    
    private fun generateResponse(message: String, systemPrompt: String): String {
        // Extract user info from system prompt if available
        val userName = extractUserName(systemPrompt)
        val userGoals = extractUserGoals(systemPrompt)
        
        return when {
            // Greetings
            message.contains("hi") || message.contains("hello") || message.contains("hey") -> {
                val greetings = listOf(
                    "Hi${if (userName.isNotEmpty()) " $userName" else ""}! 👋 How can I help you with your fitness journey today?",
                    "Hello! I'm here to support you with nutrition and fitness advice. What's on your mind?",
                    "Hey there! Ready to crush your health goals? What can I help you with?",
                    "Hi! Great to see you! How are you feeling about your fitness progress today?"
                )
                greetings.random()
            }
            
            // Goodbyes
            message.contains("bye") || message.contains("goodbye") || message.contains("see you") -> {
                val goodbyes = listOf(
                    "Goodbye! Keep up the great work with your health journey! 💪",
                    "See you later! Remember to stay hydrated and keep moving! 🌟",
                    "Bye! You've got this - keep making those healthy choices! ✨",
                    "Take care! Looking forward to supporting you on your next check-in! 🎯"
                )
                goodbyes.random()
            }
            
            // Meal/food related
            message.contains("meal") || message.contains("food") || message.contains("eat") || 
            message.contains("hungry") || message.contains("recipe") || message.contains("cook") -> {
                val mealAdvice = listOf(
                    "🍽️ For healthy meals, focus on lean proteins, colorful vegetables, and whole grains. What type of meal are you planning?",
                    "Great question about nutrition! Try to include protein, healthy fats, and fiber in each meal. Need specific suggestions?",
                    "🥗 Meal planning is key to success! Consider prep-friendly options like grilled chicken, quinoa bowls, or veggie stir-fries.",
                    "For balanced nutrition, aim for half your plate to be vegetables, quarter protein, and quarter complex carbs. What sounds good to you?"
                )
                mealAdvice.random()
            }
            
            // Exercise/workout related
            message.contains("exercise") || message.contains("workout") || message.contains("gym") ||
            message.contains("run") || message.contains("cardio") || message.contains("strength") -> {
                val exerciseAdvice = listOf(
                    "💪 Exercise is fantastic! Mix cardio and strength training for best results. What type of workout interests you most?",
                    "Great to hear you're thinking about exercise! Start with activities you enjoy - consistency beats intensity every time.",
                    "🏃‍♀️ Whether it's walking, lifting, or dancing - movement is medicine! What fits your schedule best?",
                    "Exercise tip: Aim for at least 150 minutes of moderate activity per week. Even 10-minute sessions count!"
                )
                exerciseAdvice.random()
            }
            
            // Weight/progress related
            message.contains("weight") || message.contains("scale") || message.contains("progress") ||
            message.contains("goal") || message.contains("lose") || message.contains("gain") -> {
                val progressAdvice = listOf(
                    "📊 Progress isn't just about the scale! Consider how you feel, your energy levels, and how clothes fit too.",
                    "Weight fluctuates daily - focus on weekly trends instead. You're doing great by tracking your journey!",
                    "🎯 Remember, sustainable progress takes time. Small consistent changes lead to big results!",
                    "Your goals are achievable! Focus on building healthy habits rather than just the number on the scale."
                )
                progressAdvice.random()
            }
            
            // Motivation/encouragement
            message.contains("tired") || message.contains("hard") || message.contains("difficult") ||
            message.contains("give up") || message.contains("motivation") || message.contains("help") -> {
                val motivation = listOf(
                    "🌟 I believe in you! Every healthy choice you make is an investment in your future self.",
                    "It's normal to have challenging days. What matters is that you keep showing up for yourself!",
                    "💪 You're stronger than you think! Remember why you started and how far you've already come.",
                    "Tough days don't last, but tough people do! What's one small thing you can do today to feel better?"
                )
                motivation.random()
            }
            
            // Water/hydration
            message.contains("water") || message.contains("drink") || message.contains("hydrat") -> {
                val hydrationTips = listOf(
                    "💧 Great question! Aim for 8-10 glasses of water daily. Add lemon or cucumber for variety!",
                    "Hydration is so important! Try keeping a water bottle nearby as a visual reminder to drink more.",
                    "🚰 Pro tip: Drink a glass of water before each meal - it helps with digestion and portion control!",
                    "If plain water is boring, try herbal teas or sparkling water with a splash of fruit juice!"
                )
                hydrationTips.random()
            }
            
            // Sleep related
            message.contains("sleep") || message.contains("tired") || message.contains("rest") -> {
                val sleepAdvice = listOf(
                    "😴 Sleep is crucial for recovery and weight management! Aim for 7-9 hours per night.",
                    "Good sleep hygiene helps: consistent bedtime, cool room, no screens 1 hour before bed.",
                    "🌙 Quality sleep supports your fitness goals by regulating hunger hormones and energy levels.",
                    "Rest is when your body repairs and grows stronger. Don't underestimate the power of good sleep!"
                )
                sleepAdvice.random()
            }
            
            // Simple responses
            message.length <= 3 -> {
                val shortResponses = listOf(
                    "I'm here to help! What would you like to know about nutrition or fitness?",
                    "Tell me more! How can I support your health journey today?",
                    "What's on your mind? I'm here to help with any health or fitness questions!",
                    "I'm listening! What can I help you with regarding your wellness goals?"
                )
                shortResponses.random()
            }
            
            // Default helpful response
            else -> {
                val defaultResponses = listOf(
                    "That's a great question! While I focus on fitness and nutrition, I'm here to support your health journey. What specific area would you like help with?",
                    "I'm your fitness and nutrition buddy! Whether it's meal planning, exercise tips, or motivation - I'm here to help. What interests you most?",
                    "Thanks for sharing! I specialize in health, fitness, and nutrition guidance. How can I help you reach your wellness goals?",
                    "I love that you're thinking about your health! Let me know if you need advice on workouts, meals, or staying motivated. What sounds most helpful right now?"
                )
                defaultResponses.random()
            }
        }
    }
    
    private fun extractUserName(systemPrompt: String): String {
        return try {
            val namePattern = "Name: ([^\\n]+)".toRegex()
            val match = namePattern.find(systemPrompt)
            match?.groupValues?.get(1)?.split(" ")?.first() ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun extractUserGoals(systemPrompt: String): String {
        return try {
            val goalPattern = "Destination Weight: ([^\\n]+)".toRegex()
            val match = goalPattern.find(systemPrompt)
            match?.groupValues?.get(1) ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
