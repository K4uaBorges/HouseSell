package api.http

import domain.booking.BookingService
import domain.booking.repository.InMemoryBookingRepository
import domain.booking.repository.JdbcBookingRepository
import domain.house.HouseService
import domain.house.repository.InMemoryHouseRepository
import domain.house.repository.JdbcHouseRepository
import domain.user.UsersService
import domain.user.repository.InMemoryUsersRepository
import domain.user.repository.JdbcUsersRepository
import org.postgresql.ds.PGSimpleDataSource


object HousesDataMem {
    private val houseRepository = InMemoryHouseRepository
    private val bookingRepository = InMemoryBookingRepository
    private val usersRepository = InMemoryUsersRepository

    val services = HousesServices(
        houseService = HouseService(houseRepository),
        bookingService = BookingService(bookingRepository, houseRepository),
        usersService = UsersService(usersRepository),
    )

    fun servicesFromDatabase(jdbcDatabaseUrl: String): HousesServices {
        val dataSource = PGSimpleDataSource().apply {
            setURL(jdbcDatabaseUrl)
        }

        val houseRepository = JdbcHouseRepository(dataSource)
        val bookingRepository = JdbcBookingRepository(dataSource)
        val usersRepository = JdbcUsersRepository(dataSource)

        return HousesServices(
            houseService = HouseService(houseRepository),
            bookingService = BookingService(bookingRepository, houseRepository),
            usersService = UsersService(usersRepository),
        )
    }

    fun services(jdbcDatabaseUrl: String?): HousesServices {
        return if (jdbcDatabaseUrl.isNullOrBlank()) services else servicesFromDatabase(jdbcDatabaseUrl)
    }
}
