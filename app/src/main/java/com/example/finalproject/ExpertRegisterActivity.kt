package com.example.finalproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.example.finalproject.model.Expert

fun sanitizeExpertProfile(profile: Map<String, Any?>): Map<String, Any?> {
    return profile.toMutableMap().apply {
        this["assignedUserIds"] = (this["assignedUserIds"] as? List<*>) ?: emptyList<String>()
    }
}

class ExpertRegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expert_register)

        auth = FirebaseAuth.getInstance()
        val nameEditText = findViewById<EditText>(R.id.expertNameEditText)
        val surnameEditText = findViewById<EditText>(R.id.expertSurnameEditText)
        val emailEditText = findViewById<EditText>(R.id.expertEmailEditText)
        val passwordEditText = findViewById<EditText>(R.id.expertPasswordEditText)
        val registerButton = findViewById<Button>(R.id.expertRegisterButton)
        val errorTextView = findViewById<TextView>(R.id.expertRegisterErrorTextView)

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val surname = surnameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            errorTextView.visibility = TextView.GONE

            if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || password.isEmpty()) {
                errorTextView.text = "Please fill in all fields."
                errorTextView.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val expertId = auth.currentUser?.uid ?: ""
                        val expertProfile = mapOf(
                            "expertId" to expertId,
                            "name" to name,
                            "surname" to surname,
                            "email" to email,
                            "assignedUserIds" to emptyList<String>()
                        )
                        val sanitizedExpertProfile = sanitizeExpertProfile(expertProfile)
                        FirebaseDatabase.getInstance().getReference("experts").child(expertId).setValue(sanitizedExpertProfile)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Expert registered successfully!", Toast.LENGTH_SHORT).show()
                                
                                // Add timeout mechanism for navigation
                                var navigationCompleted = false
                                
                                // Set timeout to ensure navigation happens even if assignment fails
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    if (!navigationCompleted) {
                                        navigationCompleted = true
                                        android.util.Log.d("ExpertRegister", "Assignment timeout - proceeding to dashboard")
                                        startActivity(Intent(this, ExpertDashboardActivity::class.java))
                                        finish()
                                    }
                                }, 5000) // 5 second timeout
                                
                                // Try expert assignment
                                android.util.Log.d("ExpertRegister", "Starting expert assignment rebalance")
                                ExpertAssignmentUtil.rebalanceAssignments(FirebaseDatabase.getInstance()) {
                                    if (!navigationCompleted) {
                                        navigationCompleted = true
                                        android.util.Log.d("ExpertRegister", "Assignment completed - proceeding to dashboard")
                                        startActivity(Intent(this, ExpertDashboardActivity::class.java))
                                        finish()
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                errorTextView.text = "Failed to save expert: ${e.message ?: e.toString()}"
                                errorTextView.visibility = TextView.VISIBLE
                            }
                    } else {
                        errorTextView.text = "Registration failed: ${task.exception?.localizedMessage}"
                        errorTextView.visibility = TextView.VISIBLE
                    }
                }
        }
    }
}
