package main.api.httpServer

import org.http4k.client.JavaHttpClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.server.Http4kServer
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StaticContentIntegrationTest {
    companion object {
        private const val TEST_PORT = 18980
    }

    private val client = JavaHttpClient()
    private lateinit var server: Http4kServer
    private lateinit var baseUrl: String

    @BeforeTest
    fun setup() {
        // In-memory services, sem base de dados
        val services = HousesDataMem.services
        val router = HousesRouter(HousesWebApi(services), port = TEST_PORT)
        server = router.start()
        baseUrl = "http://localhost:$TEST_PORT"
    }

    @AfterTest
    fun teardown() {
        server.stop()
    }

    @Test
    fun `GET root returns index html`() {
        val response = client(Request(Method.GET, "$baseUrl/"))

        assertEquals(Status.OK, response.status)
        val body = response.bodyString()
        assertTrue(body.contains("<title>Booking Houses / Rent House</title>"), "Deve conter o titulo")
        assertTrue(body.contains("<script type=\"module\" src=\"./index.js\""), "Deve referenciar o index.js")
        assertEquals(response.header("Content-Type")?.contains("text/html"), true)
    }

    @Test
    fun `GET index js returns javascript module`() {
        val response = client(Request(Method.GET, "$baseUrl/index.js"))

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("indexSPA.js"), "Deve ser o entrypoint do SPA")
        assertEquals(response.header("Content-Type")?.contains("javascript"), true)
    }

    @Test
    fun `GET dsl js returns dsl module`() {
        val response = client(Request(Method.GET, "$baseUrl/dsl/dsl.js"))

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("export function el"), "Deve exportar a funcao el")
    }

    @Test
    fun `GET router js returns router module`() {
        val response = client(Request(Method.GET, "$baseUrl/router/router.js"))

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("export default router"), "Deve exportar o router")
    }

    @Test
    fun `GET handlers js returns handlers module`() {
        val response = client(Request(Method.GET, "$baseUrl/handlers/indexHandlers.js"))

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("const handlers"), "Deve exportar handlers")
    }

    @Test
    fun `GET unknown path returns index html for spa routing`() {
        val response = client(Request(Method.GET, "$baseUrl/nonexistent-spa-path"))

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("<title>Booking Houses / Rent House</title>"))
    }

    @Test
    fun `GET root returns index html even when user dir is parent folder`() {
        val originalUserDir = System.getProperty("user.dir")
        val parentDir = File(originalUserDir).parentFile?.absolutePath
        assertNotNull(parentDir)

        try {
            server.stop()
            System.setProperty("user.dir", parentDir)
            server = HousesRouter(HousesWebApi(HousesDataMem.services), port = TEST_PORT).start()
            val isolatedBaseUrl = "http://localhost:$TEST_PORT"

            val response = client(Request(Method.GET, "$isolatedBaseUrl/"))

            assertEquals(Status.OK, response.status)
            assertTrue(response.bodyString().contains("<title>Booking Houses / Rent House</title>"))
        } finally {
            System.setProperty("user.dir", originalUserDir)
        }
    }
}
