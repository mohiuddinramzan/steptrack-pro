package com.steptrack.pro.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.steptrack.pro.BuildConfig
import com.steptrack.pro.R
import com.steptrack.pro.data.DailyActivity
import com.steptrack.pro.data.StepRepository
import com.steptrack.pro.data.UserProfile
import com.steptrack.pro.database.AppDatabase
import com.steptrack.pro.databinding.FragmentHomeBinding
import com.steptrack.pro.service.StepCounterService
import com.steptrack.pro.utils.BatteryUtils
import com.steptrack.pro.utils.DashboardUtils
import com.steptrack.pro.utils.PermissionUtils
import com.steptrack.pro.utils.TimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Date

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: StepRepository
    private val profile = UserProfile()
    private var permanentlyDenied = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (_binding == null) return@registerForActivityResult
            if (!PermissionUtils.hasActivityRecognition(requireContext()) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            ) {
                permanentlyDenied = !ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(), Manifest.permission.ACTIVITY_RECOGNITION
                )
            }
            refreshState()
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)
        repository = StepRepository(AppDatabase.get(requireContext().applicationContext))

        binding.versionText.text = getString(
            R.string.version_format,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        )
        binding.actionButton.setOnClickListener { onActionClicked() }
        binding.batteryButton.setOnClickListener { showBatteryGuidance() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    // Re-subscribes automatically when the date changes at midnight.
                    TimeUtils.todayKeyFlow()
                        .flatMapLatest { key -> repository.observeDay(key) }
                        .collect { day -> renderDay(day) }
                }
                launch {
                    TimeUtils.todayKeyFlow()
                        .flatMapLatest { key ->
                            val start = LocalDate.parse(key).minusDays(6).toString()
                            repository.observeRange(start, key)
                                .map { rows -> DashboardUtils.buildWeek(rows, key) }
                        }
                        .collect { bars -> binding.weekChart.setData(bars, profile.stepGoal) }
                }
                launch {
                    repository.observeState().collect { state ->
                        binding.lastStepText.text = if (state == null) {
                            getString(R.string.last_step_none)
                        } else {
                            val time = android.text.format.DateFormat
                                .getTimeFormat(requireContext())
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun renderDay(day: DailyActivity?) {
        val steps = day?.totalSteps ?: 0
        val number = NumberFormat.getInstance()
        binding.stepsText.text = number.format(steps)
        binding.goalText.text = getString(R.string.goal_format, number.format(profile.stepGoal))
        binding.percentText.text =
            getString(R.string.percent_format, DashboardUtils.percent(steps, profile.stepGoal))
        binding.ringView.setProgress(DashboardUtils.fraction(steps, profile.stepGoal))
        binding.distanceValue.text = getString(R.string.value_km, day?.distanceKm ?: 0.0)
        binding.caloriesValue.text = getString(R.string.value_kcal, day?.calories ?: 0.0)
        binding.timeValue.text = DashboardUtils.formatDuration(day?.activeSeconds ?: 0L)
    }

    private fun refreshState() {
        if (_binding == null) return
        val context = requireContext()

        if (!StepCounterService.isStepSensorAvailable(context)) {
            binding.statusText.setText(R.string.status_no_sensor)
            binding.actionButton.visibility = View.GONE
            binding.batteryButton.visibility = View.GONE
            return
        }

        if (!PermissionUtils.hasActivityRecognition(context)) {
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

        StepCounterService.start(context)
        binding.statusText.setText(R.string.status_tracking)
        binding.actionButton.visibility = View.GONE
        binding.batteryButton.visibility = View.VISIBLE
    }

    private fun onActionClicked() {
        if (permanentlyDenied) {
            BatteryUtils.openAppSettings(requireActivity())
            return
        }
        val missing = PermissionUtils.missingPermissions(requireContext())
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun showBatteryGuidance() {
        val activity = requireActivity()
        val status = if (BatteryUtils.isIgnoringOptimizations(activity)) {
            getString(R.string.battery_status_ok)
        } else {
            getString(R.string.battery_status_restricted)
        }
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.battery_title)
            .setMessage(status + "\n\n" + getString(R.string.battery_guide))
            .setPositiveButton(R.string.battery_open_settings) { _, _ ->
                BatteryUtils.openBatteryOptimizationSettings(activity)
            }
            .setNeutralButton(R.string.battery_open_app) { _, _ ->
                BatteryUtils.openAppSettings(activity)
            }
            .setNegativeButton(R.string.battery_close, null)
            .show()
    }
}
