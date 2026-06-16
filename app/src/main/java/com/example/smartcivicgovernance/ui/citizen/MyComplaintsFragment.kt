package com.example.smartcivicgovernance.ui.citizen

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcivicgovernance.R
import com.example.smartcivicgovernance.data.model.Complaint
import com.example.smartcivicgovernance.databinding.FragmentMyComplaintsBinding
import com.example.smartcivicgovernance.ui.adapter.ComplaintAdapter
import com.example.smartcivicgovernance.ui.complaint.ComplaintDetailActivity

class MyComplaintsFragment : Fragment() {

    private var _binding: FragmentMyComplaintsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CitizenViewModel by viewModels()
    private lateinit var adapter: ComplaintAdapter
    private var rawComplaintsList: List<Complaint> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMyComplaintsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        setupObservers()

        viewModel.loadCitizenComplaints()
    }

    private fun setupRecyclerView() {
        adapter = ComplaintAdapter { complaint ->
            val intent = Intent(requireContext(), ComplaintDetailActivity::class.java).apply {
                putExtra("COMPLAINT_ID", complaint.complaintId)
            }
            startActivity(intent)
        }
        binding.rvComplaints.layoutManager = LinearLayoutManager(context)
        binding.rvComplaints.adapter = adapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadCitizenComplaints()
        }

        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            filterComplaints(checkedIds.firstOrNull())
        }
    }

    private fun filterComplaints(checkedId: Int?) {
        val filteredList = when (checkedId) {
            R.id.chipPending -> rawComplaintsList.filter { it.status == "Pending" }
            R.id.chipInProgress -> rawComplaintsList.filter { it.status == "In Progress" }
            R.id.chipResolved -> rawComplaintsList.filter { it.status == "Resolved" }
            else -> rawComplaintsList
        }

        adapter.submitList(filteredList)

        if (filteredList.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvComplaints.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvComplaints.visibility = View.VISIBLE
        }
    }

    private fun setupObservers() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
            binding.shimmerView.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                binding.rvComplaints.visibility = View.GONE
                binding.layoutEmpty.visibility = View.GONE
                binding.shimmerView.startShimmer()
            } else {
                binding.shimmerView.stopShimmer()
            }
        }

        viewModel.complaints.observe(viewLifecycleOwner) { list ->
            binding.rvComplaints.visibility = View.VISIBLE
            rawComplaintsList = list
            filterComplaints(binding.chipGroupFilters.checkedChipId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
