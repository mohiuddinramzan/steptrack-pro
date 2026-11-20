package com.steptrack.pro.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.steptrack.pro.R
import com.steptrack.pro.data.DailyActivity
import com.steptrack.pro.data.SettingsStore
import com.steptrack.pro.data.StepRepository
import com.steptrack.pro.database.AppDatabase
import com.steptrack.pro.databinding.FragmentGoalsBinding
import com.steptrack.pro.utils.AchievementUtils
import com.steptrack.pro.utils.DateUtils
import com.steptrack.pro.utils.ProfileValidator
import com.steptrack.pro.utils.StreakUtils
import com.steptrack.pro.utils.TimeUtils
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

class GoalsFragment : Fragment(R.layout.fragment_goals) {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    private lateinit var settings: SettingsStore
    private lateinit var repository: StepRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentGoalsBinding.bind(view)
        val appContext = requireContext().applicationContext
        settings = SettingsStore(appContext)
        repository = StepRepository(AppDatabase.get(appContext))

        binding.goalInput.setText(settings.load().stepGoal.toString())
        binding.saveButton.setOnClickListener { saveGoal() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(TimeUtils.todayKeyFlow(), repository.observeAll()) { key, rows -> key to rows }
                    .collect { (key, rows) -> render(key, rows) }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun saveGoal() {
        val goal = ProfileValidator.parseGoal(binding.goalInput.text?.toString().orEmpty())
        if (goal == null) {
            binding.goalLayout.error = getString(R.string.error_goal)
            return
        }
        binding.goalLayout.error = null
        binding.goalInput.clearFocus()
        settings.save(settings.load().copy(stepGoal = goal))

        val today = DateUtils.dateKey(System.currentTimeMillis())
        requireActivity().lifecycleScope.launch { repository.refreshGoalFlag(today, goal) }
        Snackbar.make(binding.root, R.string.goal_saved, Snackbar.LENGTH_SHORT).show()
    }

    private fun render(todayKey: String, rows: List<DailyActivity>) {
        val goalDates = StreakUtils.goalDates(rows)
        val current = StreakUtils.current(goalDates.toSet(), LocalDate.parse(todayKey))
        val best = StreakUtils.best(goalDates)
        binding.currentStreakValue.text = resources.getQuantityString(R.plurals.streak_days, current, current)
        binding.bestStreakValue.text = resources.getQuantityString(R.plurals.streak_days, best, best)

        binding.achievementsContainer.removeAllViews()
        for (achievement in AchievementUtils.evaluate(rows)) {
            val row = layoutInflater.inflate(
                R.layout.item_achievement, binding.achievementsContainer, false
            )
            row.findViewById<TextView>(R.id.achTitle).setText(AchievementStrings.title(achievement.id))
            row.findViewById<TextView>(R.id.achDesc).setText(AchievementStrings.description(achievement.id))
            val date = achievement.unlockedOn
            row.findViewById<TextView>(R.id.achStatus).text =
                if (date != null) AchievementStrings.formatDate(date) else getString(R.string.ach_status_locked)
            row.alpha = if (achievement.unlocked) 1f else 0.55f
            binding.achievementsContainer.addView(row)
        }
    }
}
