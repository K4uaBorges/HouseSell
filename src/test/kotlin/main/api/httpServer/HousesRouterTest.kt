package main.api.httpServer

import main.data.impl.mem.InMemoryBookingRepository
import main.data.impl.mem.InMemoryHouseRepository
import main.data.impl.mem.InMemoryLocationRepository
import main.data.impl.mem.InMemoryUsersRepository
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HousesRouterTest {
    companion object {
        private const val TEST_PORT = 18980
    }

    @BeforeTest
    fun clearRepos() {
        InMemoryUsersRepository.clear()
        InMemoryHouseRepository.clear()
        InMemoryBookingRepository.clear()
        InMemoryLocationRepository.clear()
    }

    @Test
    fun `start binds the configured port`() {
        val server = HousesRouter(HousesWebApi(HousesDataMem.services), TEST_PORT).start()

        try {
            assertEquals(TEST_PORT, server.port())
        } finally {
            server.stop()
        }
    }

    @Test
    fun `start serves requests with the configured web api`() {
        val server = HousesRouter(HousesWebApi(HousesDataMem.services), TEST_PORT).start()

        try {
            val response =
                HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:${server.port()}/api/users"))
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

    @Test
    fun `start exposes cors headers for cross-origin api requests`() {
        val server = HousesRouter(HousesWebApi(HousesDataMem.services), TEST_PORT).start()

        try {
            val response =
                HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:${server.port()}/api/users"))
                        .header("Origin", "http://localhost:5500")
                        .GET()
                        .build(),
                    HttpResponse.BodyHandlers.ofString(),
                )

            assertEquals(200, response.statusCode())
            assertNotNull(response.headers().firstValue("access-control-allow-origin").orElse(null))
        } finally {
            server.stop()
        }
    }
}
