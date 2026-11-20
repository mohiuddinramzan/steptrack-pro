package com.steptrack.pro

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.steptrack.pro.data.StepRepository
import com.steptrack.pro.database.AppDatabase
import com.steptrack.pro.databinding.ActivityMainBinding
import com.steptrack.pro.service.StepCounterService
import com.steptrack.pro.utils.BatteryUtils
import com.steptrack.pro.utils.PermissionUtils
import com.steptrack.pro.utils.TimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Date

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

    @OptIn(ExperimentalCoroutinesApi::class)
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
        binding.batteryButton.setOnClickListener { showBatteryGuidance() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    // Re-subscribes automatically when the date changes at midnight.
                    TimeUtils.todayKeyFlow()
                        .flatMapLatest { key -> repository.observeDay(key) }
                        .collect { day ->
                            binding.stepsText.text =
                                NumberFormat.getInstance().format(day?.totalSteps ?: 0)
                            binding.detailsText.text = getString(
                                R.string.details_format,
                                day?.distanceKm ?: 0.0,
                                day?.calories ?: 0.0
                            )
                        }
                }
                launch {
                    repository.observeState().collect { state ->
                        binding.lastStepText.text = if (state == null) {
                            getString(R.string.last_step_none)
                        } else {
                            val time = android.text.format.DateFormat.getTimeFormat(this@MainActivity)
                                .format(Date(state.lastEventTimeMillis))
                            getString(R.string.last_step_format, time)
                        }
                    }
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
            binding.batteryButton.visibility = View.GONE
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
            binding.batteryButton.visibility = View.GONE
            return
        }

        StepCounterService.start(this)
        binding.statusText.setText(R.string.status_tracking)
        binding.actionButton.visibility = View.GONE
        binding.batteryButton.visibility = View.VISIBLE
    }

    private fun onActionClicked() {
        if (permanentlyDenied) {
            BatteryUtils.openAppSettings(this)
            return
        }
        val missing = PermissionUtils.missingPermissions(this)
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun showBatteryGuidance() {
        val status = if (BatteryUtils.isIgnoringOptimizations(this)) {
            getString(R.string.battery_status_ok)
        } else {
            getString(R.string.battery_status_restricted)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.battery_title)
            .setMessage(status + "\n\n" + getString(R.string.battery_guide))
            .setPositiveButton(R.string.battery_open_settings) { _, _ ->
                BatteryUtils.openBatteryOptimizationSettings(this)
            }
            .setNeutralButton(R.string.battery_open_app) { _, _ ->
                BatteryUtils.openAppSettings(this)
            }
            .setNegativeButton(R.string.battery_close, null)
            .show()
    }
}
