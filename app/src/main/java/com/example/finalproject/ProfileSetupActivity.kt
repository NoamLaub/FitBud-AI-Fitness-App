package com.example.finalproject

import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import android.graphics.Rect
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent

fun sanitizeUserProfile(profile: Map<String, Any?>): Map<String, Any?> {
    return profile.toMutableMap().apply {
        this["allergies"] = (this["allergies"] as? List<*>) ?: emptyList<String>()
        this["dislikedFoods"] = (this["dislikedFoods"] as? List<*>) ?: emptyList<String>()
        this["favoriteMealTimes"] = (this["favoriteMealTimes"] as? List<*>) ?: emptyList<String>()
        this["weightHistory"] = (this["weightHistory"] as? List<*>) ?: emptyList<Map<String, Any>>()
    }
}

class ProfileSetupActivity : AppCompatActivity() {
    private var lastFocusedView: View? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        val rootView = window.decorView.findViewById<View>(android.R.id.content)
        val scrollView = findViewById<ScrollView>(R.id.profileScrollView)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            rootView.getWindowVisibleDisplayFrame(r)
            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - r.bottom
            if (keypadHeight > screenHeight * 0.15) { // Keyboard is open
                lastFocusedView?.let { v ->
                    scrollView?.post {
                        scrollView.smoothScrollTo(0, v.top)
                    }
                }
            }
        }
        val focusListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) lastFocusedView = v
        }
        val editTextIds = listOf(
            R.id.nameEditText, R.id.surnameEditText, R.id.ageEditText, R.id.currentWeightEditText,
            R.id.destinationWeightEditText, R.id.allergiesEditText, R.id.dislikedFoodsEditText,
            R.id.favoriteMealTimesEditText, R.id.desiredDailyCaloriesEditText, R.id.desiredDailyProteinEditText,
            R.id.exerciseFrequencyEditText, R.id.weighInDayEditText
        )
        for (id in editTextIds) {
            findViewById<EditText>(id)?.onFocusChangeListener = focusListener
        }

        val nameEditText = findViewById<EditText>(R.id.nameEditText)
        val surnameEditText = findViewById<EditText>(R.id.surnameEditText)
        val ageEditText = findViewById<EditText>(R.id.ageEditText)
        val genderSpinner = findViewById<Spinner>(R.id.genderSpinner)
        val currentWeightEditText = findViewById<EditText>(R.id.currentWeightEditText)
        val destinationWeightEditText = findViewById<EditText>(R.id.destinationWeightEditText)
        val allergiesEditText = findViewById<EditText>(R.id.allergiesEditText)
        val dislikedFoodsEditText = findViewById<EditText>(R.id.dislikedFoodsEditText)
        val favoriteMealTimesEditText = findViewById<EditText>(R.id.favoriteMealTimesEditText)
        val desiredDailyCaloriesEditText = findViewById<EditText>(R.id.desiredDailyCaloriesEditText)
        val desiredDailyProteinEditText = findViewById<EditText>(R.id.desiredDailyProteinEditText)
        val exerciseFrequencyEditText = findViewById<EditText>(R.id.exerciseFrequencyEditText)
        val weighInDayEditText = findViewById<EditText>(R.id.weighInDayEditText)
        val saveProfileButton = findViewById<Button>(R.id.saveProfileButton)

        // Set gender spinner options
        val genderAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.gender_array, // or your gender array resource
            R.layout.spinner_item // use your new custom item layout
        )
        genderAdapter.setDropDownViewResource(R.layout.spinner_item)
        genderSpinner.adapter = genderAdapter

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            
            return
        }

        // --- Pre-fill fields in edit mode ---
        val editMode = intent.getBooleanExtra("edit_mode", false)
        if (editMode) {
    currentWeightEditText.isEnabled = false
    currentWeightEditText.setTextColor(resources.getColor(android.R.color.darker_gray))
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
            userRef.get().addOnSuccessListener { snap ->
                val profile = snap.value as? Map<*, *> ?: emptyMap<String, Any?>()
                nameEditText.setText(profile["name"]?.toString() ?: "")
                surnameEditText.setText(profile["surname"]?.toString() ?: "")
                ageEditText.setText(profile["age"]?.toString() ?: "")
                // Gender spinner
                val genderValue = profile["gender"]?.toString() ?: ""
                val genderPos = (0 until genderAdapter.count).firstOrNull { genderAdapter.getItem(it).toString().equals(genderValue, ignoreCase = true) } ?: 0
                genderSpinner.setSelection(genderPos)
                currentWeightEditText.setText(profile["currentWeight"]?.toString() ?: "")
                destinationWeightEditText.setText(profile["destinationWeight"]?.toString() ?: "")
                allergiesEditText.setText(profile["allergies"]?.let {
                    if (it is List<*>) it.joinToString(", ") else it.toString()
                } ?: "")
                dislikedFoodsEditText.setText(profile["dislikedFoods"]?.let {
                    if (it is List<*>) it.joinToString(", ") else it.toString()
                } ?: "")
                favoriteMealTimesEditText.setText(profile["favoriteMealTimes"]?.let {
                    if (it is List<*>) it.joinToString(",") else it.toString()
                } ?: "")
                desiredDailyCaloriesEditText.setText(profile["desiredDailyCalories"]?.toString() ?: "")
                desiredDailyProteinEditText.setText(profile["desiredDailyProtein"]?.toString() ?: "")
                exerciseFrequencyEditText.setText(profile["exerciseFrequency"]?.toString() ?: "")
                weighInDayEditText.setText(profile["weighInDay"]?.toString() ?: "")
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to load profile for editing.", Toast.LENGTH_SHORT).show()
            }
        }

        saveProfileButton.setOnClickListener {
            Log.d("ProfileSetup", "Save button pressed")
            
            val name = nameEditText.text.toString().trim()
            val surname = surnameEditText.text.toString().trim()
            val age = ageEditText.text.toString().trim().toIntOrNull()
            val gender = genderSpinner.selectedItem.toString()
            val currentWeight = currentWeightEditText.text.toString().trim().toFloatOrNull()
            val destinationWeight = destinationWeightEditText.text.toString().trim().toFloatOrNull()
            val allergies = allergiesEditText.text.toString().trim()
            val dislikedFoods = dislikedFoodsEditText.text.toString().trim()

            // Parse meal times as a list of strings
            val mealTimesInput = favoriteMealTimesEditText.text.toString()
            val favoriteMealTimes = mealTimesInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            val desiredDailyCalories = desiredDailyCaloriesEditText.text.toString().trim().toIntOrNull()
            val desiredDailyProtein = desiredDailyProteinEditText.text.toString().trim().toIntOrNull()
            val exerciseFrequency = exerciseFrequencyEditText.text.toString().trim().toIntOrNull()
            val weighInDay = weighInDayEditText.text.toString().trim()

            if (name.isEmpty() || surname.isEmpty() || age == null || currentWeight == null || destinationWeight == null || desiredDailyCalories == null || desiredDailyProtein == null || exerciseFrequency == null || weighInDay.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
                Log.d("ProfileSetup", "Validation failed: name=$name, surname=$surname, age=$age, currentWeight=$currentWeight, destinationWeight=$destinationWeight, desiredDailyCalories=$desiredDailyCalories, desiredDailyProtein=$desiredDailyProtein, exerciseFrequency=$exerciseFrequency, weighInDay=$weighInDay")
                return@setOnClickListener
            }

            val userProfile = mapOf(
                "userId" to userId,
                "name" to name,
                "surname" to surname,
                "age" to age,
                "gender" to gender,
                "currentWeight" to currentWeight,
                "destinationWeight" to destinationWeight,
                "allergies" to allergies,
                "dislikedFoods" to dislikedFoods,
                "favoriteMealTimes" to favoriteMealTimes,
                "desiredDailyCalories" to desiredDailyCalories,
                "desiredDailyProtein" to desiredDailyProtein,
                "exerciseFrequency" to exerciseFrequency,
                "weighInDay" to weighInDay
            )

            Log.d("ProfileSetup", "Attempting to write profile: $userProfile")

            val sanitizedProfile = sanitizeUserProfile(userProfile)
            FirebaseDatabase.getInstance().getReference("users").child(userId).setValue(sanitizedProfile)
    .addOnSuccessListener {
        Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show()

        // Assign users to experts with timeout fallback
        var navigationCompleted = false
        
        // Set a timeout to ensure navigation happens even if expert assignment fails
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!navigationCompleted) {
                navigationCompleted = true
                Log.d("ProfileSetup", "Expert assignment timeout - proceeding to MainActivity")
                try {
                    val mainIntent = Intent(this, MainActivity::class.java)
                    mainIntent.putExtra("refresh_reminders", true)
                    mainIntent.putExtra("user_profile_map", HashMap(userProfile))
                    startActivity(mainIntent)
                } catch (e: Exception) {
                    startActivity(Intent(this, MainActivity::class.java))
                }
                finish()
            }
        }, 3000) // 3 second timeout
        
        // Try expert assignment
        ExpertAssignmentUtil.rebalanceAssignments(FirebaseDatabase.getInstance()) {
            if (!navigationCompleted) {
                navigationCompleted = true
                Log.d("ProfileSetup", "Expert assignment completed - proceeding to MainActivity")
                try {
                    val mainIntent = Intent(this, MainActivity::class.java)
                    mainIntent.putExtra("refresh_reminders", true)
                    mainIntent.putExtra("user_profile_map", HashMap(userProfile))
                    startActivity(mainIntent)
                } catch (e: Exception) {
                    startActivity(Intent(this, MainActivity::class.java))
                }
                finish()
            }
        }
    }
    .addOnFailureListener { e ->
        Toast.makeText(this, "Failed to save profile: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
        }
    }
}
