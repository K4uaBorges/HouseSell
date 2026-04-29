package main.data.interfaces

import main.domain_model.booking.Booking
import main.data.interfaces.Repository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface BookingRepository : Repository<Uuid, Booking> {
    override fun create(value: Booking): Booking

    override fun getById(key: Uuid): Booking

    fun getByUserId(uid: Uuid): List<Booking>

    override fun save(value: Booking): Booking

    override fun getAll(): List<Booking>

    override fun deleteById(key: Uuid)

    override fun clear()

    override fun update(updated: Booking): Booking
}
