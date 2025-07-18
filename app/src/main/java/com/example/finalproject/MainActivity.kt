package com.example.finalproject

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var chatListView: ListView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var greetingTextView: TextView
    private val chatHistory = mutableListOf<Pair<String, String>>() // (role, content)
    private lateinit var adapter: ChatMessageAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var logWeightButton: Button
    private lateinit var editProfileButton: Button
    private val encouragementMessages = listOf(
        "You're doing great! Every step counts.",
        "Remember: Progress, not perfection.",
        "Stay positive and keep moving forward!",
        "Believe in yourself—you've got this!",
        "Small steps every day add up to big changes."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        greetingTextView = findViewById(R.id.greetingTextView)
        chatListView = findViewById(R.id.chatListView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        logWeightButton = findViewById(R.id.logWeightButton)
        editProfileButton = findViewById(R.id.editProfileButton)
        logWeightButton.visibility = View.GONE // Hide by default, will show if today is weigh-in day

        adapter = ChatMessageAdapter(this, chatHistory, { position: Int, message: Pair<String, String> ->
            val (role, content) = message

        // Always scroll to last message when entering chat
        chatListView.post {
            chatListView.smoothScrollToPosition(adapter.count - 1)
        }
            if (role == "assistant") {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId == null) return@ChatMessageAdapter
                val chatRef = FirebaseDatabase.getInstance().getReference("chat_history").child(userId)
                chatRef.get().addOnSuccessListener { snapshot ->
                    val key = snapshot.children.firstOrNull {
                        it.child("role").getValue(String::class.java) == "assistant" &&
                        it.child("content").getValue(String::class.java) == content
                    }?.key
                    if (key != null) {
                        val reported = snapshot.child(key).child("reported").getValue(Boolean::class.java) ?: false
                        if (reported) {
                            
                        } else {
                            chatRef.child(key).child("reported").setValue(true)
                            Toast.makeText(this, "Message reported to expert.", Toast.LENGTH_SHORT).show()
                            // Notify expert (create a node under /reports for expert review)
                            val usersRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
                            usersRef.child("assignedExpertId").get().addOnSuccessListener { expertSnap ->
                                val expertId = expertSnap.getValue(String::class.java)
                                if (!expertId.isNullOrBlank()) {
                                    val reportRef = FirebaseDatabase.getInstance().getReference("reports").push()
                                    reportRef.setValue(mapOf(
                                        "userId" to userId,
                                        "expertId" to expertId,
                                        "messageKey" to key,
                                        "content" to content,
                                        "timestamp" to System.currentTimeMillis()
                                    ))
                                }
                            }
                            
                        }
                    }
                }
            }
        })
        chatListView.adapter = adapter

        // --- Ensure chat auto-scrolls when keyboard opens ---
        val rootView = window.decorView.findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val r = android.graphics.Rect()
            rootView.getWindowVisibleDisplayFrame(r)
            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - r.bottom
            if (keypadHeight > screenHeight * 0.15) {
                // Keyboard is open, scroll to last message
                chatListView.post {
                    chatListView.smoothScrollToPosition(adapter.count - 1)
                }
            }
        }

        // --- Handle immediate reminder/chatbot refresh after profile edit ---
        val refreshReminders = intent.getBooleanExtra("refresh_reminders", false)
        @Suppress("DEPRECATION")
        val userProfileMap: Map<String, Any?>? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("user_profile_map", HashMap::class.java) as? Map<String, Any?>
        } else {
            @Suppress("UNCHECKED_CAST")
            intent.getSerializableExtra("user_profile_map") as? Map<String, Any?>
        }
        if (refreshReminders && userProfileMap != null) {
            @Suppress("UNCHECKED_CAST")
            scheduleMealAndWeighInReminders(userProfileMap as Map<String, Any?>)
            val systemPrompt = buildSystemPromptFromProfile(userProfileMap)
            adapter.notifyDataSetChanged()
            chatListView.setSelection(chatHistory.size - 1)

            Toast.makeText(this, "Profile updated! Reminders and chatbot context refreshed.", Toast.LENGTH_SHORT).show()
        }

        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        showDailyEncouragementIfNeeded()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            // --- Check if any meal notification is due within the next hour and not yet triggered today ---
            val now = java.util.Calendar.getInstance()
            now.timeInMillis = 1752848122000L // 2025-07-18T13:35:22+03:00 in millis
            val todayKey = "meal_notify_" + now.get(java.util.Calendar.YEAR) + "_" + now.get(java.util.Calendar.DAY_OF_YEAR)
            val mealNotifiedSet = sharedPreferences.getStringSet(todayKey, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            val mealTimes = (userProfileMap?.get("favoriteMealTimes") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            mealTimes.forEachIndexed { idx, mealTimeStr ->
                val parts = mealTimeStr.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toIntOrNull() ?: return@forEachIndexed
                    val minute = parts[1].toIntOrNull() ?: 0
                    val mealCal = java.util.Calendar.getInstance().apply {
                        timeInMillis = now.timeInMillis
                        set(java.util.Calendar.HOUR_OF_DAY, hour)
                        set(java.util.Calendar.MINUTE, minute)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val diffMillis = mealCal.timeInMillis - now.timeInMillis
                    if (diffMillis in 0..3600000 && !mealNotifiedSet.contains(mealTimeStr)) {
                        // Less than 1 hour to meal and not notified yet
                        // Send notification immediately
                        val intent = android.content.Intent(this, ReminderReceiver::class.java).apply {
                            putExtra("title", "Meal Reminder")
                            putExtra("text", "Your meal is coming up in ${diffMillis/60000} minutes! Open the app for 3 personalized meal suggestions.")
                        }
                        sendBroadcast(intent)
                        mealNotifiedSet.add(mealTimeStr)
                        sharedPreferences.edit().putStringSet(todayKey, mealNotifiedSet).apply()
                    }
                }
            }
            if (!refreshReminders) {
                val dbRef = FirebaseDatabase.getInstance().getReference("chat_history").child(userId)
                dbRef.get().addOnSuccessListener { snapshot ->
                    chatHistory.clear()
                    snapshot.children.forEach {
                        val role = it.child("role").getValue(String::class.java) ?: "user"
                        val content = it.child("content").getValue(String::class.java) ?: ""
                        chatHistory.add(role to content)
                    }
                    // If this is a new user (no chat history), show a welcoming/explanatory message
                    if (chatHistory.isEmpty()) {
                        sendButton.isEnabled = false
                        sendButton.text = "Getting to know you..."
                        val onboardingPrompt = "Welcome the new user to the FitBud app. Use their profile info to introduce yourself as their friendly AI fitness buddy, explain how the app works (logging meals, weight, reminders, chat, editing profile), and encourage them to ask questions or get support. Be warm, clear, and motivating."
                        val systemPrompt = buildSystemPromptFromProfile(userProfileMap ?: emptyMap<String, Any?>())
                        OpenAIApi.askBot(
                            onboardingPrompt,
                            chatHistory,
                            systemPrompt,
                            onResult = { botReply ->
                                runOnUiThread {
                                    chatHistory.add(Pair("assistant", botReply))
                                    adapter.notifyDataSetChanged()
                                    chatListView.post {
                                        chatListView.setSelection(chatHistory.size - 1)
                                    }
                                    sendButton.isEnabled = true
                                    sendButton.text = "Send"
                                }
                            },
                            onError = { error ->
                                runOnUiThread {
                                    chatHistory.add(Pair("assistant", "👋 Welcome to FitBud! (Sorry, I couldn't generate a personalized welcome message right now.)"))
                                    adapter.notifyDataSetChanged()
                                    chatListView.post {
                                        chatListView.setSelection(chatHistory.size - 1)
                                    }
                                    sendButton.isEnabled = true
                                    sendButton.text = "Send"
                                }
                            }
                        )
                    }
                    adapter.notifyDataSetChanged()
                    chatListView.setSelection(chatHistory.size - 1)
                }
            }
        }

        // --- Automatic Meal Suggestion Logic ---
        if (userId != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
        userRef.get().addOnSuccessListener { userSnap ->
            val profileMap = userSnap.value as? Map<*, *> ?: emptyMap<String, Any?>()
            // --- Show Log Weight button only on weigh-in day ---
            val weighInDay = (profileMap["weighInDay"] as? String)?.lowercase()
            val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
            val dayMap = mapOf(
                "sunday" to java.util.Calendar.SUNDAY,
                "monday" to java.util.Calendar.MONDAY,
                "tuesday" to java.util.Calendar.TUESDAY,
                "wednesday" to java.util.Calendar.WEDNESDAY,
                "thursday" to java.util.Calendar.THURSDAY,
                "friday" to java.util.Calendar.FRIDAY,
                "saturday" to java.util.Calendar.SATURDAY
            )
            if (weighInDay != null && dayMap[weighInDay] == today) {
                logWeightButton.visibility = View.VISIBLE
            } else {
                logWeightButton.visibility = View.GONE
            }
            // --- End weigh-in day logic ---
            val mealTimes = (profileMap["favoriteMealTimes"] as? List<*>?)?.mapNotNull { it as? String } ?: emptyList()
            val now = java.util.Calendar.getInstance()
            val nowMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
            val nextMeal = mealTimes
                .mapNotNull { mt ->
                    val parts = mt.split(":")
                    if (parts.size == 2) {
                        val h = parts[0].toIntOrNull() ?: return@mapNotNull null
                        val m = parts[1].toIntOrNull() ?: 0
                        val mealMinutes = h * 60 + m
                        if (mealMinutes > nowMinutes) mealMinutes to mt else null
                    } else null
                }
                .minByOrNull { it.first }
                ?.second
                ?: mealTimes.minByOrNull { mt ->
                    val parts = mt.split(":")
                    if (parts.size == 2) {
                        val h = parts[0].toIntOrNull() ?: 0
                        val m = parts[1].toIntOrNull() ?: 0
                        h * 60 + m
                    } else 0
                }
            val suggestionTag = if (nextMeal != null) "meal_suggestion_$nextMeal" else "meal_suggestion_none"
            if (nextMeal != null && sharedPreferences.getLong(suggestionTag, 0L) < now.timeInMillis - 30*60*1000) {
                val systemPrompt = buildSystemPromptFromProfile(profileMap)
                val mealPrompt = "Suggest 3 healthy meal ideas for my upcoming meal at $nextMeal. List them as 1, 2, 3."
                OpenAIApi.askBot(
                    mealPrompt,
                    chatHistory,
                    systemPrompt,
                    onResult = { botReply ->
                        runOnUiThread {
                            chatHistory.add("assistant" to "Here are 3 meal suggestions for your upcoming meal at $nextMeal:\n$botReply")
                            adapter.notifyDataSetChanged()
                            chatListView.setSelection(chatHistory.size - 1)
                            if (userId != null) {
                                val dbRef = FirebaseDatabase.getInstance().getReference("chat_history").child(userId)
                                dbRef.push().setValue(mapOf("role" to "assistant", "content" to "Here are 3 meal suggestions for your upcoming meal at $nextMeal:\n$botReply"))
                            }
                            sharedPreferences.edit().putLong(suggestionTag, now.timeInMillis).apply()
                        }
                    },
                    onError = { error -> }
                )
            }
        }
        }

        // --- Log Weight button logic ---
        logWeightButton.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
                userRef.child("weightHistory").get().addOnSuccessListener { snap ->
                    val todayCal = java.util.Calendar.getInstance()
                    todayCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    todayCal.set(java.util.Calendar.MINUTE, 0)
                    todayCal.set(java.util.Calendar.SECOND, 0)
                    todayCal.set(java.util.Calendar.MILLISECOND, 0)
                    val todayStart = todayCal.timeInMillis
                    val todayEnd = todayStart + 24 * 60 * 60 * 1000
                    val alreadyLogged = snap.children.any { entry ->
                        val ts = entry.child("timestamp").getValue(Long::class.java)
                        ts != null && ts in todayStart until todayEnd
                    }
                    if (alreadyLogged) {
                        Toast.makeText(this, "You have already logged your weight today.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    val dialogView = layoutInflater.inflate(R.layout.dialog_log_weight, null)
                    val weightEditText = dialogView.findViewById<EditText>(R.id.weightEditText)
                    val alertDialog = AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setCancelable(false)
                        .create()
                    dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
                        alertDialog.dismiss()
                    }
                    dialogView.findViewById<Button>(R.id.saveButton).setOnClickListener {
                        val weightStr = weightEditText.text.toString().trim()
                        val weight = weightStr.toFloatOrNull()
                        if (weight != null && userId != null) {
                            val timestamp = System.currentTimeMillis()
                            val newEntry = mapOf("timestamp" to timestamp, "weight" to weight)
                            userRef.child("weightHistory").push().setValue(newEntry).addOnSuccessListener {
                                val prevWeight = snap.children.maxByOrNull { it.child("timestamp").getValue(Long::class.java) ?: 0L }
                                    ?.child("weight")?.getValue(Float::class.java)
                                val feedbackPrompt = if (prevWeight != null) {
                                    "The user just logged a new weight: $weight kg. Last week's weight was $prevWeight kg. Analyze the progress, encourage them, and provide a short tip if possible."
                                } else {
                                    "The user just logged their first weight: $weight kg. Welcome them to their journey and encourage them."
                                }
                                val systemPrompt = buildSystemPromptFromProfile(mapOf("currentWeight" to weight))
                                OpenAIApi.askBot(
                                    feedbackPrompt,
                                    chatHistory,
                                    systemPrompt,
                                    onResult = { botReply ->
                                        runOnUiThread {
                                            chatHistory.add("assistant" to botReply)
                                            adapter.notifyDataSetChanged()
                                            chatListView.setSelection(chatHistory.size - 1)
                                            val dbRef = FirebaseDatabase.getInstance().getReference("chat_history").child(userId)
                                            dbRef.push().setValue(mapOf("role" to "assistant", "content" to botReply))
                                        }
                                    },
                                    onError = { error -> }
                                )
                                Toast.makeText(this, "Weight logged!", Toast.LENGTH_SHORT).show()
                                alertDialog.dismiss()
                            }
                        } else {
                            Toast.makeText(this, "Invalid weight entered.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    alertDialog.show()
                }
            }
        }

        // --- Edit Profile button logic ---
        editProfileButton.setOnClickListener {
            val intent = Intent(this, ProfileSetupActivity::class.java)
            intent.putExtra("edit_mode", true)
            startActivity(intent)
        }

        // --- Send button logic ---
        sendButton.setOnClickListener {
            val message = messageEditText.text.toString().trim()
            if (message.isNotEmpty()) {
                chatHistory.add("user" to message)
                messageEditText.text.clear()
                adapter.notifyDataSetChanged()
                chatListView.setSelection(chatHistory.size - 1)

                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    val dbRef = FirebaseDatabase.getInstance().getReference("chat_history").child(userId)
                    dbRef.push().setValue(mapOf("role" to "user", "content" to message))
                }
                if (userId != null) {
                    val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
                    userRef.get().addOnSuccessListener { userSnap ->
                        val profileMap = userSnap.value as? Map<*, *> ?: emptyMap<String, Any?>()
                        scheduleMealAndWeighInReminders(profileMap as Map<String, Any?>)
                        val systemPrompt = buildSystemPromptFromProfile(profileMap)
                        OpenAIApi.askBot(
                            message,
                            chatHistory,
                            systemPrompt,
                            onResult = { botReply ->
                                runOnUiThread {
                                    chatHistory.add("assistant" to botReply)
                                    adapter.notifyDataSetChanged()
                                    chatListView.setSelection(chatHistory.size - 1)
                                    val dbRef = FirebaseDatabase.getInstance().getReference("chat_history").child(userId)
                                    dbRef.push().setValue(mapOf("role" to "assistant", "content" to botReply))
                                }
                            },
                            onError = { error ->
                                runOnUiThread {
                                    chatHistory.add("assistant" to "Sorry, I couldn't answer right now.")
                                    adapter.notifyDataSetChanged()
                                    chatListView.setSelection(chatHistory.size - 1)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun buildSystemPromptFromProfile(profile: Map<*, *>): String {
        val name = profile["name"] ?: "Unknown"
        val surname = profile["surname"] ?: ""
        val age = profile["age"] ?: "?"
        val gender = profile["gender"] ?: "?"
        val currentWeight = profile["currentWeight"] ?: "?"
        val destinationWeight = profile["destinationWeight"] ?: "?"
        val allergies = (profile["allergies"] as? List<*>)?.filterIsInstance<String>()?.joinToString(", ") ?: "None"
        val dislikedFoods = (profile["dislikedFoods"] as? List<*>)?.filterIsInstance<String>()?.joinToString(", ") ?: "None"
        val mealTimes = (profile["mealTimes"] as? List<*>)?.filterIsInstance<String>()?.joinToString(", ") ?: "None"
        val calories = profile["calories"] ?: "?"
        val protein = profile["protein"] ?: "?"
        val exercise = profile["exercise"] ?: "?"
        val weighInDay = profile["weighInDay"] ?: "?"

        return """
            User Profile:
            - Name: $name $surname
            - Age: $age
            - Gender: $gender
            - Current Weight: $currentWeight
            - Destination Weight: $destinationWeight
            - Allergies: $allergies
            - Disliked Foods: $dislikedFoods
            - Favorite Meal Times: $mealTimes
            - Desired Daily Calories: $calories
            - Desired Daily Protein: $protein
            - Exercise Frequency: $exercise
            - Weigh-In Day: $weighInDay

            Be supportive, empathetic, and helpful. Use this information to personalize your advice.
        """.trimIndent()
    }

    private fun showDailyEncouragementIfNeeded() {
        val today = Calendar.getInstance()
        val lastShown = sharedPreferences.getLong("encouragement_last_shown", 0L)
        val lastCal = Calendar.getInstance().apply { timeInMillis = lastShown }
        val sameDay = today.get(Calendar.YEAR) == lastCal.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == lastCal.get(Calendar.DAY_OF_YEAR)
        if (!sameDay) {
            val message = encouragementMessages.random()
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            sharedPreferences.edit().putLong("encouragement_last_shown", today.timeInMillis).apply()
        }
    }

    private fun scheduleMealAndWeighInReminders(userProfile: Map<String, Any?>) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        for (idx in 0 until 10) {
            val intent = Intent(this, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this, 100 + idx, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
        run {
            val intent = Intent(this, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this, 200, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reminder_channel",
                "Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        val mealTimes = (userProfile["favoriteMealTimes"] as? List<String>) ?: emptyList()
        mealTimes.forEachIndexed { idx, mealTimeStr ->
            val parts = mealTimeStr.split(":")
            if (parts.size == 2) {
                var hour = parts[0].toIntOrNull() ?: return@forEachIndexed
                val minute = parts[1].toIntOrNull() ?: 0
                hour -= 1
                if (hour < 0) {
                    hour += 24
                }
                val now = java.util.Calendar.getInstance()
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, hour)
                    set(java.util.Calendar.MINUTE, minute)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                    if (before(now)) add(java.util.Calendar.DATE, 1)
                }
                val intent = Intent(this, ReminderReceiver::class.java).apply {
                    putExtra("title", "Meal Reminder")
                    putExtra("text", "Your meal is coming up in 1 hour! Open the app for 3 personalized meal suggestions.")
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    this, 100 + idx, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
        }
        val weighInDay = userProfile["weighInDay"] as? String
        if (weighInDay != null) {
            val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
            val weighInDayNum = when (weighInDay.lowercase()) {
                "sunday" -> java.util.Calendar.SUNDAY
                "monday" -> java.util.Calendar.MONDAY
                "tuesday" -> java.util.Calendar.TUESDAY
                "wednesday" -> java.util.Calendar.WEDNESDAY
                "thursday" -> java.util.Calendar.THURSDAY
                "friday" -> java.util.Calendar.FRIDAY
                "saturday" -> java.util.Calendar.SATURDAY
                else -> null
            }
            if (weighInDayNum != null) {
                val now = java.util.Calendar.getInstance()
                var daysUntil = (weighInDayNum - now.get(java.util.Calendar.DAY_OF_WEEK) + 7) % 7
                if (daysUntil == 0 && now.get(java.util.Calendar.HOUR_OF_DAY) >= 9) daysUntil = 7
                val cal = java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.DATE, daysUntil)
                    set(java.util.Calendar.HOUR_OF_DAY, 9)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val intent = Intent(this, ReminderReceiver::class.java).apply {
                    putExtra("title", "Weigh-In Reminder")
                    putExtra("text", "Today is your weigh-in day! Don't forget to log your weight.")
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    this, 200, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    AlarmManager.INTERVAL_DAY * 7,
                    pendingIntent
                )
            }
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}


