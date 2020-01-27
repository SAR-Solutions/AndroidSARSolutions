package com.sarcoordinator.sarsolutions.util

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

// Singleton class to access common methods
object GlobalUtil {

    fun convertEpochToDate(epochDate: Long): String {
        return SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            .format(Date(Timestamp(epochDate * 1000).time))
    }
}