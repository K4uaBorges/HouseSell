package main.api.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.Test

/**
 * Integration Tests for [Paging]
 * - It's simulated real scenarios of API
 * - This tests verify the behavior of paging in the context of endpoints of
 * application (houses, users, bookings, locations).
 */
class PagingIntegrationTest {
    // ==================== Houses API Scenarios ====================

    @Test
    fun `api scenario - GET houses with skip and limit`() {
        // Simulates: GET /houses?skip=10&limit=5
        // Returns houses from position 11 to 15
        val allHouses = (1..100).map { "House $it" }
        val paging = Paging.of("10", "5")

        val houses = allHouses.page(paging)

        assertEquals(5, houses.size)
        assertEquals("House 11", houses[0]) // skip 10 = start at 11
        assertEquals("House 15", houses[4])
    }

    @Test
    fun `api scenario - GET houses first page`() {
        // Simulates: GET /houses?limit=20 (first page, no skip)
        val allHouses = (1..50).map { "House $it" }
        val paging = Paging.of(null, "20")

        val houses = allHouses.page(paging)

        assertEquals(20, houses.size)
        assertEquals("House 1", houses[0])
        assertEquals("House 20", houses[19])
    }

    @Test
    fun `api scenario - GET houses last partial page`() {
        // Simulates: GET /houses?skip=40&limit=20 on a 50-item list
        // Should return only remaining 10 items (positions 41-50)
        val allHouses = (1..50).map { "House $it" }
        val paging = Paging.of("40", "20")

        val houses = allHouses.page(paging)

        assertEquals(10, houses.size) // Only 10 remain
        assertEquals("House 41", houses[0])
        assertEquals("House 50", houses[9])
    }

    @Test
    fun `api scenario - GET houses with default paging`() {
        // Simulates: GET /houses (no query params)
        // Uses defaults: skip=0, limit=20
        val allHouses = (1..100).map { "House $it" }
        val paging = Paging.of(null, null)

        val houses = allHouses.page(paging)

        assertEquals(20, houses.size)
        assertEquals("House 1", houses[0])
        assertEquals("House 20", houses[19])
    }

    @Test
    fun `api scenario - GET houses empty result`() {
        // Simulates: GET /houses?skip=1000&limit=20 on small dataset
        val allHouses = (1..50).map { "House $it" }
        val paging = Paging.of("1000", "20")

        val houses = allHouses.page(paging)

        assertTrue(houses.isEmpty())
    }

    // ==================== Users API Scenarios ====================

    @Test
    fun `api scenario - GET users first page`() {
        // Simulates: GET /users
        val allUsers = (1..25).map { "User $it" }
        val paging = Paging.of(null, null)

        val users = allUsers.page(paging)

        assertEquals(20, users.size) // default limit
        assertEquals("User 1", users[0])
        assertEquals("User 20", users[19])
    }

    @Test
    fun `api scenario - GET users second page`() {
        // Simulates: GET /users?skip=20&limit=20
        val allUsers = (1..45).map { "User $it" }
        val paging = Paging.of("20", "20")

        val users = allUsers.page(paging)

        assertEquals(20, users.size)
        assertEquals("User 21", users[0])
        assertEquals("User 40", users[19])
    }

    @Test
    fun `api scenario - GET users last page partial`() {
        // Simulates: GET /users?skip=40&limit=20 on 45 users
        val allUsers = (1..45).map { "User $it" }
        val paging = Paging.of("40", "20")

        val users = allUsers.page(paging)

        assertEquals(5, users.size)
        assertEquals("User 41", users[0])
        assertEquals("User 45", users[4])
    }

    // ==================== Bookings API Scenarios ====================

    @Test
    fun `api scenario - GET bookings by house with pagination`() {
        // Simulates: GET /bookings?hid=xxx&dateStart=...&dateEnd=...&skip=0&limit=10
        val allBookings = (1..35).map { "Booking $it" }
        val paging = Paging.of("0", "10")

        val bookings = allBookings.page(paging)

        assertEquals(10, bookings.size)
        assertEquals("Booking 1", bookings[0])
        assertEquals("Booking 10", bookings[9])
    }

    @Test
    fun `api scenario - GET bookings with large skip`() {
        // Simulates: GET /bookings?skip=100&limit=10
        val allBookings = (1..150).map { "Booking $it" }
        val paging = Paging.of("100", "10")

        val bookings = allBookings.page(paging)

        assertEquals(10, bookings.size)
        assertEquals("Booking 101", bookings[0])
        assertEquals("Booking 110", bookings[9])
    }

    // ==================== Locations API Scenarios ====================

    @Test
    fun `api scenario - GET locations list with pagination`() {
        // Simulates: GET /locations (if it supported pagination)
        val allLocations = (1..200).map { "Location $it" }
        val paging = Paging.of("50", "25")

        val locations = allLocations.page(paging)

        assertEquals(25, locations.size)
        assertEquals("Location 51", locations[0])
        assertEquals("Location 75", locations[24])
    }

    // ==================== Error Handling Scenarios ====================

    @Test
    fun `api scenario - invalid skip parameter uses default`() {
        // Simulates: GET /houses?skip=invalid&limit=10
        // Should use default skip=0
        val allHouses = (1..30).map { "House $it" }
        val paging = Paging.of("invalid", "10")

        val houses = allHouses.page(paging)

        assertEquals(10, houses.size)
        assertEquals("House 1", houses[0]) // default skip=0
    }

    @Test
    fun `api scenario - invalid limit parameter uses default`() {
        // Simulates: GET /houses?skip=0&limit=invalid
        // Should use default limit=20
        val allHouses = (1..30).map { "House $it" }
        val paging = Paging.of("0", "invalid")

        val houses = allHouses.page(paging)

        assertEquals(20, houses.size) // default limit
    }

    @Test
    fun `api scenario - negative skip throws error`() {
        // Simulates: GET /houses?skip=-1&limit=10
        // Should throw IllegalArgumentException
        try {
            Paging.of("-1", "10")
            assertTrue(false, "Should have thrown exception")
        } catch (e: IllegalArgumentException) {
            assertEquals("skip must be >= 0", e.message)
        }
    }

    @Test
    fun `api scenario - limit exceeding max throws error`() {
        // Simulates: GET /houses?skip=0&limit=200
        // Should throw IllegalArgumentException (max is 100)
        try {
            Paging.of("0", "200")
            assertTrue(false, "Should have thrown exception")
        } catch (e: IllegalArgumentException) {
            assertEquals("limit must be <= 100", e.message)
        }
    }

    @Test
    fun `api scenario - zero limit throws error`() {
        // Simulates: GET /houses?skip=0&limit=0
        try {
            Paging.of("0", "0")
            assertTrue(false, "Should have thrown exception")
        } catch (e: IllegalArgumentException) {
            assertEquals("limit must be >= 1", e.message)
        }
    }

    // ==================== Real-world Pagination Flow ====================

    @Test
    fun `api scenario - complete pagination flow for houses`() {
        // Simulates a user browsing through all houses page by page
        val allHouses = (1..55).map { "House $it" }
        val pageSize = 20

        // Page 1
        val page1 = allHouses.page(Paging.of("0", "$pageSize"))
        assertEquals(20, page1.size)
        assertEquals("House 1", page1[0])
        assertEquals("House 20", page1[19])

        // Page 2
        val page2 = allHouses.page(Paging.of("20", "$pageSize"))
        assertEquals(20, page2.size)
        assertEquals("House 21", page2[0])
        assertEquals("House 40", page2[19])

        // Page 3 (last, partial)
        val page3 = allHouses.page(Paging.of("40", "$pageSize"))
        assertEquals(15, page3.size)
        assertEquals("House 41", page3[0])
        assertEquals("House 55", page3[14])

        // Verify no overlap and all items covered
        val allPagedItems = page1 + page2 + page3
        assertEquals(allHouses.size, allPagedItems.size)
        assertEquals(allHouses, allPagedItems)
    }

    @Test
    fun `api scenario - pagination with exact multiples`() {
        // When total is exact multiple of page size (e.g., 60 items, page size 20)
        val allItems = (1..60).map { "Item $it" }

        val page1 = allItems.page(Paging.of("0", "20"))
        val page2 = allItems.page(Paging.of("20", "20"))
        val page3 = allItems.page(Paging.of("40", "20"))
        val page4 = allItems.page(Paging.of("60", "20")) // Beyond end

        assertEquals(20, page1.size)
        assertEquals(20, page2.size)
        assertEquals(20, page3.size)
        assertEquals(0, page4.size) // Empty, not error

        val allPaged = page1 + page2 + page3
        assertEquals(60, allPaged.size)
    }

    // ==================== Data Integrity Scenarios ====================

    @Test
    fun `api scenario - pagination preserves item order`() {
        // Ensures that pagination doesn't reorder items
        val orderedHouses =
            listOf(
                "House A - First",
                "House B - Second",
                "House C - Third",
                "House D - Fourth",
                "House E - Fifth",
            )

        val page1 = orderedHouses.page(Paging.of("0", "2"))
        val page2 = orderedHouses.page(Paging.of("2", "2"))
        val page3 = orderedHouses.page(Paging.of("4", "2"))

        assertEquals(listOf("House A - First", "House B - Second"), page1)
        assertEquals(listOf("House C - Third", "House D - Fourth"), page2)
        assertEquals(listOf("House E - Fifth"), page3)
    }

    @Test
    fun `api scenario - pagination with single item pages`() {
        // Edge case: limit=1
        val items = (1..5).map { "Item $it" }

        val results =
            (0..4).map { skip ->
                items.page(Paging.of("$skip", "1"))
            }

        assertEquals("Item 1", results[0][0])
        assertEquals("Item 2", results[1][0])
        assertEquals("Item 3", results[2][0])
        assertEquals("Item 4", results[3][0])
        assertEquals("Item 5", results[4][0])
    }
}
