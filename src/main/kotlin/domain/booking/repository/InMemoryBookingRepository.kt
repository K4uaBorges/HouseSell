package domain.booking.repository

import domain.booking.Booking
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object InMemoryBookingRepository : BookingRepository {
    private val bookingsById = mutableMapOf<String, Booking>()

    override fun create(booking: Booking): Booking {
        bookingsById[booking.id.toString()] = booking
        return booking
    }

    override fun getById(id: String): Booking? = bookingsById[id]

    override fun getAll(): List<Booking> = bookingsById.values.toList()

    override fun delete(booking: Booking) = bookingsById.remove(booking.id.toString())


    fun clear() = bookingsById.clear()
}
