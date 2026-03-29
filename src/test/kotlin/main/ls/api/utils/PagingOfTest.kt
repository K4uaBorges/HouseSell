package main.api.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test

class PagingOfTest {
    // ==================== Valid Cases ====================
    @Test
    fun `of with null values returns default paging`() {
        val paging = Paging.of(null, null)

        assertEquals(0, paging.skip)
        assertEquals(20, paging.limit)
    }

    @Test
    fun `of with valid values returns correct paging`() {
        val paging = Paging.of("10", "5")

        assertEquals(10, paging.skip)
        assertEquals(5, paging.limit)
    }

    @Test
    fun `of with zero skip returns valid paging`() {
        val paging = Paging.of("0", "20")

        assertEquals(0, paging.skip)
        assertEquals(20, paging.limit)
    }

    @Test
    fun `of with only skip null uses default skip`() {
        val paging = Paging.of(null, "30")

        assertEquals(0, paging.skip)
        assertEquals(30, paging.limit)
    }

    @Test
    fun `of with only limit null uses default limit`() {
        val paging = Paging.of("5", null)

        assertEquals(5, paging.skip)
        assertEquals(20, paging.limit)
    }

    @Test
    fun `of with empty strings uses defaults`() {
        val paging = Paging.of("", "")

        assertEquals(0, paging.skip)
        assertEquals(20, paging.limit)
    }

    @Test
    fun `of with invalid string uses defaults`() {
        // Invalid strings become null via toIntOrNull, so defaults are used
        val paging = Paging.of("invalid", "not_a_number")

        assertEquals(0, paging.skip)
        assertEquals(20, paging.limit)
    }

    @Test
    fun `of with mixed valid and invalid uses default for invalid`() {
        val paging = Paging.of("invalid", "15")

        assertEquals(0, paging.skip) // invalid -> default
        assertEquals(15, paging.limit) // valid
    }

    // ==================== Error Cases ====================

    @Test
    fun `of with negative skip throws IllegalArgumentException`() {
        val exception = assertThrows<IllegalArgumentException> {
            Paging.of("-1", "10")
        }
        assertEquals("skip must be >= 0", exception.message)
    }

    @Test
    fun `of with limit zero throws IllegalArgumentException`() {
        val exception = assertThrows<IllegalArgumentException> {
            Paging.of("0", "0")
        }
        assertEquals("limit must be >= 1", exception.message)
    }

    @Test
    fun `of with negative limit throws IllegalArgumentException`() {
        val exception = assertThrows<IllegalArgumentException> {
            Paging.of("0", "-5")
        }
        assertEquals("limit must be >= 1", exception.message)
    }

    @Test
    fun `of with limit exceeding max throws IllegalArgumentException`() {
        val exception = assertThrows<IllegalArgumentException> {
            Paging.of("0", "101")
        }
        assertEquals("limit must be <= 100", exception.message)
    }

    @Test
    fun `of with limit at max plus one throws`() {
        val exception = assertThrows<IllegalArgumentException> {
            Paging.of("0", "101")
        }
        assertTrue(exception.message!!.contains("100"))
    }
}
