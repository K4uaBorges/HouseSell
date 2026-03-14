package utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object BookingDateUtils {
    private val postgresFormatter = DateTimeFormatter.BASIC_ISO_DATE
    private val postgresDateRegex = Regex("^\\d{8}$")

    fun parsePostgresDate(raw: String): LocalDate {
        val normalized = raw.trim()
        require(postgresDateRegex.matches(normalized)) {
            "Date must use PostgreSQL date format YYYYMMDD."
        }

        return try {
            LocalDate.parse(normalized, postgresFormatter)
        } catch (_: DateTimeParseException) {
            throw IllegalArgumentException("Date must be a valid calendar date in YYYYMMDD format.")
        }
    }

    fun formatPostgresDate(date: LocalDate): String = date.format(postgresFormatter)

    fun includes(startDate: LocalDate, endDate: LocalDate, date: LocalDate): Boolean {
        return !date.isBefore(startDate) && date.isBefore(endDate)
    }

    fun overlaps(
        firstStart: LocalDate,
        firstEnd: LocalDate,
        secondStart: LocalDate,
        secondEnd: LocalDate,
    ): Boolean {
        return firstStart < secondEnd && secondStart < firstEnd
    }
}
