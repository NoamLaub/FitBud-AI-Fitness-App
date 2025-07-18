package com.example.finalproject

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class ChatMessageAdapter(
    private val context: Context,
    private val messages: List<Pair<String, String>>, // (role, content)
    private val onReportClick: ((position: Int, message: Pair<String, String>) -> Unit)? = null,
    private val reportedPositions: Set<Int> = emptySet() // positions of reported messages (for expert UI)
) : BaseAdapter() {
    override fun getViewTypeCount(): Int = 2
    override fun getItemViewType(position: Int): Int {
        val (role, _) = messages[position]
        return if (role == "assistant") 1 else 0
    }
    override fun getCount(): Int = messages.size
    override fun getItem(position: Int): Any = messages[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Sound effect logic: play sound when new message appears
        val (role, content) = messages[position]
        val layoutId = if (role == "assistant") R.layout.item_message_bot else R.layout.item_message_user
        val isNewMessage = convertView == null
        val view = if (isNewMessage || ((convertView?.getTag(R.id.message_type_tag) as? Int) != getItemViewType(position))) {
            val v = LayoutInflater.from(context).inflate(layoutId, parent, false)
            v.setTag(R.id.message_type_tag, getItemViewType(position))
            v
        } else {
            convertView!!
        }
        val messageText = view?.findViewById<TextView>(R.id.text_message_body)
        messageText?.text = content
        val reportButton = view?.findViewById<View?>(R.id.button_report)

        // Highlight reported messages for experts
        if (reportedPositions.contains(position)) {
            // Example: yellow background for reported
            view?.setBackgroundResource(R.drawable.bg_reported_message)
        } else {
            view?.setBackgroundResource(0) // default background
        }

        if (role == "assistant" && reportButton != null) {
            // Show report button only if not reported (for user UI)
            if (!reportedPositions.contains(position)) {
                reportButton.visibility = View.VISIBLE
                reportButton.setOnClickListener {
                    onReportClick?.invoke(position, messages[position])
                }
            } else {
                reportButton.visibility = View.GONE
            }
        } else if (reportButton != null) {
            reportButton.visibility = View.GONE
        }

        // Play sound effect for new messages
        if (isNewMessage && view != null) {
            val soundResId = if (role == "assistant") R.raw.receive else R.raw.send
            try {
                val mediaPlayer = android.media.MediaPlayer.create(context, soundResId)
                mediaPlayer?.setOnCompletionListener { mp ->
                    mp.release()
                }
                mediaPlayer?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return view!!
    }
}
