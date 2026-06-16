package com.example.smartcivicgovernance.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartcivicgovernance.R
import com.example.smartcivicgovernance.data.model.User
import com.example.smartcivicgovernance.databinding.ItemUserBinding

class UserAdapter(private val onToggleStatus: (User, Boolean) -> Unit) :
    ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(private val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            val context = binding.root.context
            binding.tvUserName.text = user.name
            binding.tvUserEmail.text = user.email
            binding.tvUserRoleText.text = "Role: ${user.role.uppercase()}"

            val statusText = if (user.disabled) {
                if (user.role == "worker") "Status: PENDING APPROVAL" else "Status: DISABLED"
            } else {
                "Status: APPROVED"
            }
            binding.tvUserStatusText.text = statusText
            binding.tvUserStatusText.setTextColor(
                if (user.disabled) {
                    androidx.core.content.ContextCompat.getColor(context, R.color.status_pending)
                } else {
                    androidx.core.content.ContextCompat.getColor(context, R.color.status_resolved)
                }
            )

            // Disabled state check
            binding.switchUserStatus.setOnCheckedChangeListener(null) // Clear listener first to avoid trigger on bind
            binding.switchUserStatus.isChecked = !user.disabled

            binding.switchUserStatus.setOnCheckedChangeListener { _, isChecked ->
                onToggleStatus(user, isChecked)
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
