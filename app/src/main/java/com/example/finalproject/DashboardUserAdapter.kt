package com.example.finalproject

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.finalproject.model.User

class DashboardUserAdapter(
    context: Context,
    private val users: List<User>,
    var highlightUserIds: Set<String>
) : ArrayAdapter<User>(context, 0, users) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val user = users[position]
        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.item_dashboard_user, parent, false
        )
        val nameTextView = view.findViewById<TextView>(R.id.userNameTextView)
        val displayName = listOfNotNull(user.name, user.surname)
            .filter { !it.isNullOrBlank() }
            .joinToString(" ")
            .ifBlank { user.userId ?: "Unknown" }
        nameTextView.text = displayName
        if (highlightUserIds.contains(user.userId)) {
            nameTextView.setBackgroundColor(Color.parseColor("#FFF59D")) // Light yellow highlight
        } else {
            nameTextView.setBackgroundColor(Color.TRANSPARENT)
        }
        return view
    }
}
