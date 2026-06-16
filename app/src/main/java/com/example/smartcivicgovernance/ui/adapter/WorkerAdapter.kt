package com.example.smartcivicgovernance.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartcivicgovernance.data.model.Worker
import com.example.smartcivicgovernance.databinding.ItemWorkerBinding

class WorkerAdapter(private val onItemClick: (Worker) -> Unit) :
    ListAdapter<Worker, WorkerAdapter.WorkerViewHolder>(WorkerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerViewHolder {
        val binding = ItemWorkerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WorkerViewHolder(private val binding: ItemWorkerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(worker: Worker) {
            binding.tvWorkerRank.text = worker.rank.toString()
            binding.tvLeaderboardWorkerName.text = worker.name
            binding.tvWorkerSubstats.text = "Rating: ${String.format("%.1f", worker.averageRating)} | Solved: ${worker.issuesSolved}"
            binding.tvWorkerPoints.text = "${worker.totalPoints} pts"

            binding.root.setOnClickListener {
                onItemClick(worker)
            }
        }
    }

    class WorkerDiffCallback : DiffUtil.ItemCallback<Worker>() {
        override fun areItemsTheSame(oldItem: Worker, newItem: Worker): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: Worker, newItem: Worker): Boolean {
            return oldItem == newItem
        }
    }
}
