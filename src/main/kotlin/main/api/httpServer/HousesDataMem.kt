package main.api.httpServer

import main.data.impl.caches.HouseInfoCache
import main.data.impl.jdbc.DatabaseSchemaInitializer
import main.data.impl.jdbc.JdbcBookingRepository
import main.data.impl.jdbc.JdbcHouseRepository
import main.data.impl.jdbc.JdbcLocationRepository
import main.data.impl.jdbc.JdbcUsersRepository
import main.data.impl.mem.InMemoryBookingRepository
import main.data.impl.mem.InMemoryHouseRepository
import main.data.impl.mem.InMemoryLocationRepository
import main.data.impl.mem.InMemoryUsersRepository
import main.domain.booking.BookingService
import main.domain.house.HouseService
import main.domain.location.LocationService
import main.domain.user.UsersService
import org.postgresql.ds.PGSimpleDataSource

object HousesDataMem {
    private const val DEFAULT_HOUSE_CACHE_SIZE = 100

    private val houseRepository = InMemoryHouseRepository
    private val inMemoryHouseCache = HouseInfoCache(cacheLimit())
    private val bookingRepository = InMemoryBookingRepository
    private val usersRepository = InMemoryUsersRepository
    private val locationRepository = InMemoryLocationRepository

    val services =
        HousesServices(
            houseService = HouseService(houseRepository, inMemoryHouseCache),
            bookingService =
                BookingService(
                    bookingRepository,
                    houseRepository,
                ),
            usersService = UsersService(usersRepository),
            locationService = LocationService(locationRepository),
        )

    fun services(
        jdbcDatabaseUrl: String,
        databaseUser: String? = null,
        databasePass: String? = null,
    ): HousesServices {
        val dataSource =
            PGSimpleDataSource().apply {
                setURL(jdbcDatabaseUrl)
                databaseUser?.trim()?.takeIf { it.isNotEmpty() }?.let(::setUser)
                databasePass?.trim()?.takeIf { it.isNotEmpty() }?.let(::setPassword)
            }

        DatabaseSchemaInitializer.ensureSchema(dataSource)

        val houseCache = HouseInfoCache(cacheLimit())
        val houseRepository = JdbcHouseRepository(dataSource, houseCache)
        val bookingRepository = JdbcBookingRepository(dataSource)
        val usersRepository = JdbcUsersRepository(dataSource)
        val locationRepository = JdbcLocationRepository(dataSource)

        return HousesServices(
            houseService = HouseService(houseRepository, houseCache),
            bookingService =
                BookingService(
                    bookingRepository,
                    houseRepository,
                ),
            usersService = UsersService(usersRepository),
            locationService = LocationService(locationRepository),
        )
    }

    private fun cacheLimit(): Int =
        System.getenv("HOUSE_CACHE_SIZE")
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
            ?: DEFAULT_HOUSE_CACHE_SIZE
}
