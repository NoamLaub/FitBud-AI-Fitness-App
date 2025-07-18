package com.example.finalproject

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.example.finalproject.model.User
import com.google.firebase.auth.FirebaseAuth

class ExpertUserChatActivity : AppCompatActivity() {
    private lateinit var chatListView: ListView
    private lateinit var editResponseButton: Button
    private lateinit var messageEditText: EditText
    private lateinit var saveEditButton: Button
    private lateinit var userId: String
    private lateinit var userName: String
    private lateinit var database: DatabaseReference
    private val chatHistory = mutableListOf<Pair<String, String>>() // (role, content)
    private var reportedPositions = mutableSetOf<Int>()
    private lateinit var adapter: ChatMessageAdapter
    private var editingPosition: Int = -1
    // Map of edited message key to its original content
    private val editedMessageKeys = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatListView = findViewById(R.id.chatListView)
        messageEditText = findViewById(R.id.messageEditText)
        messageEditText.isEnabled = true
        messageEditText.isFocusable = true
        messageEditText.isFocusableInTouchMode = true
        messageEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        editResponseButton = findViewById(R.id.sendButton)

        userId = intent.getStringExtra("userId") ?: ""
        userName = intent.getStringExtra("userName") ?: "User"
        database = FirebaseDatabase.getInstance().getReference()
        adapter = ChatMessageAdapter(this, chatHistory, null, reportedPositions)
        chatListView.adapter = adapter

        loadChatHistory()

        chatListView.setOnItemClickListener { _, _, position, _ ->
            val (role, content) = chatHistory[position]
            if (role == "assistant") {
                database.child("chat_history").child(userId).get().addOnSuccessListener { snapshot ->
                    val key = snapshot.children.firstOrNull {
                        it.child("role").getValue(String::class.java) == "assistant" &&
                        it.child("content").getValue(String::class.java) == content
                    }?.key
                    if (key != null) {
                        val reported = snapshot.child(key).child("reported").getValue(Boolean::class.java) ?: false
                        if (reported) {
                            // Show edit dialog
                            val editText = EditText(this)
                            editText.setText(content)
                            editText.setSelection(editText.text.length)
                            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                            editText.maxLines = 6
                            editText.minLines = 3
                            editText.gravity = android.view.Gravity.TOP or android.view.Gravity.START
                            editText.setPadding(32, 32, 32, 32)
                            editText.setBackgroundColor(android.graphics.Color.parseColor("#E9F5E1"))
                            editText.isEnabled = true
                            editText.isFocusable = true
                            editText.isFocusableInTouchMode = true
                            val dialog = android.app.AlertDialog.Builder(this)
                                .setTitle("Edit Reported Message")
                                .setView(editText)
                                .setPositiveButton("Save") { dialog, _ ->
                                    val newContent = editText.text.toString()
                                    if (newContent.isNotBlank()) {
                                        // Update in Firebase
                                        database.child("chat_history").child(userId).child(key).child("content").setValue(newContent)
                                        // Update in chatHistory
                                        chatHistory[position] = "assistant" to newContent
                                        adapter.notifyDataSetChanged()
                                        Toast.makeText(this, "Message updated.", Toast.LENGTH_SHORT).show()
                                        // --- NEW: Immediately update the /reports node for this message ---
                                        val reportsRef = database.child("reports")
                                        val expertId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                        if (expertId != null) {
                                            reportsRef.orderByChild("userId").equalTo(userId)
                                                .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                                                    override fun onDataChange(reportsSnap: com.google.firebase.database.DataSnapshot) {
                                                        for (report in reportsSnap.children) {
                                                            val reportExpertId = report.child("expertId").getValue(String::class.java)
                                                            val reportContent = report.child("chatbotMessage").getValue(String::class.java)
                                                            val reportMsgKey = report.child("messageKey").getValue(String::class.java)
                                                            if (reportExpertId == expertId && reportMsgKey == key) {
                                                                // Mark this report as resolved (force overwrite)
                                                                report.ref.child("resolved").setValue(true).addOnCompleteListener { task ->
                                                                    if (task.isSuccessful) {
                                                                        android.util.Log.d("ExpertUserChatActivity", "Marked report as resolved for expertId=$expertId, userId=$userId, content='$content'")
                                                                        // Double-check after update: if report still exists and not resolved, delete as fallback
                                                                        report.ref.child("resolved").get().addOnSuccessListener { resolvedSnap ->
                                                                            val resolved = resolvedSnap.getValue(Boolean::class.java) ?: false
                                                                            if (!resolved) {
                                                                                android.util.Log.w("ExpertUserChatActivity", "Report still not resolved after update, deleting as fallback.")
                                                                                report.ref.removeValue()
                                                                            }
                                                                        }
                                                                    } else {
                                                                        android.util.Log.e("ExpertUserChatActivity", "Failed to mark report as resolved, deleting as fallback.")
                                                                        report.ref.removeValue()
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                                                        android.util.Log.e("ExpertUserChatActivity", "Failed to query reports: ${error.message}")
                                                    }
                                                })
                                        }
                                        // --- END NEW ---
                                    }
                                    dialog.dismiss()
                                }
                                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                                .create()
                            dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                            dialog.show()
                            // Ensure EditText is focused and keyboard opens
                            editText.requestFocus()
                            editText.post {
                                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                                imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                            }
                        } else {
                            Toast.makeText(this, "Only reported messages can be edited.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Only bot responses can be edited.", Toast.LENGTH_SHORT).show()
            }
        }

        loadChatHistory()
    }

    private fun loadChatHistory() {
        database.child("chat_history").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatHistory.clear()
                reportedPositions.clear()
                var idx = 0
                snapshot.children.forEach {
                    val role = it.child("role").getValue(String::class.java) ?: ""
                    val content = it.child("content").getValue(String::class.java) ?: ""
                    chatHistory.add(role to content)
                    val reported = it.child("reported").getValue(Boolean::class.java) ?: false
                    if (role == "assistant" && reported) {
                        reportedPositions.add(idx)
                    }
                    idx++
                }
                // Recreate adapter to update highlights
                adapter = ChatMessageAdapter(this@ExpertUserChatActivity, chatHistory, null, reportedPositions)
                chatListView.adapter = adapter
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun saveChatHistory() {
        val chatRef = database.child("chat_history").child(userId)
        chatRef.removeValue()
        // To preserve 'reported' state, fetch current messages and their keys
        database.child("chat_history").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatHistory.forEach { (role, content) ->
                    // Find the original message to check if it was reported
                    val original = snapshot.children.firstOrNull {
                        it.child("role").getValue(String::class.java) == role &&
                        it.child("content").getValue(String::class.java) == content
                    }
                    val reported = original?.child("reported")?.getValue(Boolean::class.java) ?: false
                    val messageMap = if (reported) {
                        mapOf("role" to role, "content" to content, "reported" to true)
                    } else {
                        mapOf("role" to role, "content" to content)
                    }
                    chatRef.push().setValue(messageMap)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onPause() {
    super.onPause()
    // Clear the 'reported' flag for all edited messages
    if (editedMessageKeys.isNotEmpty()) {
        val chatRef = database.child("chat_history").child(userId)
        val reportsRef = database.child("reports")
        var anyReportCleared = false
        for ((key, originalContent) in editedMessageKeys) {
            // 1. Clear 'reported' flag in chat_history
            chatRef.child(key).child("reported").setValue(false)
            android.util.Log.d("ExpertUserChatActivity", "Cleared 'reported' flag for key=$key, userId=$userId")
            android.widget.Toast.makeText(this, "Cleared 'reported' for key=$key", android.widget.Toast.LENGTH_SHORT).show()
            // 2. Mark matching report entry in /reports as resolved using original content
            val expertId = FirebaseAuth.getInstance().currentUser?.uid ?: continue
            reportsRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(reportsSnap: DataSnapshot) {
                        var found = false
                        for (report in reportsSnap.children) {
                            val reportExpertId = report.child("expertId").getValue(String::class.java)
                            val reportContent = report.child("chatbotMessage").getValue(String::class.java)
                            if (reportExpertId == expertId && reportContent == originalContent) {
    // Mark this report as resolved (force overwrite)
    report.ref.child("resolved").setValue(true).addOnCompleteListener { task ->
        if (task.isSuccessful) {
            android.util.Log.d("ExpertUserChatActivity", "Marked report as resolved for expertId=$expertId, userId=$userId, content='$originalContent'")
            // Double-check after update: if report still exists and not resolved, delete as fallback
            report.ref.child("resolved").get().addOnSuccessListener { resolvedSnap ->
                val resolved = resolvedSnap.getValue(Boolean::class.java) ?: false
                if (!resolved) {
                    android.util.Log.w("ExpertUserChatActivity", "Report still not resolved after update, deleting as fallback.")
                    report.ref.removeValue()
                }
            }
            android.widget.Toast.makeText(this@ExpertUserChatActivity, "Marked report as resolved for content: $originalContent", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.util.Log.e("ExpertUserChatActivity", "Failed to mark report as resolved, deleting as fallback.")
            report.ref.removeValue()
        }
    }
    found = true
    anyReportCleared = true
}
                        }
                        if (!found) {
                            android.util.Log.w("ExpertUserChatActivity", "No matching report found for expertId=$expertId, userId=$userId, content='$originalContent'")
                            android.widget.Toast.makeText(this@ExpertUserChatActivity, "No matching report found for: $originalContent", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        // If any report was cleared, trigger dashboard refresh
                        if (anyReportCleared) {
                            val expertRef = database.child("experts").child(expertId).child("refresh")
                            expertRef.runTransaction(object : Transaction.Handler {
                                override fun doTransaction(currentData: MutableData): Transaction.Result {
                                    val current = (currentData.getValue(Int::class.java) ?: 0) + 1
                                    currentData.value = current
                                    return Transaction.success(currentData)
                                }
                                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
                            })
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        android.util.Log.e("ExpertUserChatActivity", "Failed to query reports: ${error.message}")
                        
                    }
                })
        }
        editedMessageKeys.clear()
    }
}
}
