package com.example.smartcivicgovernance.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartcivicgovernance.R
import com.example.smartcivicgovernance.data.model.Complaint
import com.example.smartcivicgovernance.databinding.ItemComplaintBinding

class ComplaintAdapter(private val onItemClick: (Complaint) -> Unit) :
    ListAdapter<Complaint, ComplaintAdapter.ComplaintViewHolder>(ComplaintDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComplaintViewHolder {
        val binding = ItemComplaintBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ComplaintViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ComplaintViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ComplaintViewHolder(private val binding: ItemComplaintBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(complaint: Complaint) {
            binding.tvComplaintTitle.text = complaint.title
            binding.tvComplaintAddress.text = complaint.address
            binding.tvPriorityText.text = "Priority: ${complaint.priority}"

            // Formatting date (very basic, can be expanded to proper formatter)
            binding.tvComplaintDate.text = complaint.createdAt?.toDate()?.toString() ?: ""

            // Status chip colors and text
            binding.tvStatusChip.text = complaint.status
            val context = binding.root.context
            
            val (bgColor, txtColor) = when (complaint.status) {
                "Pending" -> Pair(R.color.status_pending_bg, R.color.status_pending)
                "In Progress" -> Pair(R.color.status_in_progress_bg, R.color.status_in_progress)
                "Resolved" -> Pair(R.color.status_resolved_bg, R.color.status_resolved)
                "Verification Pending" -> Pair(R.color.status_verification_pending_bg, R.color.status_verification_pending)
                else -> Pair(R.color.status_rejected_bg, R.color.status_rejected)
            }
            
            binding.tvStatusChip.setBackgroundColor(ContextCompat.getColor(context, bgColor))
            binding.tvStatusChip.setTextColor(ContextCompat.getColor(context, txtColor))

            // Category icons
            val iconRes = when (complaint.category) {
                "Pothole" -> R.drawable.ic_pothole
                "Garbage" -> R.drawable.ic_garbage
                "Water Leakage" -> R.drawable.ic_water
                "Drainage" -> R.drawable.ic_drainage
                "Streetlight" -> R.drawable.ic_streetlight
                else -> R.drawable.ic_traffic
            }
            binding.ivCategoryIcon.setImageResource(iconRes)

            binding.root.setOnClickListener {
                onItemClick(complaint)
            }
        }
    }

    class ComplaintDiffCallback : DiffUtil.ItemCallback<Complaint>() {
        override fun areItemsTheSame(oldItem: Complaint, newItem: Complaint): Boolean {
            return oldItem.complaintId == newItem.complaintId
        }

        override fun areContentsTheSame(oldItem: Complaint, newItem: Complaint): Boolean {
            return oldItem == newItem
        }
    }
}
