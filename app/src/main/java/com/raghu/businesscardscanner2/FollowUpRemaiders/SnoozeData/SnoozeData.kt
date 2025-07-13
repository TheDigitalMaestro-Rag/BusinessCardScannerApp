package com.raghu.businesscardscanner2.FollowUpRemaiders.SnoozeData

data class SnoozeOption(
    val label: String,
    val minutes: Long,
    val isCustom: Boolean = false
)