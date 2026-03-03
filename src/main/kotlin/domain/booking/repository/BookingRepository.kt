package domain.booking.repository

import domain.booking.Booking

interface BookingRepository {
    fun create(booking: Booking): Booking
    fun getById(id: String): Booking?
    fun getAll(): List<Booking>
    fun delete(booking: Booking): Booking?
}
