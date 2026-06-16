package com.example.smartcivicgovernance.ui.worker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.smartcivicgovernance.databinding.FragmentWorkerStatsBinding

class WorkerStatsFragment : Fragment() {

    private var _binding: FragmentWorkerStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWorkerStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadWorkerStats()
    }

    private fun setupObservers() {
        viewModel.workerStats.observe(viewLifecycleOwner) { stats ->
            if (stats != null) {
                binding.tvStatPoints.text = stats.totalPoints.toString()
                binding.tvStatSolved.text = stats.issuesSolved.toString()
                binding.tvStatActive.text = stats.activeTasks.toString()
                binding.tvStatRating.text = String.format("%.1f", stats.averageRating)
                binding.tvStatRank.text = "#${stats.rank}"
                binding.tvStatTime.text = "${stats.averageResolutionTimeMinutes.toInt()}m"
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
