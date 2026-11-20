package com.steptrack.pro.data

/** Defaults are used until the Profile screen (Phase 7) lets the user change them. */
data class UserProfile(
    val heightCm: Int = 170,
    val weightKg: Double = 70.0,
    val stepGoal: Int = 10_000
)
