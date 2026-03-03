package domain.booking

import java.time.LocalDate
import utils.BookingDateUtils
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class Booking(
    val id: Uuid,
    val hid: String,
    val uid: Uuid,
    val startDate: Date,
    val endDate: Date,
)

@JvmInline
value class Date private constructor(val value: LocalDate) : Comparable<Date> {
    companion object {
        fun of(raw: String): Date = Date(BookingDateUtils.parsePostgresDate(raw))
        fun from(value: LocalDate): Date = Date(value)
    }

    override fun compareTo(other: Date): Int = value.compareTo(other.value)

    override fun toString(): String = BookingDateUtils.formatPostgresDate(value)
}
