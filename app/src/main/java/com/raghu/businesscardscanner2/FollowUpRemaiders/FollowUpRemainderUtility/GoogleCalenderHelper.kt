package com.raghu.businesscardscanner2.FollowUpRemaiders.FollowUpRemainderUtility

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.raghu.businesscardscanner2.FollowUpRemaiders.FollowUpReminderEntity

object GoogleCalendarHelper {

    fun addEvent(context: Context, reminder: FollowUpReminderEntity) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, reminder.message)
            putExtra(CalendarContract.Events.DESCRIPTION, reminder.notes ?: "")
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, reminder.dueDate)
        }
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}