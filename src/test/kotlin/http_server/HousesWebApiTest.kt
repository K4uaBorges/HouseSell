package http_server

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

class HousesWebApiTest {
    private val api = HousesWebApi(HousesDataMem.services)

    @BeforeTest
    fun clearRepos() {
        InMemoryUsersRepository.clear()
        InMemoryHouseRepository.clear()
        InMemoryBookingRepository.clear()
        InMemoryLocationRepository.clear()
    }

    @Test
    fun `POST users with invalid json returns 400`() {
        val response = api.routes(
            Request(Method.POST, "/users")
                .header("Content-Type", "application/json")
                .body("{invalid"),
        )

        assertEquals(Status.BAD_REQUEST, response.status)
        assertTrue(response.bodyString().contains("Invalid JSON body."))
    }

    @Test
    fun `POST users creates and GET users lists it`() {
        val createResponse = api.routes(
            Request(Method.POST, "/users")
                .header("Content-Type", "application/json")
                .body("""{"name":"Alice","email":"alice@example.com"}"""),
        )
        val listResponse = api.routes(Request(Method.GET, "/users"))

        assertEquals(Status.CREATED, createResponse.status)
        assertEquals(Status.OK, listResponse.status)
        assertTrue(listResponse.bodyString().contains("alice@example.com"))
    }

    @Test
    fun `DELETE users returns delete response and removes user`() {
        val createResponse = api.routes(
            Request(Method.POST, "/users")
                .header("Content-Type", "application/json")
                .body("""{"name":"Alice","email":"alice@example.com"}"""),
        )
        val userId = """"id":"([^"]+)"""".toRegex().find(createResponse.bodyString())!!.groupValues[1]

        val deleteResponse = api.routes(
            Request(Method.DELETE, "/users/$userId")
                .header("Content-Type", "application/json")
                .body("""{"id":"$userId"}"""),
        )

        assertEquals(Status.OK, deleteResponse.status)
        assertTrue(deleteResponse.bodyString().contains(""""deleted":true"""))

        val listResponse = api.routes(Request(Method.GET, "/users"))
        assertTrue(!listResponse.bodyString().contains("alice@example.com"))
    }
}
