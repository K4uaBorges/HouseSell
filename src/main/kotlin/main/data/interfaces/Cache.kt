package main.data.interfaces

import main.domain_model.booking.Booking

interface Cache<K, V> {
    fun getById(id: K): V?

    fun put(
        id: K,
        value: V,
    )

    fun clear()
}
