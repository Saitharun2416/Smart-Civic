package com.example.smartcivicgovernance.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartcivicgovernance.R
import com.example.smartcivicgovernance.data.model.Notification
import com.example.smartcivicgovernance.databinding.ItemNotificationBinding

class NotificationAdapter(private val onItemClick: (Notification) -> Unit) :
    ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notif: Notification) {
            binding.tvNotifTitle.text = notif.title
            binding.tvNotifBody.text = notif.body
            binding.tvNotifDate.text = notif.createdAt?.toDate()?.toString() ?: ""

            // Status category icon
            val iconRes = when (notif.type) {
                "complaint_accepted" -> R.drawable.ic_tasks
                "complaint_resolved" -> R.drawable.ic_add
                "points_earned" -> R.drawable.ic_leaderboard
                else -> R.drawable.ic_notifications
            }
            binding.ivNotifIcon.setImageResource(iconRes)

            binding.root.setOnClickListener {
                onItemClick(notif)
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem.notifId == newItem.notifId
        }

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem == newItem
        }
    }
}
