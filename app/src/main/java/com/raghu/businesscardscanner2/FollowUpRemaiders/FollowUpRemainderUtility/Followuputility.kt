package com.raghu.businesscardscanner2.FollowUpRemaiders.FollowUpRemainderUtility

import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.google.android.gms.common.api.GoogleApi

object AlarmPermissionHelper {
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            // Permission not required before Android 12
            true
        }
    }

    fun requestExactAlarmPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                activity.startActivity(intent)
            } catch (e: Exception) {
                // Fallback if the intent can't be launched
                Toast.makeText(
                    activity,
                    "Please enable exact alarms in app settings",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}