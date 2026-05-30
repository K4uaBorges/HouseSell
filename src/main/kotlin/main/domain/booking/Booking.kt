package main.domain.booking

import main.utils.BookingDateUtils
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class Booking(
    val id: Uuid,
    val hid: Uuid,
    val uid: Uuid,
    val startDate: Date,
    val endDate: Date,
)

@JvmInline
value class Date private constructor(
    val value: java.sql.Date,
) : Comparable<Date> {
    companion object {
        fun of(raw: String): Date = Date(BookingDateUtils.parsePostgresDate(raw))

        fun from(value: java.sql.Date): Date = Date(value)
    }

    override fun compareTo(other: Date): Int = value.compareTo(other.value)

    override fun toString(): String = BookingDateUtils.formatPostgresDate(value)
}
