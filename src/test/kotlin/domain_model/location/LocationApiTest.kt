package domain_model.location

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import main.api.http_server.HousesDataMem
import main.api.http_server.HousesWebApi
import main.data.impl.mem.InMemoryBookingRepository
import main.data.impl.mem.InMemoryHouseRepository
import main.data.impl.mem.InMemoryLocationRepository
import main.data.impl.mem.InMemoryUsersRepository
import main.domain_model.user.Email
import main.domain_model.user.Name
import main.domain_model.user.User
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
private data class LocationResponse(
    val id: String,
    val parentId: String? = null,
)

@OptIn(ExperimentalUuidApi::class)
class LocationApiTest {
    private val services = HousesDataMem.services
    private val api = HousesWebApi(services)
    private val json = Json { ignoreUnknownKeys = true }
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
    fun `POST locations creates country`() {
        val request =
            Request(Method.POST, "/locations")
                .header("Authorization", "Bearer $adminToken")
                .header("Content-Type", "application/json")
                .body("""{"name":"Portugal","type":"COUNTRY"}""")

        val response = api.routes(request)

        assertEquals(Status.CREATED, response.status)
        assertTrue(response.bodyString().contains("Portugal"))
    }

    @Test
    fun `GET locations returns list`() {
        api.routes(
            Request(Method.POST, "/locations")
                .header("Authorization", "Bearer $adminToken")
                .header("Content-Type", "application/json")
                .body("""{"name":"Portugal","type":"COUNTRY"}"""),
        )

        val response = api.routes(Request(Method.GET, "/locations"))

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("Portugal"))
    }

    @Test
    fun `GET location with invalid id returns 400`() {
        val response = api.routes(Request(Method.GET, "/locations/invalid-uuid"))

        assertEquals(Status.BAD_REQUEST, response.status)
    }

    @Test
    fun `GET location with non-existent id returns 404`() {
        val response = api.routes(Request(Method.GET, "/locations/${UUID.randomUUID()}"))

        assertEquals(Status.NOT_FOUND, response.status)
    }

    @Test
    fun `POST locations without body should return 400`() {
        val request =
            Request(Method.POST, "/locations")
                .header("Authorization", "Bearer $adminToken")
                .header("Content-Type", "application/json")
                .body("""""")

        val response = api.routes(request)

        assertEquals(Status.BAD_REQUEST, response.status)
    }

    @Test
    fun `POST location with invalid hierarchy returns 400`() {
        val countryResp =
            api.routes(
                Request(Method.POST, "/locations")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body("""{"name":"Portugal","type":"COUNTRY"}"""),
            )

        val countryId = extractId(countryResp.bodyString())
        val response =
            api.routes(
                Request(Method.POST, "/locations")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body("""{"name":"Lisboa","type":"DISTRICT","parentId":"$countryId"}"""),
            )

        assertEquals(Status.BAD_REQUEST, response.status)
    }

    @Test
    fun `PUT locations updates location`() {
        val created =
            api.routes(
                Request(Method.POST, "/locations")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body("""{"name":"Portugal","type":"COUNTRY"}"""),
            )
        val id = extractId(created.bodyString())

        val response =
            api.routes(
                Request(Method.PUT, "/locations/$id")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body("""{"name":"Portugal Updated","type":"COUNTRY","parentId":null}"""),
            )

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("Portugal Updated"))
    }

    @Test
    fun `GET location children all returns 200`() {
        val country =
            api.routes(
                Request(Method.POST, "/locations")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body("""{"name":"Portugal","type":"COUNTRY"}"""),
            )
        val countryId = extractId(country.bodyString())

        api.routes(
            Request(Method.POST, "/locations")
                .header("Authorization", "Bearer $adminToken")
                .header("Content-Type", "application/json")
                .body("""{"name":"Norte","type":"REGION","parentId":"$countryId"}"""),
        )

        val response = api.routes(Request(Method.GET, "/locations/$countryId/childrenAll"))

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("Norte"))
    }

    @Test
    fun `GET location children direct returns 200`() {
        val country =
            api.routes(
                Request(Method.POST, "/locations")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body("""{"name":"Portugal","type":"COUNTRY"}"""),
            )
        val countryId = extractId(country.bodyString())

        api.routes(
            Request(Method.POST, "/locations")
                .header("Authorization", "Bearer $adminToken")
                .header("Content-Type", "application/json")
                .body("""{"name":"Norte","type":"REGION","parentId":"$countryId"}"""),
        )

        val response = api.routes(Request(Method.GET, "/locations/$countryId/childrenDirect"))

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("Norte"))
    }

    @Test
    fun `GET location path returns 200`() {
        val country =
            api.routes(
                Request(Method.POST, "/locations")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body("""{"name":"Portugal","type":"COUNTRY"}"""),
            )
        val countryId = extractId(country.bodyString())
        val region =
            api.routes(
                Request(Method.POST, "/locations")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body("""{"name":"Norte","type":"REGION","parentId":"$countryId"}"""),
            )
        val regionId = extractId(region.bodyString())

        val response = api.routes(Request(Method.GET, "/locations/$regionId/path"))

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("Portugal"))
        assertTrue(response.bodyString().contains("Norte"))
    }

    @Test
    fun `DELETE location removes leaf`() {
        val created =
            api.routes(
                Request(Method.POST, "/locations")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body("""{"name":"Portugal","type":"COUNTRY"}"""),
            )

        val createdChild =
            api.routes(
                Request(Method.POST, "/locations")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body("""{"name":"Norte","type":"REGION","parentId":"${extractId(created.bodyString())}"}"""),
            )

        val createdChildSqd =
            api.routes(
                Request(Method.POST, "/locations")
                    .header("Authorization", "Bearer $adminToken")
                    .header("Content-Type", "application/json")
                    .body("""{"name":"Porto","type":"DISTRICT","parentId":"${extractId(createdChild.bodyString())}"}"""),
            )
        val idchlsqd = extractId(createdChildSqd.bodyString())

        val deleteResponse =
            api.routes(
                Request(Method.DELETE, "/locations/$idchlsqd")
                    .header("Authorization", "Bearer $adminToken"),
            )
        val getResponse3 = api.routes(Request(Method.GET, "/locations/$idchlsqd"))

        assertEquals(Status.OK, deleteResponse.status)
        assertEquals(Status.NOT_FOUND, getResponse3.status)
    }

    private fun extractId(jsonStr: String): String = json.decodeFromString<domain_model.location.LocationResponse>(jsonStr).id

    private fun seedAuthUserToken(): Uuid {
        val authUser =
            User(
                id = Uuid.random(),
                name = Name.of("Zulu Admin"),
                email = Email.of("zulu-admin@example.com"),
                token = Uuid.random(),
            )
        InMemoryUsersRepository.create(authUser)
        return authUser.token
    }
}
