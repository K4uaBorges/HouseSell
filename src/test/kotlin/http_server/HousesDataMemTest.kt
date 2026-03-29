package http_server

import main.api.http_server.HousesDataMem
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class HousesDataMemTest {
    @Test
    fun `services uses in-memory services when jdbc url is null`() {
        val resolved = HousesDataMem.services(null)

        assertSame(HousesDataMem.services, resolved)
    }

    @Test
    fun `services creates database-backed services when jdbc url is provided`() {
        val resolved = HousesDataMem.services("jdbc:postgresql://localhost:5432/testdb")

        assertNotSame(HousesDataMem.services, resolved)
    }
}
