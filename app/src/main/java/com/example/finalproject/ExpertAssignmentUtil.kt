package com.example.finalproject

import com.google.firebase.database.*
import com.example.finalproject.model.Expert
import com.example.finalproject.model.User

object ExpertAssignmentUtil {
    /**
     * Reassigns all users equally among all experts in the database.
     * This should be called after a new expert registers or when users change.
     */
    fun rebalanceAssignments(database: FirebaseDatabase, onComplete: (() -> Unit)? = null) {
    android.util.Log.d("ExpertAssignmentUtil", "rebalanceAssignments called")
        // Sanitize all expert assignedUserIds before rebalance
        sanitizeAllExpertAssignments(database) {
        android.util.Log.d("ExpertAssignmentUtil", "sanitizeAllExpertAssignments complete, proceeding to doRebalanceAssignments")
            // Proceed with rebalance after sanitization
            doRebalanceAssignments(database, onComplete)
        }
    }

    // Utility to sanitize all expert assignedUserIds in Firebase
    fun sanitizeAllExpertAssignments(database: FirebaseDatabase, onComplete: (() -> Unit)? = null) {
        android.util.Log.d("ExpertAssignmentUtil", "sanitizeAllExpertAssignments starting")
        val expertsRef = database.getReference("experts")
        android.util.Log.d("ExpertAssignmentUtil", "Getting experts reference: ${expertsRef.path}")
        expertsRef.get().addOnSuccessListener { expertsSnap ->
            android.util.Log.d("ExpertAssignmentUtil", "Experts query successful, children count: ${expertsSnap.childrenCount}")
            val updates = hashMapOf<String, Any?>()
            for (expertSnap in expertsSnap.children) {
                val expertId = expertSnap.key ?: continue
                val assignedUserIdsAny = expertSnap.child("assignedUserIds").value
                val assignedUserIds = when (assignedUserIdsAny) {
                    is List<*> -> assignedUserIdsAny.filterIsInstance<String>()
                    is String -> if (assignedUserIdsAny.isBlank()) emptyList() else listOf(assignedUserIdsAny)
                    null -> emptyList()
                    else -> emptyList()
                }
                updates["experts/$expertId/assignedUserIds"] = assignedUserIds
            }
            android.util.Log.d("ExpertAssignmentUtil", "Sanitization updates prepared: ${updates.size} updates")
            if (updates.isNotEmpty()) {
                database.reference.updateChildren(updates).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        android.util.Log.d("ExpertAssignmentUtil", "Sanitization updates completed successfully")
                    } else {
                        android.util.Log.e("ExpertAssignmentUtil", "Sanitization updates failed: ${task.exception?.message}")
                    }
                    onComplete?.invoke()
                }
            } else {
                android.util.Log.d("ExpertAssignmentUtil", "No sanitization updates needed")
                onComplete?.invoke()
            }
        }.addOnFailureListener { exception ->
            android.util.Log.e("ExpertAssignmentUtil", "Failed to get experts for sanitization: ${exception.message}")
            android.util.Log.e("ExpertAssignmentUtil", "Exception details: $exception")
            // Still call onComplete to prevent hanging
            onComplete?.invoke()
        }
    }

    private fun doRebalanceAssignments(database: FirebaseDatabase, onComplete: (() -> Unit)? = null) {
        android.util.Log.d("ExpertAssignmentUtil", "doRebalanceAssignments called")
        val expertsRef = database.getReference("experts")
        val usersRef = database.getReference("users")
        
        expertsRef.get().addOnSuccessListener { expertsSnap ->
            android.util.Log.d("ExpertAssignmentUtil", "Experts snapshot received: ${expertsSnap.childrenCount} experts")
            
            usersRef.get().addOnSuccessListener { usersSnap ->
                android.util.Log.d("ExpertAssignmentUtil", "Users snapshot received: ${usersSnap.childrenCount} users")
                
                val expertIds = expertsSnap.children.mapNotNull { it.key }
                val userIds = usersSnap.children.mapNotNull { it.key }
                
                android.util.Log.d("ExpertAssignmentUtil", "Expert IDs: $expertIds")
                android.util.Log.d("ExpertAssignmentUtil", "User IDs: $userIds")
                
                if (expertIds.isEmpty()) {
                    android.util.Log.d("ExpertAssignmentUtil", "No experts found; skipping assignment.")
                    onComplete?.invoke()
                    return@addOnSuccessListener
                }
                
                if (userIds.isEmpty()) {
                    android.util.Log.d("ExpertAssignmentUtil", "No users found; clearing expert assignments.")
                    // Clear all expert assignments if no users exist
                    val updates = hashMapOf<String, Any?>()
                    expertIds.forEach { expertId ->
                        updates["experts/$expertId/assignedUserIds"] = emptyList<String>()
                    }
                    database.reference.updateChildren(updates).addOnCompleteListener {
                        android.util.Log.d("ExpertAssignmentUtil", "Expert assignments cleared")
                        onComplete?.invoke()
                    }
                    return@addOnSuccessListener
                }
                
                // Distribute users equally among experts
                val assignments = mutableMapOf<String, MutableList<String>>()
                expertIds.forEach { assignments[it] = mutableListOf() }
                
                if (expertIds.size == 1) {
                    // If only one expert, assign all users to them
                    assignments[expertIds[0]] = userIds.toMutableList()
                    android.util.Log.d("ExpertAssignmentUtil", "Single expert mode: assigning all ${userIds.size} users to ${expertIds[0]}")
                } else {
                    // Distribute users evenly among multiple experts
                    userIds.forEachIndexed { idx, userId ->
                        val expertIdx = idx % expertIds.size
                        val expertId = expertIds[expertIdx]
                        assignments[expertId]?.add(userId)
                        android.util.Log.d("ExpertAssignmentUtil", "Assigning user $userId to expert $expertId (index $expertIdx)")
                    }
                }
                
                // Log final assignments
                assignments.forEach { (expertId, userList) ->
                    android.util.Log.d("ExpertAssignmentUtil", "Expert $expertId will have ${userList.size} users: $userList")
                }
                
                // Prepare updates for Firebase
                val updates = hashMapOf<String, Any?>()
                
                // Update expert assignments
                expertIds.forEach { expertId ->
                    val assignedUserIds = assignments[expertId] ?: mutableListOf<String>()
                    updates["experts/$expertId/assignedUserIds"] = assignedUserIds.toList()
                    android.util.Log.d("ExpertAssignmentUtil", "Setting expert $expertId assignedUserIds to: ${assignedUserIds.toList()}")
                }
                
                // Update user assignments
                userIds.forEach { userId ->
                    val assignedExpert = assignments.entries.find { it.value.contains(userId) }?.key
                    if (assignedExpert != null) {
                        updates["users/$userId/assignedExpertId"] = assignedExpert
                        android.util.Log.d("ExpertAssignmentUtil", "Setting user $userId assignedExpertId to: $assignedExpert")
                    } else {
                        android.util.Log.w("ExpertAssignmentUtil", "User $userId was not assigned to any expert!")
                    }
                }
                
                android.util.Log.d("ExpertAssignmentUtil", "Applying ${updates.size} updates to Firebase")
                
                // Apply all updates to Firebase
                database.reference.updateChildren(updates)
                    .addOnSuccessListener {
                        android.util.Log.d("ExpertAssignmentUtil", "Expert assignment updates completed successfully")
                        onComplete?.invoke()
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("ExpertAssignmentUtil", "Failed to update expert assignments: ${e.message}", e)
                        onComplete?.invoke()
                    }
            }.addOnFailureListener { e ->
                android.util.Log.e("ExpertAssignmentUtil", "Failed to get users: ${e.message}", e)
                onComplete?.invoke()
            }
        }.addOnFailureListener { e ->
            android.util.Log.e("ExpertAssignmentUtil", "Failed to get experts: ${e.message}", e)
            onComplete?.invoke()
        }
    }
}
