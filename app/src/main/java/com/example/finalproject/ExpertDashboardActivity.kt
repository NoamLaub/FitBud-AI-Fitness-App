package com.example.finalproject

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.finalproject.model.User

class ExpertDashboardActivity : AppCompatActivity() {
    private lateinit var assignedUsersListView: ListView
    private lateinit var noUsersTextView: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var expertId: String
    private lateinit var database: DatabaseReference
    private val assignedUsers = mutableListOf<User>()
    private lateinit var adapter: DashboardUserAdapter
    private val userReportCounts = mutableMapOf<String, Int>() // userId -> count

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expert_dashboard)

        assignedUsersListView = findViewById(R.id.assignedUsersListView)
        noUsersTextView = findViewById(R.id.noUsersTextView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        expertId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        database = FirebaseDatabase.getInstance().getReference()
        adapter = DashboardUserAdapter(this, assignedUsers, highlightUserIds = userReportCounts.filter { it.value > 0 }.keys)
        assignedUsersListView.adapter = adapter

        loadAssignedUsers()
        listenForReports()
        listenForDashboardRefresh()

        assignedUsersListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val user = assignedUsers[position]
            val intent = Intent(this, ExpertUserChatActivity::class.java)
            intent.putExtra("userId", user.userId)
            intent.putExtra("userName", "${user.name} ${user.surname}")
            startActivity(intent)
        }
    }

    private fun loadAssignedUsers() {
        loadingProgressBar.visibility = View.VISIBLE
        assignedUsersListView.visibility = View.GONE
        noUsersTextView.visibility = View.GONE
        // Find expert entry and get assigned user IDs
        database.child("experts").child(expertId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val assignedUserIdsAny = snapshot.child("assignedUserIds").value
                android.util.Log.d("ExpertDashboard", "assignedUserIdsAny=$assignedUserIdsAny")

val userIds: List<String> = when (assignedUserIdsAny) {
                    is List<*> -> assignedUserIdsAny.filterIsInstance<String>()
                    is String -> if (assignedUserIdsAny.isBlank()) emptyList() else listOf(assignedUserIdsAny)
                    null -> emptyList()
                    else -> emptyList()
                }
                android.util.Log.d("ExpertDashboard", "userIds=$userIds")

if (userIds.isEmpty()) {
                    loadingProgressBar.visibility = View.GONE
                    noUsersTextView.visibility = View.VISIBLE
                    assignedUsersListView.visibility = View.GONE
                } else {
                    loadUserDetails(userIds)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                loadingProgressBar.visibility = View.GONE
                noUsersTextView.text = "Failed to load users."
                noUsersTextView.visibility = View.VISIBLE
                assignedUsersListView.visibility = View.GONE
            }
        })
    }

    private fun loadUserDetails(userIds: List<String>) {
        assignedUsers.clear()
        var loadedCount = 0
        val totalToLoad = userIds.size
        android.util.Log.d("ExpertDashboard", "Starting to load user details for userIds=$userIds")
        if (totalToLoad == 0) {
            loadingProgressBar.visibility = View.GONE
            noUsersTextView.visibility = View.GONE
            assignedUsersListView.visibility = View.VISIBLE
            adapter = DashboardUserAdapter(this@ExpertDashboardActivity, assignedUsers, highlightUserIds = userReportCounts.filter { it.value > 0 }.keys)
            assignedUsersListView.adapter = adapter
        }
        for (userId in userIds) {
            database.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val user = snapshot.getValue(User::class.java)
                        if (user != null) {
                            // Prevent duplicate users by userId
                            if (assignedUsers.none { it.userId == user.userId }) {
                                assignedUsers.add(user)
                            }
                            val displayName = listOfNotNull(user.name, user.surname)
                                .filter { !it.isNullOrBlank() }
                                .joinToString(" ")
                                .ifBlank { user.userId ?: "Unknown" }
                            
                            android.util.Log.d("ExpertDashboard", "Loaded user: $displayName (${user.userId})")
                        } else {
                            // Fallback: display userId if user object is null
                            val fallbackId = snapshot.key ?: "Unknown"
                            
                            android.util.Log.w("ExpertDashboard", "User data missing for userId=$fallbackId, showing fallback")
                        }
                    } catch (e: Exception) {
                        val fallbackId = snapshot.key ?: "Unknown"
                        
                        android.util.Log.e("ExpertDashboard", "Failed to load user for userId=$fallbackId: ${e.message}")
                    }
                    loadedCount++
                    if (loadedCount == totalToLoad) {
                        loadingProgressBar.visibility = View.GONE
                        if (assignedUsers.isEmpty()) {
                            noUsersTextView.visibility = View.VISIBLE
                            assignedUsersListView.visibility = View.GONE
                        } else {
                            noUsersTextView.visibility = View.GONE
                            assignedUsersListView.visibility = View.VISIBLE
                            adapter.highlightUserIds = userReportCounts.filter { it.value > 0 }.keys
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
    loadedCount++
    if (loadedCount == totalToLoad) {
        loadingProgressBar.visibility = View.GONE
        if (assignedUsers.isEmpty()) {
            noUsersTextView.visibility = View.VISIBLE
            assignedUsersListView.visibility = View.GONE
        } else {
            noUsersTextView.visibility = View.GONE
            assignedUsersListView.visibility = View.VISIBLE
            adapter.notifyDataSetChanged()
        }
    }
}
            })
        }
    }

    private fun refreshUserReportCountsAndReload() {
        val reportsRef = database.child("reports")
        reportsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userReportCounts.clear()
                for (reportSnap in snapshot.children) {
                    val expertIdVal = reportSnap.child("expertId").getValue(String::class.java)
                    val userIdVal = reportSnap.child("userId").getValue(String::class.java)
                    val resolved = reportSnap.child("resolved").getValue(Boolean::class.java) ?: false
                    if (expertIdVal == expertId && userIdVal != null && !resolved) {
                        userReportCounts[userIdVal] = (userReportCounts[userIdVal] ?: 0) + 1
                    }
                }
                loadAssignedUsers()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenForReports() {
        // Listen for new reports for this expert
        val reportsRef = database.child("reports")
        reportsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userReportCounts.clear()
                for (reportSnap in snapshot.children) {
                    val expertIdVal = reportSnap.child("expertId").getValue(String::class.java)
                    val userIdVal = reportSnap.child("userId").getValue(String::class.java)
                    val resolved = reportSnap.child("resolved").getValue(Boolean::class.java) ?: false
                    if (expertIdVal == expertId && userIdVal != null && !resolved) {
                        userReportCounts[userIdVal] = (userReportCounts[userIdVal] ?: 0) + 1
                    }
                }
                // Always refresh the assigned users and badge counts
                loadAssignedUsers()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenForDashboardRefresh() {
        val refreshRef = database.child("experts").child(expertId).child("refresh")
        refreshRef.addValueEventListener(object : ValueEventListener {
            private var lastValue: Int? = null
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue(Int::class.java)
                if (lastValue != null && value != null && value != lastValue) {
                    // Refresh dashboard when 'refresh' value changes
                    refreshUserReportCountsAndReload()
                }
                lastValue = value
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onResume() {
        super.onResume()
        // Fallback: force rebalance of user assignments every time dashboard is resumed
        ExpertAssignmentUtil.rebalanceAssignments(com.google.firebase.database.FirebaseDatabase.getInstance()) {
            android.widget.Toast.makeText(this, "Checked user assignments.", android.widget.Toast.LENGTH_SHORT).show()
            loadAssignedUsers()
        }
    }
}
