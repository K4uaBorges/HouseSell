package main.utils

import java.sql.Date

object BookingDateUtils {
    private val postgresDateRegex = Regex("^\\d{4}\\-(0[1-9]|1[012])\\-(0[1-9]|[12][0-9]|3[01])\$")

    fun parsePostgresDate(raw: String): Date {
        val normalized = raw.trim()
        require(postgresDateRegex.matches(normalized)) {
            "Date must use PostgreSQL date format YYYY-MM-DD."
        }

        return try {
            Date.valueOf(normalized)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid date format: ${e.message}", e)
        }
    }

    fun formatPostgresDate(date: Date): String = date.toString()

    fun includes(
        startDate: Date,
        endDate: Date,
        date: Date,
    ): Boolean = !date.before(startDate) && date.before(endDate)

    fun overlaps(
        firstStart: Date,
        firstEnd: Date,
        secondStart: Date,
        secondEnd: Date,
    ): Boolean = firstStart < secondEnd && secondStart < firstEnd
}
