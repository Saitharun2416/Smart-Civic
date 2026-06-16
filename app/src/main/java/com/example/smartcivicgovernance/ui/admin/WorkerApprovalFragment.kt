package com.example.smartcivicgovernance.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcivicgovernance.databinding.FragmentWorkerApprovalBinding
import com.example.smartcivicgovernance.ui.adapter.WorkerApprovalAdapter

class WorkerApprovalFragment : Fragment() {

    private var _binding: FragmentWorkerApprovalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminViewModel by viewModels()
    private lateinit var approvalAdapter: WorkerApprovalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWorkerApprovalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()

        binding.swipeRefreshWorkerApproval.setOnRefreshListener {
            viewModel.loadAllUsers()
        }

        viewModel.loadAllUsers()
    }

    private fun setupRecyclerView() {
        approvalAdapter = WorkerApprovalAdapter { worker ->
            viewModel.setUserAccountStatus(worker.uid, true)
        }
        binding.rvWorkerApprovalList.layoutManager = LinearLayoutManager(context)
        binding.rvWorkerApprovalList.adapter = approvalAdapter
    }

    private fun setupObservers() {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            val pendingWorkers = users.filter { it.role == "worker" && it.disabled }
            approvalAdapter.submitList(pendingWorkers)

            if (pendingWorkers.isEmpty()) {
                binding.tvWorkerApprovalEmpty.visibility = View.VISIBLE
                binding.swipeRefreshWorkerApproval.visibility = View.GONE
            } else {
                binding.tvWorkerApprovalEmpty.visibility = View.GONE
                binding.swipeRefreshWorkerApproval.visibility = View.VISIBLE
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshWorkerApproval.isRefreshing = isLoading
        }

        viewModel.actionSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Worker approved successfully", Toast.LENGTH_SHORT).show()
                viewModel.loadAllUsers()
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
