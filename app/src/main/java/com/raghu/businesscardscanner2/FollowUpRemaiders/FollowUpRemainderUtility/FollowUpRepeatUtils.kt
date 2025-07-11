package com.raghu.businesscardscanner2.FollowUpRemaiders.FollowUpRemainderUtility


import java.util.Calendar

object FollowUpRepeatUtils {

    fun calculateNextTriggerTime(currentTime: Long, repeatType: String): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentTime }

        when (repeatType) {
            "Daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "Monthly" -> calendar.add(Calendar.MONTH, 1)
            else -> return -1L // No repeat
        }

        return calendar.timeInMillis
    }
}
