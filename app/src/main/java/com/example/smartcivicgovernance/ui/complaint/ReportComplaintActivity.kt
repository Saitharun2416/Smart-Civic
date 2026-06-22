package com.example.smartcivicgovernance.ui.complaint

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.smartcivicgovernance.R
import com.example.smartcivicgovernance.databinding.ActivityReportComplaintBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportComplaintActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportComplaintBinding
    private val viewModel: ComplaintViewModel by viewModels()

    private var selectedLatLng: LatLng? = LatLng(40.7128, -74.0060)
    private var selectedAddress: String = ""

    private var imageUri: Uri? = null
    private var photoFile: File? = null

    // Permission launchers
    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            fetchCurrentLocation()
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            dispatchTakePictureIntent()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Media launchers
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            imageUri = result.data?.data
            binding.ivComplaintImage.setImageURI(imageUri)
        }
    }

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            binding.ivComplaintImage.setImageURI(imageUri)
        } else {
            imageUri = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportComplaintBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Setup category spinner
        val categories = arrayOf("Pothole", "Garbage", "Water Leakage", "Drainage", "Streetlight", "Traffic", "Other")
        val spinnerAdapter = ArrayAdapter(this, R.layout.spinner_item, categories)
        binding.spinnerCategory.setAdapter(spinnerAdapter)
        binding.spinnerCategory.setText(categories[0], false)

        setupListeners()
        setupObservers()

        // Check location permissions and pre-fill address on start
        checkLocationPermissions()
    }

    private fun setupListeners() {
        binding.btnGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        binding.btnCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent()
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.tilAddress.setEndIconOnClickListener {
            checkLocationPermissions()
        }

        binding.btnSubmit.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val category = binding.spinnerCategory.text.toString()
            val address = binding.etAddress.text.toString().trim()

            if (title.isEmpty() || description.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var lat = selectedLatLng?.latitude ?: 0.0
            var lon = selectedLatLng?.longitude ?: 0.0
            if (address != selectedAddress) {
                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocationName(address, 1)
                    if (!addresses.isNullOrEmpty()) {
                        lat = addresses[0].latitude
                        lon = addresses[0].longitude
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            viewModel.reportNewComplaint(
                title = title,
                description = description,
                category = category,
                imageUri = imageUri,
                latitude = lat,
                longitude = lon,
                address = address
            )
        }
    }

    private fun setupObservers() {
        viewModel.loading.observe(this) { isLoading ->
            binding.btnSubmit.isEnabled = !isLoading
            binding.btnSubmit.text = if (isLoading) "Submitting..." else "Submit Complaint"
        }

        viewModel.reportSuccess.observe(this) { complaintId ->
            if (complaintId != null) {
                Toast.makeText(this, "Complaint reported successfully!", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        viewModel.error.observe(this) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkLocationPermissions() {
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            fetchCurrentLocation()
        } else {
            requestLocationPermissionLauncher.launch(permissions)
        }
    }

    private fun fetchCurrentLocation() {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    selectedLatLng = currentLatLng
                    resolveAddress(location.latitude, location.longitude)
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun resolveAddress(lat: Double, lon: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addressLine = addresses[0].getAddressLine(0)
                selectedAddress = addressLine
                binding.etAddress.setText(selectedAddress)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            binding.etAddress.setText("$lat, $lon")
            selectedAddress = "$lat, $lon"
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.smartcivicgovernance.fileprovider",
                        it
                    )
                    imageUri = photoURI
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePhotoLauncher.launch(takePictureIntent)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            photoFile = this
        }
    }
}
