package com.steptrack.pro.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.steptrack.pro.R
import com.steptrack.pro.data.DailyActivity
import com.steptrack.pro.data.StepRepository
import com.steptrack.pro.data.UserProfile
import com.steptrack.pro.database.AppDatabase
import com.steptrack.pro.databinding.FragmentStatisticsBinding
import com.steptrack.pro.utils.DashboardUtils
import com.steptrack.pro.utils.Period
import com.steptrack.pro.utils.StatsMode
import com.steptrack.pro.utils.StatsUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.WeekFields
import java.util.Locale

class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private data class Query(val mode: StatsMode, val anchor: LocalDate)
    private data class Render(val query: Query, val period: Period, val rows: List<DailyActivity>)

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: StepRepository
    private val profile = UserProfile()
    private val query = MutableStateFlow(Query(StatsMode.WEEK, LocalDate.now()))

    private val firstDay: DayOfWeek
        get() = WeekFields.of(Locale.getDefault()).firstDayOfWeek

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = savedInstanceState?.getString(KEY_MODE)
        val anchor = savedInstanceState?.getString(KEY_ANCHOR)
        if (mode != null && anchor != null) {
            try {
                query.value = Query(StatsMode.valueOf(mode), LocalDate.parse(anchor))
            } catch (e: IllegalArgumentException) {
                // Keep the default query.
            } catch (e: DateTimeParseException) {
                // Keep the default query.
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_MODE, query.value.mode.name)
        outState.putString(KEY_ANCHOR, query.value.anchor.toString())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatisticsBinding.bind(view)
        repository = StepRepository(AppDatabase.get(requireContext().applicationContext))

        binding.modeGroup.check(
            when (query.value.mode) {
                StatsMode.DAY -> R.id.modeDay
                StatsMode.WEEK -> R.id.modeWeek
                StatsMode.MONTH -> R.id.modeMonth
            }
        )
        binding.modeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                R.id.modeDay -> StatsMode.DAY
                R.id.modeMonth -> StatsMode.MONTH
                else -> StatsMode.WEEK
            }
            query.value = query.value.copy(mode = mode)
        }
        binding.prevButton.setOnClickListener { move(-1) }
        binding.nextButton.setOnClickListener { move(1) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                query
                    .flatMapLatest { q ->
                        val period = StatsUtils.periodFor(q.mode, q.anchor, firstDay)
                        repository.observeRange(period.start.toString(), period.end.toString())
                            .map { rows -> Render(q, period, rows) }
                    }
                    .collect { render(it) }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun move(delta: Int) {
        val q = query.value
        if (delta > 0 && !StatsUtils.canGoForward(q.mode, q.anchor, LocalDate.now(), firstDay)) return
        query.value = q.copy(anchor = StatsUtils.shift(q.mode, q.anchor, delta))
    }

    private fun render(r: Render) {
        val today = LocalDate.now()
        val q = r.query
        val summary = StatsUtils.summarize(r.rows, r.period, today)
        val number = NumberFormat.getInstance()

        binding.periodLabel.text = StatsUtils.periodLabel(q.mode, r.period)

        val canForward = StatsUtils.canGoForward(q.mode, q.anchor, today, firstDay)
        binding.nextButton.isEnabled = canForward
        binding.nextButton.alpha = if (canForward) 1f else 0.35f

        if (q.mode == StatsMode.DAY) {
            binding.chartCard.visibility = View.GONE
        } else {
            binding.chartCard.visibility = View.VISIBLE
            binding.statsChart.setData(
                StatsUtils.buildBars(r.rows, r.period, q.mode),
                profile.stepGoal,
                if (q.mode == StatsMode.MONTH) 5 else 1,
                today.toString()
            )
        }

        binding.summaryContainer.removeAllViews()
        addRow(R.string.stat_steps, number.format(summary.totalSteps))
        addRow(R.string.stat_distance, getString(R.string.value_km, summary.distanceKm))
        addRow(R.string.stat_calories, getString(R.string.value_kcal, summary.calories))
        addRow(R.string.stat_time, DashboardUtils.formatDuration(summary.activeSeconds))

        if (q.mode == StatsMode.DAY) {
            addRow(
                R.string.stat_goal,
                getString(if (summary.goalDays > 0) R.string.answer_yes else R.string.answer_no)
            )
        } else {
            addRow(R.string.stat_avg, number.format(summary.avgStepsPerActiveDay))
            summary.bestDate?.let { date ->
                val shortDate = LocalDate.parse(date)
                    .format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
                addRow(
                    R.string.stat_best,
                    getString(R.string.best_format, number.format(summary.bestSteps), shortDate)
                )
            }
            addRow(
                R.string.stat_goal,
                getString(R.string.goal_days_format, summary.goalDays, summary.dayCount)
            )
        }

        binding.emptyText.visibility = if (summary.totalSteps == 0) View.VISIBLE else View.GONE
    }

    private fun addRow(labelRes: Int, value: String) {
        val row = layoutInflater.inflate(R.layout.item_stat_row, binding.summaryContainer, false)
        row.findViewById<TextView>(R.id.statLabel).setText(labelRes)
        row.findViewById<TextView>(R.id.statValue).text = value
        binding.summaryContainer.addView(row)
    }

    companion object {
        private const val KEY_MODE = "stats_mode"
        private const val KEY_ANCHOR = "stats_anchor"
    }
}
