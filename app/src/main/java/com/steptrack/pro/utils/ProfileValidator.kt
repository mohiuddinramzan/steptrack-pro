package com.steptrack.pro.utils

/** Parses and range-checks text typed by the user. Returns null when the text is not acceptable. */
object ProfileValidator {

    const val MIN_HEIGHT_CM = 100
    const val MAX_HEIGHT_CM = 250
    const val MIN_WEIGHT_KG = 20.0
    const val MAX_WEIGHT_KG = 300.0
    const val MIN_GOAL = 100
    const val MAX_GOAL = 100_000

    fun parseHeight(text: String): Int? =
        text.trim().toIntOrNull()?.takeIf { it in MIN_HEIGHT_CM..MAX_HEIGHT_CM }

    fun parseWeight(text: String): Double? =
        text.trim().replace(',', '.').toDoubleOrNull()
            ?.takeIf { it.isFinite() && it in MIN_WEIGHT_KG..MAX_WEIGHT_KG }

    fun parseGoal(text: String): Int? =
        text.trim().toIntOrNull()?.takeIf { it in MIN_GOAL..MAX_GOAL }
}
