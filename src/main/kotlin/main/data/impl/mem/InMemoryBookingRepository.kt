package main.data.impl.mem

import main.data.interfaces.BookingRepository
import main.domain_model.booking.Booking
import main.errors.NoBookingExist
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object InMemoryBookingRepository : BookingRepository {
    private val bookingsById = mutableMapOf<Uuid, Booking>()

    override fun create(value: Booking): Booking {
        bookingsById[value.id] = value
        return value
    }

    override fun save(value: Booking): Booking {
        bookingsById[value.id] ?: throw NoBookingExist("Booking not found.")
        bookingsById[value.id] = value
        return value
    }

    override fun update(updated: Booking): Booking =
        save(updated)

    override fun getById(key: Uuid): Booking = bookingsById[key] ?: throw NoBookingExist("Booking not found.")

    override fun getAll(): List<Booking> = bookingsById.values.toList()

    override fun deleteById(key: Uuid) {
        bookingsById.remove(key) ?: throw NoBookingExist("Booking not found.")
    }

    override fun clear() = bookingsById.clear()
}
