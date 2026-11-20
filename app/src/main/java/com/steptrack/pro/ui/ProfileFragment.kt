package com.steptrack.pro.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.steptrack.pro.R
import com.steptrack.pro.data.SettingsStore
import com.steptrack.pro.databinding.FragmentProfileBinding
import com.steptrack.pro.utils.ProfileValidator

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var settings: SettingsStore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)
        settings = SettingsStore(requireContext().applicationContext)

        val profile = settings.load()
        binding.heightInput.setText(profile.heightCm.toString())
        binding.weightInput.setText(profile.weightKg.toString())
        binding.saveButton.setOnClickListener { save() }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun save() {
        val height = ProfileValidator.parseHeight(binding.heightInput.text?.toString().orEmpty())
        val weight = ProfileValidator.parseWeight(binding.weightInput.text?.toString().orEmpty())

        binding.heightLayout.error = if (height == null) getString(R.string.error_height) else null
        binding.weightLayout.error = if (weight == null) getString(R.string.error_weight) else null
        if (height == null || weight == null) return

        binding.heightInput.clearFocus()
        binding.weightInput.clearFocus()
        settings.save(settings.load().copy(heightCm = height, weightKg = weight))
        Snackbar.make(binding.root, R.string.profile_saved, Snackbar.LENGTH_SHORT).show()
    }
}
