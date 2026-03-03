package api.http

import domain.booking.BookingService
import domain.booking.repository.InMemoryBookingRepository
import domain.house.HouseService
import domain.house.repository.InMemoryHouseRepository
import domain.user.UsersService
import domain.user.repository.InMemoryUsersRepository

object HousesDataMem {
    val houseRepository = InMemoryHouseRepository
    val bookingRepository = InMemoryBookingRepository
    val usersRepository = InMemoryUsersRepository

    val services = HousesServices(
        houseService = HouseService(houseRepository),
        bookingService = BookingService(bookingRepository, houseRepository),
        usersService = UsersService(usersRepository),
    )
}
