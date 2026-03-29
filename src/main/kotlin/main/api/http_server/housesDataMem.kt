package main.api.http_server

import org.postgresql.ds.PGSimpleDataSource
import main.data.impl.jdbc.JdbcBookingRepository
import main.data.impl.jdbc.JdbcHouseRepository
import main.data.impl.jdbc.JdbcLocationRepository
import main.data.impl.jdbc.JdbcUsersRepository
import main.data.impl.mem.InMemoryBookingRepository
import main.data.impl.mem.InMemoryHouseRepository
import main.data.impl.mem.InMemoryLocationRepository
import main.data.impl.mem.InMemoryUsersRepository
import main.domain_model.booking.BookingService
import main.domain_model.house.HouseService
import main.domain_model.location.LocationService
import main.domain_model.user.UsersService

object HousesDataMem {
    private val houseRepository = InMemoryHouseRepository
    private val bookingRepository = InMemoryBookingRepository
    private val usersRepository = InMemoryUsersRepository
    private val locationRepository = InMemoryLocationRepository

    val services =
        HousesServices(
            houseService = HouseService(houseRepository),
            bookingService =
                BookingService(
                    bookingRepository,
                    houseRepository,
                ),
            usersService = UsersService(usersRepository),
            locationService = LocationService(locationRepository),
        )

    fun servicesFromDatabase(jdbcDatabaseUrl: String): HousesServices {
        val dataSource =
            PGSimpleDataSource().apply {
                setURL(jdbcDatabaseUrl)
            }

        val houseRepository = JdbcHouseRepository(dataSource)
        val bookingRepository = JdbcBookingRepository(dataSource)
        val usersRepository = JdbcUsersRepository(dataSource)
        val locationRepository = JdbcLocationRepository(dataSource)

        return HousesServices(
            houseService = HouseService(houseRepository),
            bookingService = BookingService(
                bookingRepository,
                houseRepository,
            ),
            usersService = UsersService(usersRepository),
            locationService = LocationService(locationRepository),
        )
    }

    fun services(jdbcDatabaseUrl: String?): HousesServices {
        return if (jdbcDatabaseUrl.isNullOrBlank()) services else servicesFromDatabase(jdbcDatabaseUrl)
    }
}
