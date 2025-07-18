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
        val expertsRef = database.getReference("experts")
        expertsRef.get().addOnSuccessListener { expertsSnap ->
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
            if (updates.isNotEmpty()) {
                database.reference.updateChildren(updates).addOnCompleteListener {
                    onComplete?.invoke()
                }
            } else {
                onComplete?.invoke()
            }
        }
    }

    private fun doRebalanceAssignments(database: FirebaseDatabase, onComplete: (() -> Unit)? = null) {
    android.util.Log.d("ExpertAssignmentUtil", "doRebalanceAssignments called")
        val expertsRef = database.getReference("experts")
        val usersRef = database.getReference("users")
        expertsRef.get().addOnSuccessListener { expertsSnap ->
            usersRef.get().addOnSuccessListener { usersSnap ->
                val expertIds = expertsSnap.children.mapNotNull { it.key }
                val userIds = usersSnap.children.mapNotNull { it.key }
                android.util.Log.d("ExpertAssignmentUtil", "expertIds=$expertIds, userIds=$userIds")
                if (expertIds.isEmpty() || userIds.isEmpty()) {
                    android.util.Log.d("ExpertAssignmentUtil", "No experts or users; skipping assignment.")
                    onComplete?.invoke()
                    return@addOnSuccessListener
                }
                // Distribute users equally
                val assignments = mutableMapOf<String, MutableList<String>>()
                expertIds.forEach { assignments[it] = mutableListOf() }
                if (expertIds.size == 1) {
                    // If only one expert, assign all users to them
                    assignments[expertIds[0]] = userIds.toMutableList()
                } else {
                    userIds.forEachIndexed { idx, userId ->
                        val expertIdx = idx % expertIds.size
                        assignments[expertIds[expertIdx]]?.add(userId)
                    }
                }
                // Update users' assignedExpertId and experts' assignedUserIds
                val updates = hashMapOf<String, Any?>()
                // Ensure every expert gets assignedUserIds, even if empty and always a List<String>
                expertIds.forEach { expertId ->
                    val assignedUserIds = assignments[expertId] ?: mutableListOf<String>()
                    updates["experts/$expertId/assignedUserIds"] = assignedUserIds.toList()
                    assignedUserIds.forEach { userId ->
                        updates["users/$userId/assignedExpertId"] = expertId
                    }
                }
                database.reference.updateChildren(updates).addOnCompleteListener {
                    onComplete?.invoke()
                }
            }
        }
    }
}
