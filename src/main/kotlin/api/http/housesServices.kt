package api.http

import domain.booking.BookingService
import domain.house.HouseService
import domain.user.UsersService

data class HousesServices(
    val houseService: HouseService,
    val bookingService: BookingService,
    val usersService: UsersService,
)
