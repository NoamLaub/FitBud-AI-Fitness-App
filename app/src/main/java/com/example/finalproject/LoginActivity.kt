package com.example.finalproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.d("BOOT", "LoginActivity onCreate")
        android.widget.Toast.makeText(this, "LoginActivity started", android.widget.Toast.LENGTH_SHORT).show()
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val loginErrorTextView = findViewById<TextView>(R.id.loginErrorTextView)
        val expertRegisterButton = findViewById<Button?>(R.id.expertRegisterButton)
        expertRegisterButton?.setOnClickListener {
            startActivity(Intent(this, ExpertRegisterActivity::class.java))
        }

        fun goToProfileSetup() {
            startActivity(Intent(this, ProfileSetupActivity::class.java))
            finish()
        }

        // Login logic
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            loginErrorTextView.visibility = TextView.GONE
            if (email.isEmpty() || password.isEmpty()) {
                loginErrorTextView.text = "Please enter email and password."
                loginErrorTextView.visibility = TextView.VISIBLE
                return@setOnClickListener
            }
            auth.signInWithEmailAndPassword(email, password)
    .addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                // Check if user is expert
                val expertRef = FirebaseDatabase.getInstance().getReference("experts").child(userId)
                expertRef.get().addOnSuccessListener { expertSnap ->
                    if (expertSnap.exists()) {
                        // Expert user: go to expert dashboard
                        startActivity(Intent(this, ExpertDashboardActivity::class.java))
                        finish()
                    } else {
                        // Regular user: check profile
                        val dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
                        dbRef.get().addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            } else {
                                goToProfileSetup()
                            }
                        }.addOnFailureListener {
                            goToProfileSetup()
                        }
                    }
                }.addOnFailureListener {
                    // Fallback to regular user logic
                    val dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
                    dbRef.get().addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            goToProfileSetup()
                        }
                    }.addOnFailureListener {
                        goToProfileSetup()
                    }
                }
            } else {
                goToProfileSetup()
            }
        } else {
            loginErrorTextView.text = "Login failed: ${task.exception?.localizedMessage}"
            loginErrorTextView.visibility = TextView.VISIBLE
        }
    }
        }

        // Registration logic
        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            loginErrorTextView.visibility = TextView.GONE
            if (email.isEmpty() || password.isEmpty()) {
                loginErrorTextView.text = "Please enter email and password."
                loginErrorTextView.visibility = TextView.VISIBLE
                return@setOnClickListener
            }
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        goToProfileSetup()
                    } else {
                        loginErrorTextView.text = "Registration failed: ${task.exception?.localizedMessage}"
                        loginErrorTextView.visibility = TextView.VISIBLE
                    }
                }
        }
    }
}
