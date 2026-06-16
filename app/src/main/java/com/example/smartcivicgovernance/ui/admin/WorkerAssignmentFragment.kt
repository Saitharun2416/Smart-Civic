package com.example.smartcivicgovernance.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcivicgovernance.data.model.Complaint
import com.example.smartcivicgovernance.data.model.Worker
import com.example.smartcivicgovernance.databinding.FragmentWorkerAssignmentBinding
import com.example.smartcivicgovernance.ui.adapter.ComplaintAdapter

class WorkerAssignmentFragment : Fragment() {

    private var _binding: FragmentWorkerAssignmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminViewModel by viewModels()
    private lateinit var complaintAdapter: ComplaintAdapter

    private var rawComplaintsList: List<Complaint> = emptyList()
    private var workerList: List<Worker> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWorkerAssignmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()

        viewModel.loadAllComplaints()
        viewModel.loadAllWorkers()
    }

    private fun setupRecyclerView() {
        complaintAdapter = ComplaintAdapter { complaint ->
            showAssignWorkerDialog(complaint)
        }
        binding.rvPendingAssignList.layoutManager = LinearLayoutManager(context)
        binding.rvPendingAssignList.adapter = complaintAdapter
    }

    private fun setupObservers() {
        viewModel.complaints.observe(viewLifecycleOwner) { complaints ->
            rawComplaintsList = complaints
            applyFilters()
        }

        viewModel.workers.observe(viewLifecycleOwner) { workers ->
            workerList = workers
        }

        viewModel.actionSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Worker assigned successfully", Toast.LENGTH_SHORT).show()
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
        val filtered = rawComplaintsList.filter { it.status == "Pending" }
        complaintAdapter.submitList(filtered)

        if (filtered.isEmpty()) {
            binding.tvAssignEmpty.visibility = View.VISIBLE
            binding.rvPendingAssignList.visibility = View.GONE
        } else {
            binding.tvAssignEmpty.visibility = View.GONE
            binding.rvPendingAssignList.visibility = View.VISIBLE
        }
    }

    private fun showAssignWorkerDialog(complaint: Complaint) {
        if (workerList.isEmpty()) {
            Toast.makeText(context, "No workers available to assign", Toast.LENGTH_SHORT).show()
            return
        }

        val workerNames = workerList.map { "${it.name} (${it.activeTasks} active tasks)" }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Assign Worker to Complaint")
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
