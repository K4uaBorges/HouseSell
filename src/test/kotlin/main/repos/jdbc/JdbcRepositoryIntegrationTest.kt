package main.repos.jdbc

import main.data.impl.caches.HouseInfoCache
import main.data.impl.jdbc.JdbcBookingRepository
import main.data.impl.jdbc.JdbcHouseRepository
import main.data.impl.jdbc.JdbcLocationRepository
import main.data.impl.jdbc.JdbcUsersRepository
import main.domain.booking.Booking
import main.domain.booking.Date
import main.domain.house.House
import main.domain.house.Title
import main.domain.location.Location
import main.domain.location.LocationName
import main.domain.location.LocationType
import main.domain.user.Email
import main.domain.user.Name
import main.domain.user.User
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class JdbcRepositoryIntegrationTest : PostgresTestContainer() {
    private val userRepo by lazy { JdbcUsersRepository(dataSource) }
    private val locationRepo by lazy { JdbcLocationRepository(dataSource) }
    private val houseRepo by lazy { JdbcHouseRepository(dataSource, HouseInfoCache(limit = 100)) }
    private val bookingRepo by lazy { JdbcBookingRepository(dataSource) }

    @Test
    fun `complete workflow - create user location house and booking`() {
        // 1. Criar usuário proprietário
        val owner =
            User(
                id = Uuid.random(),
                name = Name.of("John Owner"),
                email = Email.of("owner@test.com"),
                token = Uuid.random(),
            )
        userRepo.create(owner)

        // 2. Criar hierarquia de localização
        val country =
            locationRepo.create(
                Location(
                    id = Uuid.random(),
                    name = LocationName.of("Portugal"),
                    type = LocationType.COUNTRY,
                    parentId = null,
                ),
            )

        val region =
            locationRepo.create(
                Location(
                    id = Uuid.random(),
                    name = LocationName.of("Norte"),
                    type = LocationType.REGION,
                    parentId = country.id,
                ),
            )

        val district =
            locationRepo.create(
                Location(
                    id = Uuid.random(),
                    name = LocationName.of("Lisboa"),
                    type = LocationType.DISTRICT,
                    parentId = region.id,
                ),
            )

        // 3. Criar casa
        val house =
            houseRepo.create(
                House(
                    id = Uuid.random(),
                    uid = owner.id,
                    title = Title.of("Casa em Lisboa"),
                    lid = district.id,
                    areaSqMt = 120,
                    pricePerNight = 150.0,
                    description = "Linda casa no centro",
                ),
            )

        // 4. Criar usuário locatário
        val renter =
            User(
                id = Uuid.random(),
                name = Name.of("Jane Renter"),
                email = Email.of("renter@test.com"),
                token = Uuid.random(),
            )
        userRepo.create(renter)

        // 5. Criar booking
        val booking =
            bookingRepo.create(
                Booking(
                    id = Uuid.random(),
                    hid = house.id,
                    uid = renter.id,
                    startDate = Date.of("2024-07-01"),
                    endDate = Date.of("2024-07-07"),
                ),
            )

        // Verificações
        assertEquals(owner.id, houseRepo.getById(house.id).uid)
        assertEquals(district.id, houseRepo.getById(house.id).lid)
        assertEquals(house.id, bookingRepo.getById(booking.id).hid)
        assertEquals(renter.id, bookingRepo.getById(booking.id).uid)

        // Verificar path da localização
        val path = locationRepo.getFullPath(district.id)
        assertEquals(3, path.size)
        assertEquals("Portugal", path[0].name.value)
        assertEquals("Norte", path[1].name.value)
        assertEquals("Lisboa", path[2].name.value)
    }

    @Test
    fun `cascade delete - removing user should remove their houses and bookings`() {
        // Setup
        val owner =
            userRepo.create(
                User(
                    id = Uuid.random(),
                    name = Name.of("Owner"),
                    email = Email.of("owner@test.com"),
                    token = Uuid.random(),
                ),
            )

        val location =
            locationRepo.create(
                Location(
                    id = Uuid.random(),
                    name = LocationName.of("Portugal"),
                    type = LocationType.COUNTRY,
                    parentId = null,
                ),
            )

        // Verificar que tudo existe
        assertEquals(2, userRepo.getAll().size)
        assertEquals(1, houseRepo.getAll().size)
        assertEquals(1, bookingRepo.getAll().size)

        // Deletar owner
        userRepo.deleteById(owner.id)

        // Verificar que owner foi removido
        assertEquals(1, userRepo.getAll().size) // só sobrou renter

        // House deve ter sido removido (CASCADE)
        assertEquals(0, houseRepo.getAll().size)

        // Booking deve ter sido removido (CASCADE de house)
        assertEquals(0, bookingRepo.getAll().size)
    }

    @Test
    fun `location hierarchy - complex nested structure`() {
        // Criar hierarquia completa
        val portugal =
            locationRepo.create(
                Location(
                    id = Uuid.random(),
                    name = LocationName.of("Portugal"),
                    type = LocationType.COUNTRY,
                    parentId = null,
                ),
            )

        val norte =
            locationRepo.create(
                Location(
                    id = Uuid.random(),
                    name = LocationName.of("Norte"),
                    type = LocationType.REGION,
                    parentId = portugal.id,
                ),
            )

        val porto =
            locationRepo.create(
                Location(
                    id = Uuid.random(),
                    name = LocationName.of("Porto"),
                    type = LocationType.DISTRICT,
                    parentId = norte.id,
                ),
            )

        val maia =
            locationRepo.create(
                Location(
                    id = Uuid.random(),
                    name = LocationName.of("Maia"),
                    type = LocationType.MUNICIPALITY,
                    parentId = porto.id,
                ),
            )

        val vilaNova =
            locationRepo.create(
                Location(
                    id = Uuid.random(),
                    name = LocationName.of("Vila Nova"),
                    type = LocationType.LOCALITY,
                    parentId = maia.id,
                ),
            )

        // Verificar path completo
        val fullPath = locationRepo.getFullPath(vilaNova.id)
        assertEquals(5, fullPath.size)
        assertEquals("Portugal", fullPath[0].name.value)
        assertEquals("Norte", fullPath[1].name.value)
        assertEquals("Porto", fullPath[2].name.value)
        assertEquals("Maia", fullPath[3].name.value)
        assertEquals("Vila Nova", fullPath[4].name.value)

        // Verificar children em cada nível
        assertEquals(4, locationRepo.getChildrenAll(portugal.id).size) // Norte
        assertEquals(3, locationRepo.getChildrenAll(norte.id).size) // Porto
        assertEquals(2, locationRepo.getChildrenAll(porto.id).size) // Maia
        assertEquals(1, locationRepo.getChildrenAll(maia.id).size) // Vila Nova
        assertEquals(0, locationRepo.getChildrenAll(vilaNova.id).size) // folha
    }

    @Test
    fun `concurrent bookings - multiple users booking different houses`() {
        val owner =
            userRepo.create(
                User(
                    id = Uuid.random(),
                    name = Name.of("Owner"),
                    email = Email.of("owner@test.com"),
                    token = Uuid.random(),
                ),
            )

        val location =
            locationRepo.create(
                Location(
                    id = Uuid.random(),
                    name = LocationName.of("Portugal"),
                    type = LocationType.COUNTRY,
                    parentId = null,
                ),
            )

        // Criar 3 casas
        val houses =
            (1..3).map { i ->
                houseRepo.create(
                    House(
                        id = Uuid.random(),
                        uid = owner.id,
                        title = Title.of("Casa $i"),
                        lid = location.id,
                        areaSqMt = 100 * i,
                        pricePerNight = 100.0 * i,
                        description = "Casa número $i",
                    ),
                )
            }

        // Criar 3 usuários, cada um booka uma casa
        val renters =
            (1..3).map { i ->
                val user =
                    userRepo.create(
                        User(
                            id = Uuid.random(),
                            name = Name.of("Renter $i"),
                            email = Email.of("renter$i@test.com"),
                            token = Uuid.random(),
                        ),
                    )

                bookingRepo.create(
                    Booking(
                        id = Uuid.random(),
                        hid = houses[i - 1].id,
                        uid = user.id,
                        startDate = Date.of("2024-0$i-01"),
                        endDate = Date.of("2024-0$i-07"),
                    ),
                )
                user
            }

        // Verificações
        assertEquals(4, userRepo.getAll().size) // owner + 3 renters
        assertEquals(3, houseRepo.getAll().size)
        assertEquals(3, bookingRepo.getAll().size)

        // Cada booking deve estar associado à casa correta
        renters.forEachIndexed { index, renter ->
            val booking = bookingRepo.getAll().find { it.uid == renter.id }!!
            assertEquals(houses[index].id, booking.hid)
        }
    }
}
