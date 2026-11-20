package com.steptrack.pro

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.steptrack.pro.data.StepRepository
import com.steptrack.pro.database.AppDatabase
import com.steptrack.pro.databinding.ActivityMainBinding
import com.steptrack.pro.service.StepCounterService
import com.steptrack.pro.utils.DateUtils
import com.steptrack.pro.utils.PermissionUtils
import kotlinx.coroutines.launch
import java.text.NumberFormat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: StepRepository
    private var permanentlyDenied = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (!PermissionUtils.hasActivityRecognition(this) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            ) {
                permanentlyDenied = !ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.ACTIVITY_RECOGNITION
                )
            }
            refreshState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = StepRepository(AppDatabase.get(applicationContext))

        binding.versionText.text = getString(
            R.string.version_format,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        )
        binding.actionButton.setOnClickListener { onActionClicked() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val key = DateUtils.dateKey(System.currentTimeMillis())
                repository.observeDay(key).collect { day ->
                    binding.stepsText.text =
                        NumberFormat.getInstance().format(day?.totalSteps ?: 0)
                    binding.detailsText.text = getString(
                        R.string.details_format,
                        day?.distanceKm ?: 0.0,
                        day?.calories ?: 0.0
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        refreshState()
    }

    private fun refreshState() {
        if (!StepCounterService.isStepSensorAvailable(this)) {
            binding.statusText.setText(R.string.status_no_sensor)
            binding.actionButton.visibility = View.GONE
            return
        }

        if (!PermissionUtils.hasActivityRecognition(this)) {
            if (permanentlyDenied) {
                binding.statusText.setText(R.string.status_permission_blocked)
                binding.actionButton.setText(R.string.action_open_settings)
            } else {
                binding.statusText.setText(R.string.status_permission_needed)
                binding.actionButton.setText(R.string.action_grant)
            }
            binding.actionButton.visibility = View.VISIBLE
            return
        }

        StepCounterService.start(this)
        binding.statusText.setText(R.string.status_tracking)
        binding.actionButton.visibility = View.GONE
    }

    private fun onActionClicked() {
        if (permanentlyDenied) {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
            )
            return
        }
        val missing = PermissionUtils.missingPermissions(this)
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }
}
