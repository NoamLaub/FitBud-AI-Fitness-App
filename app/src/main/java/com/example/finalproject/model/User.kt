package com.example.finalproject.model

data class WeightEntry(
    val date: String = "", // ISO format
    val weight: Float = 0f
)

// All fields are nullable or have defensive defaults to prevent Firebase deserialization errors
// This allows loading even if types are mismatched or fields are missing in the DB

data class User(
    val userId: String? = null,
    val email: String? = null,
    val name: String? = null,
    val surname: String? = null,
    val age: Any? = null, // Accepts Int, Long, String
    val gender: String? = null,
    val currentWeight: Any? = null, // Accepts Float, Double, Int, String
    val destinationWeight: Any? = null, // Accepts Float, Double, Int, String
    val allergies: List<String>? = emptyList(),
    val dislikedFoods: List<String>? = emptyList(),
    val favoriteMealTimes: List<String>? = emptyList(),
    val desiredDailyCalories: Any? = null, // Accepts Int, Long, String
    val desiredDailyProtein: Any? = null, // Accepts Int, Long, String
    val exerciseFrequency: Any? = null, // Accepts Int, Long, String
    val weighInDay: String? = null,
    val assignedExpertId: String? = null,
    val weightHistory: List<WeightEntry>? = emptyList()
) {
    // Type-safe accessors for UI/logic
    val ageInt: Int?
        get() = when (age) {
            is Int -> age
            is Long -> (age as Long).toInt()
            is String -> age.toIntOrNull()
            else -> null
        }
    val ageString: String?
        get() = age?.toString()
    val currentWeightFloat: Float?
        get() = when (currentWeight) {
            is Float -> currentWeight
            is Double -> (currentWeight as Double).toFloat()
            is Int -> (currentWeight as Int).toFloat()
            is Long -> (currentWeight as Long).toFloat()
            is String -> currentWeight.toFloatOrNull()
            else -> null
        }
    val currentWeightString: String?
        get() = currentWeight?.toString()
    val destinationWeightFloat: Float?
        get() = when (destinationWeight) {
            is Float -> destinationWeight
            is Double -> (destinationWeight as Double).toFloat()
            is Int -> (destinationWeight as Int).toFloat()
            is Long -> (destinationWeight as Long).toFloat()
            is String -> destinationWeight.toFloatOrNull()
            else -> null
        }
    val destinationWeightString: String?
        get() = destinationWeight?.toString()
    val desiredDailyCaloriesInt: Int?
        get() = when (desiredDailyCalories) {
            is Int -> desiredDailyCalories
            is Long -> (desiredDailyCalories as Long).toInt()
            is String -> desiredDailyCalories.toIntOrNull()
            else -> null
        }
    val desiredDailyCaloriesString: String?
        get() = desiredDailyCalories?.toString()
    val desiredDailyProteinInt: Int?
        get() = when (desiredDailyProtein) {
            is Int -> desiredDailyProtein
            is Long -> (desiredDailyProtein as Long).toInt()
            is String -> desiredDailyProtein.toIntOrNull()
            else -> null
        }
    val desiredDailyProteinString: String?
        get() = desiredDailyProtein?.toString()
    val exerciseFrequencyInt: Int?
        get() = when (exerciseFrequency) {
            is Int -> exerciseFrequency
            is Long -> (exerciseFrequency as Long).toInt()
            is String -> exerciseFrequency.toIntOrNull()
            else -> null
        }
    val exerciseFrequencyString: String?
        get() = exerciseFrequency?.toString()
}


