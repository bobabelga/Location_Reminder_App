package com.bobabelga.locationreminders.locationreminders.data

import com.bobabelga.locationreminders.locationreminders.data.dto.ReminderDTO
import com.bobabelga.locationreminders.locationreminders.data.dto.Result

/**
 * Main entry point for accessing reminders data.
 */
interface ReminderDataSource {
    suspend fun getReminders(): Result<List<ReminderDTO>>
    suspend fun saveReminder(reminder: ReminderDTO)
    suspend fun getReminder(id: String): Result<ReminderDTO>
    suspend fun deleteAllReminders()
}