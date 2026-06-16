package com.example.smartcivicgovernance.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcivicgovernance.data.model.Complaint
import com.example.smartcivicgovernance.databinding.FragmentAdminOverviewBinding
import com.example.smartcivicgovernance.ui.adapter.WorkerAdapter
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class AdminOverviewFragment : Fragment() {

    private var _binding: FragmentAdminOverviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminViewModel by viewModels()
    private lateinit var workerAdapter: WorkerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAdminOverviewBinding.inflate(inflater, container, false)
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
        workerAdapter = WorkerAdapter { worker ->
            Toast.makeText(context, "${worker.name} clicked", Toast.LENGTH_SHORT).show()
        }
        binding.rvTopWorkers.layoutManager = LinearLayoutManager(context)
        binding.rvTopWorkers.adapter = workerAdapter
    }

    private fun setupObservers() {
        viewModel.complaints.observe(viewLifecycleOwner) { complaints ->
            populateCharts(complaints)
        }

        viewModel.workers.observe(viewLifecycleOwner) { list ->
            // Filter top 5 workers by points
            val topWorkers = list.sortedByDescending { it.totalPoints }.take(5)
            workerAdapter.submitList(topWorkers)
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populateCharts(complaints: List<Complaint>) {
        if (complaints.isEmpty()) return

        // 1. Pie Chart - Status
        val pendingCount = complaints.count { it.status == "Pending" }.toFloat()
        val progressCount = complaints.count { it.status == "In Progress" }.toFloat()
        val resolvedCount = complaints.count { it.status == "Resolved" }.toFloat()
        val rejectedCount = complaints.count { it.status == "Rejected" }.toFloat()

        val statusEntries = ArrayList<PieEntry>()
        if (pendingCount > 0) statusEntries.add(PieEntry(pendingCount, "Pending"))
        if (progressCount > 0) statusEntries.add(PieEntry(progressCount, "In Progress"))
        if (resolvedCount > 0) statusEntries.add(PieEntry(resolvedCount, "Resolved"))
        if (rejectedCount > 0) statusEntries.add(PieEntry(rejectedCount, "Rejected"))

        val statusDataSet = PieDataSet(statusEntries, "Status Breakdown")
        statusDataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
        binding.pieChartStatus.data = PieData(statusDataSet)
        binding.pieChartStatus.description.isEnabled = false
        binding.pieChartStatus.animateY(1000)
        binding.pieChartStatus.invalidate()

        // 2. Bar Chart - Category
        val categories = arrayOf("Pothole", "Garbage", "Water Leakage", "Drainage", "Streetlight", "Traffic")
        val categoryEntries = ArrayList<BarEntry>()

        categories.forEachIndexed { index, cat ->
            val count = complaints.count { it.category == cat }.toFloat()
            categoryEntries.add(BarEntry(index.toFloat(), count))
        }

        val categoryDataSet = BarDataSet(categoryEntries, "Complaints by Category")
        categoryDataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        binding.barChartCategory.data = BarData(categoryDataSet)
        binding.barChartCategory.description.isEnabled = false
        binding.barChartCategory.animateY(1000)
        binding.barChartCategory.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
