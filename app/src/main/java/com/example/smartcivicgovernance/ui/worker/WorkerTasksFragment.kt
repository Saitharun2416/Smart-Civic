package com.example.smartcivicgovernance.ui.worker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcivicgovernance.databinding.FragmentWorkerTasksBinding
import com.example.smartcivicgovernance.ui.adapter.ComplaintAdapter
import com.example.smartcivicgovernance.ui.worker.TaskDetailActivity

class WorkerTasksFragment : Fragment() {

    private var _binding: FragmentWorkerTasksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkerViewModel by viewModels()
    private lateinit var activeAdapter: ComplaintAdapter
    private lateinit var availableAdapter: ComplaintAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWorkerTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupListeners()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadTasks()
    }

    private fun setupRecyclerViews() {
        activeAdapter = ComplaintAdapter { complaint ->
            val intent = Intent(requireContext(), TaskDetailActivity::class.java).apply {
                putExtra("COMPLAINT_ID", complaint.complaintId)
            }
            startActivity(intent)
        }
        binding.rvActiveTasks.layoutManager = LinearLayoutManager(context)
        binding.rvActiveTasks.adapter = activeAdapter

        availableAdapter = ComplaintAdapter { complaint ->
            val intent = Intent(requireContext(), TaskDetailActivity::class.java).apply {
                putExtra("COMPLAINT_ID", complaint.complaintId)
            }
            startActivity(intent)
        }
        binding.rvAvailableTasks.layoutManager = LinearLayoutManager(context)
        binding.rvAvailableTasks.adapter = availableAdapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadTasks()
        }
    }

    private fun setupObservers() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
            
            binding.shimmerActive.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.shimmerAvailable.visibility = if (isLoading) View.VISIBLE else View.GONE

            if (isLoading) {
                binding.rvActiveTasks.visibility = View.GONE
                binding.rvAvailableTasks.visibility = View.GONE
                binding.tvActiveEmpty.visibility = View.GONE
                binding.tvAvailableEmpty.visibility = View.GONE
                
                binding.shimmerActive.startShimmer()
                binding.shimmerAvailable.startShimmer()
            } else {
                binding.shimmerActive.stopShimmer()
                binding.shimmerAvailable.stopShimmer()
            }
        }

        viewModel.activeTasks.observe(viewLifecycleOwner) { list ->
            binding.rvActiveTasks.visibility = View.VISIBLE
            activeAdapter.submitList(list)

            if (list.isEmpty()) {
                binding.tvActiveEmpty.visibility = View.VISIBLE
                binding.rvActiveTasks.visibility = View.GONE
            } else {
                binding.tvActiveEmpty.visibility = View.GONE
                binding.rvActiveTasks.visibility = View.VISIBLE
            }
        }

        viewModel.availableTasks.observe(viewLifecycleOwner) { list ->
            binding.rvAvailableTasks.visibility = View.VISIBLE
            availableAdapter.submitList(list)

            if (list.isEmpty()) {
                binding.tvAvailableEmpty.visibility = View.VISIBLE
                binding.rvAvailableTasks.visibility = View.GONE
            } else {
                binding.tvAvailableEmpty.visibility = View.GONE
                binding.rvAvailableTasks.visibility = View.VISIBLE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
