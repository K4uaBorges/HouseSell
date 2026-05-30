package main.domain.house

import main.api.httpServer.HousesDataMem
import main.api.httpServer.HousesWebApi
import main.data.impl.mem.InMemoryBookingRepository
import main.data.impl.mem.InMemoryHouseRepository
import main.data.impl.mem.InMemoryLocationRepository
import main.data.impl.mem.InMemoryUsersRepository
import main.domain.user.Email
import main.domain.user.Name
import main.domain.user.User
import main.domain.user.UserRole
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class HouseApiTest {
    private val api = HousesWebApi(HousesDataMem.services)
    private lateinit var adminToken: String

    @BeforeTest
    fun setup() {
        InMemoryUsersRepository.clear()
        InMemoryHouseRepository.clear()
        InMemoryBookingRepository.clear()
        InMemoryLocationRepository.clear()
        adminToken = seedAuthUserToken().toString()
    }

    @Test
    fun `POST houses creates house`() {
        val token = createUserAndGetToken("owner@example.com")
        val locationId = createLeafLocationAndGetId()
        val response =
            api.routes(
                Request(Method.POST, "/houses")
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .body(
                        """
                        {
                            "title":"Casa Azul",
                            "lid":"$locationId",
                            "areaSqMt":120,
                            "pricePerNight":95.0,
                            "description":"Casa perto da praia"
                        }
                        """.trimIndent(),
                    ),
            )

        assertEquals(Status.CREATED, response.status)
        assertTrue(response.bodyString().contains("Casa Azul"))
    }

    @Test
    fun `POST houses rejects duplicate title for same owner`() {
        val token = createUserAndGetToken("owner-duplicate-title@example.com")
        val locationId = createLeafLocationAndGetId()

        api.routes(
            Request(Method.POST, "/houses")
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .body(
                    """
                    {
                        "title":"Casa Azul",
                        "lid":"$locationId",
                        "areaSqMt":120,
                        "pricePerNight":95.0,
                        "description":"Casa perto da praia"
                    }
                    """.trimIndent(),
                ),
        )

        val response =
            api.routes(
                Request(Method.POST, "/houses")
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .body(
                        """
                        {
                            "title":"  casa azul  ",
                            "lid":"$locationId",
                            "areaSqMt":130,
                            "pricePerNight":105.0,
                            "description":"Outra descrição"
                        }
                        """.trimIndent(),
                    ),
            )

        assertEquals(Status.BAD_REQUEST, response.status)
        assertTrue(response.bodyString().contains("Já existe uma house tua com esse título."))
    }

    @Test
    fun `POST houses rejects duplicate description for same owner`() {
        val token = createUserAndGetToken("owner-duplicate-description@example.com")
        val locationId = createLeafLocationAndGetId()

        api.routes(
            Request(Method.POST, "/houses")
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .body(
                    """
                    {
                        "title":"Casa Azul",
                        "lid":"$locationId",
                        "areaSqMt":120,
                        "pricePerNight":95.0,
                        "description":"Descrição repetida"
                    }
                    """.trimIndent(),
                ),
        )

        val response =
            api.routes(
                Request(Method.POST, "/houses")
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .body(
                        """
                        {
                            "title":"Casa Verde",
                            "lid":"$locationId",
                            "areaSqMt":130,
                            "pricePerNight":105.0,
                            "description":"   descrição    repetida "
                        }
                        """.trimIndent(),
                    ),
            )

        assertEquals(Status.BAD_REQUEST, response.status)
        assertTrue(response.bodyString().contains("Já existe uma house tua com essa descrição."))
    }

    @Test
    fun `GET houses returns list`() {
        val token = createUserAndGetToken("owner@example.com")
        val locationId = createLeafLocationAndGetId()
        api.routes(
            Request(Method.POST, "/houses")
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .body(
                    """
                    {
                        "title":"Casa Azul",
                        "lid":"$locationId",
                        "areaSqMt":120,
                        "pricePerNight":95.0,
                        "description":"Casa perto da praia"
                    }
                    """.trimIndent(),
                ),
        )

        val response = api.routes(Request(Method.GET, "/houses"))

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("Casa Azul"))
    }

    @Test
    fun `GET house with invalid id returns 400`() {
        val response = api.routes(Request(Method.GET, "/houses/invalid-uuid"))

        assertEquals(Status.BAD_REQUEST, response.status)
    }

    @Test
    fun `GET house with non existent id returns 404`() {
        val response = api.routes(Request(Method.GET, "/houses/${Uuid.random()}"))

        assertEquals(Status.NOT_FOUND, response.status)
    }

    @Test
    fun `GET houses mine without authorization returns 401`() {
        val response = api.routes(Request(Method.GET, "/houses/mine"))

        assertEquals(Status.UNAUTHORIZED, response.status)
    }

    @Test
    fun `GET houses mine returns only owner houses`() {
        val ownerToken = createUserAndGetToken("owner@example.com")
        val otherToken = createUserAndGetToken("other@example.com")
        val locationId = createLeafLocationAndGetId()

        api.routes(
            Request(Method.POST, "/houses")
                .header("Authorization", "Bearer $ownerToken")
                .header("Content-Type", "application/json")
                .body(
                    """
                    {
                        "title":"Owner House",
                        "lid":"$locationId",
                        "areaSqMt":120,
                        "pricePerNight":95.0,
                        "description":"Casa owner"
                    }
                    """.trimIndent(),
                ),
        )
        api.routes(
            Request(Method.POST, "/houses")
                .header("Authorization", "Bearer $otherToken")
                .header("Content-Type", "application/json")
                .body(
                    """
                    {
                        "title":"Other House",
                        "lid":"$locationId",
                        "areaSqMt":90,
                        "pricePerNight":70.0,
                        "description":"Casa other"
                    }
                    """.trimIndent(),
                ),
        )

        val response = api.routes(Request(Method.GET, "/houses/mine").header("Authorization", "Bearer $ownerToken"))

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("Owner House"))
        assertTrue(!response.bodyString().contains("Other House"))
    }

    @Test
    fun `PUT house updates house when authorized`() {
        val token = createUserAndGetToken("owner@example.com")
        val houseId = createHouseAndGetId(token, "Casa Azul")
        val newLocationId = createLeafLocationAndGetId()

        val response =
            api.routes(
                Request(Method.PUT, "/houses/$houseId")
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .body(
                        """
                        {
                            "title":"Casa Verde",
                            "lid":"$newLocationId",
                            "areaSqMt":130,
                            "pricePerNight":100.0,
                            "description":"Atualizada"
                        }
                        """.trimIndent(),
                    ),
            )

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("Casa Verde"))
    }

    @Test
    fun `DELETE house removes house when authorized`() {
        val token = createUserAndGetToken("owner@example.com")
        val houseId = createHouseAndGetId(token, "Casa para remover")

        val deleteResponse =
            api.routes(
                Request(Method.DELETE, "/houses/$houseId")
                    .header("Authorization", "Bearer $token"),
            )
        val getResponse = api.routes(Request(Method.GET, "/houses/$houseId"))

        assertEquals(Status.OK, deleteResponse.status)
        assertEquals(Status.NOT_FOUND, getResponse.status)
    }

    @Test
    fun `DELETE house with mismatched body id returns 400`() {
        val token = createUserAndGetToken("owner@example.com")
        val houseId = createHouseAndGetId(token, "Casa body mismatch")

        val response =
            api.routes(
                Request(Method.DELETE, "/houses/$houseId")
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .body("""{"id":"${Uuid.random()}"}"""),
            )

        assertEquals(Status.BAD_REQUEST, response.status)
    }

    @Test
    fun `GET houses available without required dates returns 400`() {
        val response = api.routes(Request(Method.GET, "/houses/available"))

        assertEquals(Status.BAD_REQUEST, response.status)
    }

    @Test
    fun `GET houses available returns only free house when another is booked`() {
        val token = createUserAndGetToken("availability-owner@example.com")
        val locationSeed = Uuid.random().toString().take(8)
        val countryId = createLocationAndGetId(token, "PT-$locationSeed", "COUNTRY")
        val regionId = createLocationAndGetId(token, "Reg-$locationSeed", "REGION", countryId)
        val districtId = createLocationAndGetId(token, "Dis-$locationSeed", "DISTRICT", regionId)
        val municipalityId = createLocationAndGetId(token, "Mun-$locationSeed", "MUNICIPALITY", districtId)
        val locationId = createLocationAndGetId(token, "Loc-$locationSeed", "LOCALITY", municipalityId)

        val freeHouseId = createHouseAndGetId(token, "Casa Livre", locationId)
        val bookedHouseId = createHouseAndGetId(token, "Casa Ocupada", locationId)

        val bookingResponse =
            api.routes(
                Request(Method.POST, "/bookings")
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .body(
                        """
                        {
                            "hid":"$bookedHouseId",
                            "startDate":"2026-06-10",
                            "endDate":"2026-06-15"
                        }
                        """.trimIndent(),
                    ),
            )
        assertEquals(Status.CREATED, bookingResponse.status)

        val availableResponse =
            api.routes(
                Request(Method.GET, "/houses/available?startDate=2026-06-11&endDate=2026-06-12"),
            )

        assertEquals(Status.OK, availableResponse.status)
        assertTrue(availableResponse.bodyString().contains(freeHouseId))
        assertTrue(availableResponse.bodyString().contains("Casa Livre"))
        assertTrue(!availableResponse.bodyString().contains(bookedHouseId))
        assertTrue(!availableResponse.bodyString().contains("Casa Ocupada"))
    }

    @Test
    fun `POST house without authorization returns 401`() {
        val response =
            api.routes(
                Request(Method.POST, "/houses")
                    .header("Content-Type", "application/json")
                    .body(
                        """
                        {
                            "title":"Casa Azul",
                            "lid":"${Uuid.random()}",
                            "areaSqMt":120,
                            "pricePerNight":95.0,
                            "description":"Casa perto da praia"
                        }
                        """.trimIndent(),
                    ),
            )

        assertEquals(Status.UNAUTHORIZED, response.status)
    }

    @Test
    fun `GET house available days returns available days for month`() {
        val token = createUserAndGetToken("available-days-owner@example.com")
        val houseId = createHouseAndGetId(token, "Casa Calendario")

        api.routes(
            Request(Method.POST, "/bookings")
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .body("""{"hid":"$houseId","startDate":"2026-06-10","endDate":"2026-06-15"}"""),
        )

        val response = api.routes(Request(Method.GET, "/houses/$houseId/available-days?year=2026&month=6"))
        val body = response.bodyString()

        assertEquals(Status.OK, response.status)
        assertTrue(body.contains("\"houseId\":\"$houseId\""))
        assertTrue(body.contains("\"year\":2026"))
        assertTrue(body.contains("\"month\":6"))
        assertTrue(!body.contains("2026-06-10"))
        assertTrue(!body.contains("2026-06-14"))
        assertTrue(body.contains("2026-06-15"))
    }

    @Test
    fun `GET house available days without required query params returns 400`() {
        val token = createUserAndGetToken("available-days-missing@example.com")
        val houseId = createHouseAndGetId(token, "Casa Missing Query")

        val response = api.routes(Request(Method.GET, "/houses/$houseId/available-days?year=2026"))

        assertEquals(Status.BAD_REQUEST, response.status)
    }

    @Test
    fun `GET house available days for non existent house returns 404`() {
        val response = api.routes(Request(Method.GET, "/houses/${Uuid.random()}/available-days?year=2026&month=6"))

        assertEquals(Status.NOT_FOUND, response.status)
    }

    private fun createUserAndGetToken(email: String): String {
        val response =
            api.routes(
                Request(Method.POST, "/users")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body(
                        """
                        {
                            "name":"Owner",
                            "email":"$email",
                            "password":"Secret123"
                        }
                        """.trimIndent(),
                    ),
            )

        return extractField(response.bodyString(), "token")
    }

    private fun createHouseAndGetId(
        token: String,
        title: String,
        lid: String = createLeafLocationAndGetId(),
    ): String {
        val response =
            api.routes(
                Request(Method.POST, "/houses")
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .body(
                        """
                        {
                            "title":"$title",
                            "lid":"$lid",
                            "areaSqMt":120,
                            "pricePerNight":95.0,
                            "description":"Casa teste"
                        }
                        """.trimIndent(),
                    ),
            )
        return extractField(response.bodyString(), "id")
    }

    private fun createLocationAndGetId(
        _token: String,
        name: String,
        type: String,
        parentId: String? = null,
    ): String {
        val parentField = parentId?.let { "\"$it\"" } ?: "null"
        val payload =
            """
            {
                "name":"$name",
                "type":"$type",
                "parentId":$parentField
            }
            """.trimIndent()
        val response =
            api.routes(
                Request(Method.POST, "/locations")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body(payload),
            )
        assertEquals(Status.CREATED, response.status, response.bodyString())
        return extractField(response.bodyString(), "id")
    }

    private fun createLeafLocationAndGetId(): String {
        val seed = Uuid.random().toString().take(8)
        val countryId = createLocationAndGetId(adminToken, "PT-$seed", "COUNTRY")
        val regionId = createLocationAndGetId(adminToken, "Reg-$seed", "REGION", countryId)
        val districtId = createLocationAndGetId(adminToken, "Dis-$seed", "DISTRICT", regionId)
        val municipalityId = createLocationAndGetId(adminToken, "Mun-$seed", "MUNICIPALITY", districtId)
        return createLocationAndGetId(adminToken, "Loc-$seed", "LOCALITY", municipalityId)
    }

    private fun extractField(
        json: String,
        field: String,
    ): String =
        "\"$field\":\"([^\"]+)\""
            .toRegex()
            .find(json)
            ?.groupValues
            ?.get(1)
            ?: error("Could not extract $field from response")

    private fun seedAuthUserToken(): Uuid {
        val authUser =
            User(
                id = Uuid.random(),
                name = Name.of("Zulu Admin"),
                email = Email.of("zulu-admin@example.com"),
                token = Uuid.random(),
                role = UserRole.ADMIN,
            )
        InMemoryUsersRepository.create(authUser)
        return authUser.token
    }
}
