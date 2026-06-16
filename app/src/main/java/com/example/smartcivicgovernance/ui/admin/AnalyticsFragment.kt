package com.example.smartcivicgovernance.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.smartcivicgovernance.R
import com.example.smartcivicgovernance.data.model.Complaint
import com.example.smartcivicgovernance.databinding.FragmentAnalyticsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class AnalyticsFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminViewModel by viewModels()
    private var googleMap: GoogleMap? = null
    private var complaintsList: List<Complaint> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Map
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupObservers()
        viewModel.loadAllComplaints()
    }

    private fun setupObservers() {
        viewModel.complaints.observe(viewLifecycleOwner) { complaints ->
            complaintsList = complaints
            populateLineChart(complaints)
            populateBarChart(complaints)
            updateMapMarkers()
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        updateMapMarkers()
    }

    private fun updateMapMarkers() {
        val map = googleMap ?: return
        map.clear()

        var hasMarker = false
        var lastLatLng = LatLng(20.5937, 78.9629) // Default India center

        complaintsList.forEach { complaint ->
            if (complaint.latitude != 0.0 && complaint.longitude != 0.0) {
                val position = LatLng(complaint.latitude, complaint.longitude)
                
                val markerColor = when (complaint.status) {
                    "Pending" -> BitmapDescriptorFactory.HUE_RED
                    "In Progress" -> BitmapDescriptorFactory.HUE_ORANGE
                    "Resolved" -> BitmapDescriptorFactory.HUE_GREEN
                    else -> BitmapDescriptorFactory.HUE_RED
                }

                map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(complaint.title)
                        .snippet("Status: ${complaint.status}")
                        .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                )
                lastLatLng = position
                hasMarker = true
            }
        }

        if (hasMarker) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLatLng, 10f))
        }
    }

    private fun populateLineChart(complaints: List<Complaint>) {
        if (complaints.isEmpty()) return

        // Count complaints in the last 30 days
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val dateCounts = LinkedHashMap<String, Int>()

        // Initialize last 30 days with 0
        for (i in 29 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = dateFormat.format(c.time)
            dateCounts[dateStr] = 0
        }

        // Fill counts
        complaints.forEach { complaint ->
            complaint.createdAt?.toDate()?.let { date ->
                val dateStr = dateFormat.format(date)
                if (dateCounts.containsKey(dateStr)) {
                    dateCounts[dateStr] = dateCounts[dateStr]!! + 1
                }
            }
        }

        val entries = ArrayList<Entry>()
        val dates = dateCounts.keys.toList()
        dates.forEachIndexed { index, date ->
            entries.add(Entry(index.toFloat(), dateCounts[date]!!.toFloat()))
        }

        val dataSet = LineDataSet(entries, "Complaints Filed")
        dataSet.color = ColorTemplate.COLORFUL_COLORS[0]
        dataSet.valueTextSize = 10f
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(false)

        val lineData = LineData(dataSet)
        binding.lineChartTrends.data = lineData

        val xAxis = binding.lineChartTrends.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(dates)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f

        binding.lineChartTrends.description.isEnabled = false
        binding.lineChartTrends.animateX(1000)
        binding.lineChartTrends.invalidate()
    }

    private fun populateBarChart(complaints: List<Complaint>) {
        val categories = arrayOf("Pothole", "Garbage", "Water Leakage", "Drainage", "Streetlight", "Traffic")
        val categoryAverages = FloatArray(categories.size)

        categories.forEachIndexed { index, cat ->
            val resolvedCatComplaints = complaints.filter {
                it.category.equals(cat, ignoreCase = true) && 
                it.status == "Resolved" && 
                it.acceptedAt != null && 
                it.resolvedAt != null
            }

            if (resolvedCatComplaints.isNotEmpty()) {
                val totalHours = resolvedCatComplaints.map {
                    val diff = it.resolvedAt!!.toDate().time - it.acceptedAt!!.toDate().time
                    diff.toFloat() / (1000 * 60 * 60)
                }.sum()
                categoryAverages[index] = totalHours / resolvedCatComplaints.size
            } else {
                categoryAverages[index] = 0f
            }
        }

        val entries = ArrayList<BarEntry>()
        categories.forEachIndexed { index, _ ->
            entries.add(BarEntry(index.toFloat(), categoryAverages[index]))
        }

        val dataSet = BarDataSet(entries, "Avg Hours to Resolve")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 10f

        val barData = BarData(dataSet)
        binding.barChartResolutionTime.data = barData

        val xAxis = binding.barChartResolutionTime.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(categories.toList())
        xAxis.granularity = 1f

        binding.barChartResolutionTime.description.isEnabled = false
        binding.barChartResolutionTime.animateY(1000)
        binding.barChartResolutionTime.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
