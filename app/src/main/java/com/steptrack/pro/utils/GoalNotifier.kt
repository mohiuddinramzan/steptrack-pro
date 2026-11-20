package com.steptrack.pro.utils

object GoalNotifier {

    /** Notify once per day, only after the goal has been reached. */
    fun shouldNotify(goalCompleted: Boolean, lastNotifiedDate: String?, today: String): Boolean =
        goalCompleted && lastNotifiedDate != today
}
