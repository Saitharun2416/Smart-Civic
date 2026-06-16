package com.example.smartcivicgovernance.ui.admin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcivicgovernance.R
import com.example.smartcivicgovernance.data.model.Complaint
import com.example.smartcivicgovernance.data.model.Worker
import com.example.smartcivicgovernance.databinding.FragmentComplaintsManagementBinding
import com.example.smartcivicgovernance.ui.adapter.ComplaintAdapter
import com.example.smartcivicgovernance.ui.complaint.ComplaintDetailActivity

class ComplaintsManagementFragment : Fragment() {

    private var _binding: FragmentComplaintsManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminViewModel by viewModels()
    private lateinit var complaintAdapter: ComplaintAdapter

    private var rawComplaintsList: List<Complaint> = emptyList()
    private var workerList: List<Worker> = emptyList()

    private var selectedStatus: String = "All"
    private var selectedCategory: String = "All"
    private var searchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentComplaintsManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilters()
        setupSearch()
        setupObservers()

        binding.swipeRefreshComplaints.setOnRefreshListener {
            viewModel.loadAllComplaints()
            viewModel.loadAllWorkers()
        }

        viewModel.loadAllComplaints()
        viewModel.loadAllWorkers()
    }

    private fun setupRecyclerView() {
        complaintAdapter = ComplaintAdapter { complaint ->
            if (complaint.status == "Pending") {
                showAssignWorkerDialog(complaint)
            } else {
                val intent = Intent(requireContext(), ComplaintDetailActivity::class.java).apply {
                    putExtra("COMPLAINT_ID", complaint.complaintId)
                }
                startActivity(intent)
            }
        }
        binding.rvAdminComplaints.layoutManager = LinearLayoutManager(context)
        binding.rvAdminComplaints.adapter = complaintAdapter
    }

    private fun setupFilters() {
        val statuses = arrayOf("All", "Pending", "In Progress", "Resolved", "Rejected")
        val statusAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, statuses)
        binding.spinnerStatusFilter.setAdapter(statusAdapter)
        binding.spinnerStatusFilter.setText(statuses[0], false)

        binding.spinnerStatusFilter.setOnItemClickListener { _, _, position, _ ->
            selectedStatus = statuses[position]
            applyFilters()
        }

        val categories = arrayOf("All", "Pothole", "Garbage", "Water Leakage", "Drainage", "Streetlight", "Traffic", "Other")
        val categoryAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, categories)
        binding.spinnerCategoryFilter.setAdapter(categoryAdapter)
        binding.spinnerCategoryFilter.setText(categories[0], false)

        binding.spinnerCategoryFilter.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = categories[position]
            applyFilters()
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString().trim()
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupObservers() {
        viewModel.complaints.observe(viewLifecycleOwner) { complaints ->
            rawComplaintsList = complaints
            applyFilters()
        }

        viewModel.workers.observe(viewLifecycleOwner) { workers ->
            workerList = workers
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshComplaints.isRefreshing = isLoading
        }

        viewModel.actionSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Action completed successfully", Toast.LENGTH_SHORT).show()
                viewModel.loadAllComplaints()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyFilters() {
        val filtered = rawComplaintsList.filter { complaint ->
            val matchesStatus = selectedStatus == "All" || complaint.status.equals(selectedStatus, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || complaint.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isEmpty() || 
                    complaint.title.contains(searchQuery, ignoreCase = true) || 
                    complaint.description.contains(searchQuery, ignoreCase = true) || 
                    complaint.address.contains(searchQuery, ignoreCase = true)

            matchesStatus && matchesCategory && matchesSearch
        }
        complaintAdapter.submitList(filtered)
    }

    private fun showAssignWorkerDialog(complaint: Complaint) {
        if (workerList.isEmpty()) {
            Toast.makeText(context, "No workers available to assign", Toast.LENGTH_SHORT).show()
            return
        }

        val workerNames = workerList.map { "${it.name} (${it.activeTasks} active tasks)" }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Assign Worker")
            .setItems(workerNames) { _, which ->
                val selectedWorker = workerList[which]
                viewModel.assignWorkerToComplaint(complaint.complaintId, selectedWorker.uid, selectedWorker.name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
