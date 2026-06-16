package com.example.smartcivicgovernance.ui.citizen

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcivicgovernance.data.model.Complaint
import com.example.smartcivicgovernance.databinding.FragmentCitizenHomeBinding
import com.example.smartcivicgovernance.ui.adapter.ComplaintAdapter
import com.example.smartcivicgovernance.ui.complaint.ComplaintDetailActivity
import com.example.smartcivicgovernance.ui.complaint.ReportComplaintActivity

class CitizenHomeFragment : Fragment() {

    private var _binding: FragmentCitizenHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CitizenViewModel by viewModels()
    private lateinit var adapter: ComplaintAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCitizenHomeBinding.inflate(inflater, container, false)
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

        binding.btnEmptyReport.setOnClickListener {
            startActivity(Intent(requireContext(), ReportComplaintActivity::class.java))
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
            adapter.submitList(list)

            // Update stats
            val total = list.size
            val pending = list.count { it.status == "Pending" }
            val resolved = list.count { it.status == "Resolved" }

            binding.tvTotalCount.text = total.toString()
            binding.tvPendingCount.text = pending.toString()
            binding.tvResolvedCount.text = resolved.toString()

            if (list.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.rvComplaints.visibility = View.GONE
            } else {
                binding.layoutEmpty.visibility = View.GONE
                binding.rvComplaints.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
