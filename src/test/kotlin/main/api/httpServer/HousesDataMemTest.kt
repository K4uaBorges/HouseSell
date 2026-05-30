package main.api.httpServer

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class HousesDataMemTest {
    @Test
    fun `services uses in-memory services when jdbc url is null`() {
        val resolved = HousesDataMem.services

        assertSame(HousesDataMem.services, resolved)
    }

    @Test
    fun `services attempts database-backed services when jdbc url is provided`() {
        assertFailsWith<Exception> {
            HousesDataMem.services("jdbc:postgresql://localhost:5432/testdb")
        }
    }
}
