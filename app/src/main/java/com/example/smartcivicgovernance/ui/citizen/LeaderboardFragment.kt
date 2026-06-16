package com.example.smartcivicgovernance.ui.citizen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcivicgovernance.databinding.FragmentLeaderboardBinding
import com.example.smartcivicgovernance.ui.adapter.WorkerAdapter

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CitizenViewModel by viewModels()
    private lateinit var adapter: WorkerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        setupObservers()

        viewModel.loadLeaderboard()
    }

    private fun setupRecyclerView() {
        adapter = WorkerAdapter { worker ->
            Toast.makeText(context, "${worker.name}: ${worker.totalPoints} points", Toast.LENGTH_SHORT).show()
        }
        binding.rvLeaderboard.layoutManager = LinearLayoutManager(context)
        binding.rvLeaderboard.adapter = adapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadLeaderboard()
        }
    }

    private fun setupObservers() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
            binding.shimmerView.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                binding.rvLeaderboard.visibility = View.GONE
                binding.layoutEmpty.visibility = View.GONE
                binding.shimmerView.startShimmer()
            } else {
                binding.shimmerView.stopShimmer()
            }
        }

        viewModel.leaderboard.observe(viewLifecycleOwner) { list ->
            binding.rvLeaderboard.visibility = View.VISIBLE
            adapter.submitList(list)

            if (list.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.rvLeaderboard.visibility = View.GONE
            } else {
                binding.layoutEmpty.visibility = View.GONE
                binding.rvLeaderboard.visibility = View.VISIBLE
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
