package domain_model.house

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import main.api.http_server.HousesDataMem
import main.api.http_server.HousesWebApi
import main.data.impl.mem.InMemoryBookingRepository
import main.data.impl.mem.InMemoryHouseRepository
import main.data.impl.mem.InMemoryLocationRepository
import main.data.impl.mem.InMemoryUsersRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class HouseApiTest {
    private val api = HousesWebApi(HousesDataMem.services)

    @BeforeTest
    fun setup() {
        InMemoryUsersRepository.clear()
        InMemoryHouseRepository.clear()
        InMemoryBookingRepository.clear()
        InMemoryLocationRepository.clear()
    }

    @Test
    fun `POST houses creates house`() {
        val token = createUserAndGetToken("owner@example.com")
        val response =
            api.routes(
                Request(Method.POST, "/houses")
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .body(
                        """{"title":"Casa Azul","lid":"${Uuid.random()}","areaSqMt":120,"pricePerNight":95.0,"description":"Casa perto da praia"}""",
                    ),
            )

        assertEquals(Status.CREATED, response.status)
        assertTrue(response.bodyString().contains("Casa Azul"))
    }

    @Test
    fun `GET houses returns list`() {
        val token = createUserAndGetToken("owner@example.com")
        api.routes(
            Request(Method.POST, "/houses")
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .body(
                    """{"title":"Casa Azul","lid":"${Uuid.random()}","areaSqMt":120,"pricePerNight":95.0,"description":"Casa perto da praia"}""",
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

        api.routes(
            Request(Method.POST, "/houses")
                .header("Authorization", "Bearer $ownerToken")
                .header("Content-Type", "application/json")
                .body(
                    """{"title":"Owner House","lid":"${Uuid.random()}","areaSqMt":120,"pricePerNight":95.0,"description":"Casa owner"}""",
                ),
        )
        api.routes(
            Request(Method.POST, "/houses")
                .header("Authorization", "Bearer $otherToken")
                .header("Content-Type", "application/json")
                .body("""{"title":"Other House","lid":"${Uuid.random()}","areaSqMt":90,"pricePerNight":70.0,"description":"Casa other"}"""),
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

        val response =
            api.routes(
                Request(Method.PUT, "/houses/$houseId")
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .body(
                        """{"title":"Casa Verde","lid":"${Uuid.random()}","areaSqMt":130,"pricePerNight":100.0,"description":"Atualizada"}""",
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
    fun `POST house without authorization returns 401`() {
        val response =
            api.routes(
                Request(Method.POST, "/houses")
                    .header("Content-Type", "application/json")
                    .body(
                        """{"title":"Casa Azul","lid":"${Uuid.random()}","areaSqMt":120,"pricePerNight":95.0,"description":"Casa perto da praia"}""",
                    ),
            )

        assertEquals(Status.UNAUTHORIZED, response.status)
    }

    private fun createUserAndGetToken(email: String): String {
        val response =
            api.routes(
                Request(Method.POST, "/users")
                    .header("Content-Type", "application/json")
                    .body("""{"name":"Owner","email":"$email"}"""),
            )

        return extractField(response.bodyString(), "token")
    }

    private fun createHouseAndGetId(
        token: String,
        title: String,
    ): String {
        val response =
            api.routes(
                Request(Method.POST, "/houses")
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .body(
                        """{"title":"$title","lid":"${Uuid.random()}","areaSqMt":120,"pricePerNight":95.0,"description":"Casa teste"}""",
                    ),
            )
        return extractField(response.bodyString(), "id")
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
}
