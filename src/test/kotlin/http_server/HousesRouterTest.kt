package http_server

import main.api.http_server.HousesDataMem
import main.api.http_server.HousesRouter
import main.api.http_server.HousesWebApi
import main.data.impl.mem.InMemoryBookingRepository
import main.data.impl.mem.InMemoryHouseRepository
import main.data.impl.mem.InMemoryLocationRepository
import main.data.impl.mem.InMemoryUsersRepository
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HousesRouterTest {
    @BeforeTest
    fun clearRepos() {
        InMemoryUsersRepository.clear()
        InMemoryHouseRepository.clear()
        InMemoryBookingRepository.clear()
        InMemoryLocationRepository.clear()
    }

    @Test
    fun `start binds the configured port`() {
        val port = ServerSocket(0).use { it.localPort }
        val server = HousesRouter(HousesWebApi(HousesDataMem.services), port).start()

        try {
            assertEquals(port, server.port())
        } finally {
            server.stop()
        }
    }

    @Test
    fun `start serves requests with the configured web api`() {
        val server = HousesRouter(HousesWebApi(HousesDataMem.services), 0).start()

        try {
            val response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:${server.port()}/users"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString(),
            )

            assertEquals(200, response.statusCode())
            assertTrue(response.body().contains("\"users\""))
        } finally {
            server.stop()
        }
    }
}
