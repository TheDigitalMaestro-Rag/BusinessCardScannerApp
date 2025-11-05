// CalendarIntegrationService.kt
package com.project.businesscardscannerapp.Calendar

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.TimeZone

class CalendarIntegrationService(private val context: Context) {

    companion object {
        const val CALENDAR_PERMISSION_REQUEST_CODE = 1001
        private const val CALENDAR_WRITE_PERMISSION = Manifest.permission.WRITE_CALENDAR
    }

    fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, CALENDAR_WRITE_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun requestCalendarPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(CALENDAR_WRITE_PERMISSION),
            CALENDAR_PERMISSION_REQUEST_CODE
        )
    }

    fun addFollowUpToCalendar(
        cardName: String,
        company: String?,
        followUpTime: Long,
        notes: String? = null,
        durationMinutes: Int = 30
    ): Boolean {
        if (!hasCalendarPermission()) {
            Toast.makeText(context, "Calendar permission required", Toast.LENGTH_SHORT).show()
            return false
        }

        return try {
            val startTime = followUpTime
            val endTime = startTime + (durationMinutes * 60 * 1000)

            val values = ContentValues().apply {
                put(CalendarContract.Events.TITLE, "Follow-up: $cardName${company?.let { " - $it" } ?: ""}")
                put(CalendarContract.Events.DESCRIPTION,
                    notes ?: "Business card follow-up reminder${company?.let { " from $it" } ?: ""}")
                put(CalendarContract.Events.DTSTART, startTime)
                put(CalendarContract.Events.DTEND, endTime)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.CALENDAR_ID, getDefaultCalendarId())
                put(CalendarContract.Events.EVENT_LOCATION, "")
                put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val success = uri != null

            if (success) {
                Toast.makeText(context, "Follow-up added to calendar", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to add to calendar", Toast.LENGTH_SHORT).show()
            }
            success

        } catch (e: SecurityException) {
            Toast.makeText(context, "Calendar access denied", Toast.LENGTH_SHORT).show()
            false
        } catch (e: Exception) {
            Toast.makeText(context, "Calendar error: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun getDefaultCalendarId(): Long {
        return try {
            val projection = arrayOf(CalendarContract.Calendars._ID)
            val selection = "${CalendarContract.Calendars.IS_PRIMARY} = 1"

            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getLong(0)
                } else {
                    // Fallback to first available calendar
                    getFirstAvailableCalendarId()
                }
            } ?: getFirstAvailableCalendarId()
        } catch (e: Exception) {
            getFirstAvailableCalendarId()
        }
    }

    private fun getFirstAvailableCalendarId(): Long {
        return try {
            val projection = arrayOf(CalendarContract.Calendars._ID)

            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getLong(0)
                } else {
                    1L // Default fallback
                }
            } ?: 1L
        } catch (e: Exception) {
            1L
        }
    }

    fun openCalendarApp(startTime: Long? = null) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = CalendarContract.CONTENT_URI
            if (startTime != null) {
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "No calendar app found", Toast.LENGTH_SHORT).show()
        }
    }
}