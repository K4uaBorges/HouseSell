package http_server

import main.api.http_server.HousesDataMem
import main.api.http_server.HousesRouter
import main.api.http_server.HousesWebApi
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.server.Http4kServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StaticContentIntegrationTest {
    private val client = JavaHttpClient()
    private lateinit var server: Http4kServer
    private lateinit var baseUrl: String

    @BeforeTest
    fun setup() {
        // In-memory services, sem base de dados
        val services = HousesDataMem.services(null)
        // Porta 0 = porta aleatória livre
        val router = HousesRouter(HousesWebApi(services), port = 0)
        server = router.start()
        baseUrl = "http://localhost:${server.port()}"
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
        assertTrue(body.contains("<title>Houses Rentals</title>"), "Deve conter o titulo")
        assertTrue(body.contains("<script src=\"indexSPA.js\""), "Deve referenciar o indexSPA.js")
        assertEquals(response.header("Content-Type")?.contains("text/html"), true)
    }

    @Test
    fun `GET index spa js returns javascript module`() {
        val response = client(Request(Method.GET, "$baseUrl/indexSPA.js"))

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("import router from"), "Deve ser o entrypoint do SPA")
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
        assertTrue(response.bodyString().contains("<title>Houses Rentals</title>"))
    }
}
