package main.api.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.Test

class PagingPageTest {
    // ==================== Basic Cases ====================

    @Test
    fun `page with empty list returns empty list`() {
        val emptyList = emptyList<String>()
        val paging = Paging(0, 10)

        val result = emptyList.page(paging)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `page with skip 0 takes first elements`() {
        val list = (1..20).toList()
        val paging = Paging(0, 5)

        val result = list.page(paging)

        assertEquals(listOf(1, 2, 3, 4, 5), result)
    }

    @Test
    fun `page with skip takes elements from correct position`() {
        val list = (1..20).toList()
        val paging = Paging(10, 5)

        val result = list.page(paging)

        // skip=10 means drop first 10 (indices 0-9), so we get elements 11-15
        assertEquals(listOf(11, 12, 13, 14, 15), result)
    }

    @Test
    fun `page with skip larger than list size returns empty list`() {
        val list = (1..10).toList()
        val paging = Paging(20, 5)

        val result = list.page(paging)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `page with limit larger than remaining elements returns all remaining`() {
        val list = (1..10).toList()
        val paging = Paging(5, 20)

        val result = list.page(paging)

        // skip 5 -> [6,7,8,9,10], limit 20 but only 5 remain
        assertEquals(listOf(6, 7, 8, 9, 10), result)
        assertEquals(5, result.size)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `page with exact boundary returns correct sublist`() {
        val list = (1..15).toList()
        val paging = Paging(10, 5)

        val result = list.page(paging)

        // Elements 11-15 (indices 10-14)
        assertEquals(listOf(11, 12, 13, 14, 15), result)
    }

    @Test
    fun `page with limit 1 returns single element`() {
        val list = (1..100).toList()
        val paging = Paging(50, 1)

        val result = list.page(paging)

        assertEquals(listOf(51), result)
    }

    @Test
    fun `page with skip at last element returns single element`() {
        val list = (1..10).toList()
        val paging = Paging(9, 5)

        val result = list.page(paging)

        assertEquals(listOf(10), result)
    }

    @Test
    fun `page with skip at exact end returns empty list`() {
        val list = (1..10).toList()
        val paging = Paging(10, 5)

        val result = list.page(paging)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `page with skip beyond end returns empty list`() {
        val list = (1..10).toList()
        val paging = Paging(100, 5)

        val result = list.page(paging)

        assertTrue(result.isEmpty())
    }

    // ==================== Different Types ====================

    @Test
    fun `page works with strings`() {
        val stringList = listOf("a", "b", "c", "d", "e")
        val paging = Paging(2, 2)

        val result = stringList.page(paging)

        assertEquals(listOf("c", "d"), result)
    }

    @Test
    fun `page works with data classes`() {
        data class TestItem(val id: Int, val name: String)

        val items = (1..10).map { TestItem(it, "Item $it") }
        val paging = Paging(3, 4)

        val result = items.page(paging)

        assertEquals(4, result.size)
        assertEquals(4, result[0].id) // Item 4 (index 3)
        assertEquals(7, result[3].id) // Item 7 (index 6)
    }

    @Test
    fun `page preserves order of original list`() {
        val list = listOf("z", "a", "m", "b", "c")
        val paging = Paging(1, 3)

        val result = list.page(paging)

        assertEquals(listOf("a", "m", "b"), result)
    }

    // ==================== Pagination Scenarios ====================

    @Test
    fun `consecutive pages cover entire list`() {
        val list = (1..10).toList()
        val pageSize = 3

        val page1 = list.page(Paging(0, pageSize))
        val page2 = list.page(Paging(3, pageSize))
        val page3 = list.page(Paging(6, pageSize))
        val page4 = list.page(Paging(9, pageSize))

        assertEquals(listOf(1, 2, 3), page1)
        assertEquals(listOf(4, 5, 6), page2)
        assertEquals(listOf(7, 8, 9), page3)
        assertEquals(listOf(10), page4)

        // Verify all elements are covered
        val allElements = page1 + page2 + page3 + page4
        assertEquals(list, allElements)
    }

    @Test
    fun `page with default paging on small list returns all`() {
        val list = (1..5).toList()
        val paging = Paging(0, 20) // default limit

        val result = list.page(paging)

        assertEquals(list, result)
    }

    @Test
    fun `page with default paging on large list returns first 20`() {
        val list = (1..100).toList()
        val paging = Paging(0, 20) // default limit

        val result = list.page(paging)

        assertEquals(20, result.size)
        assertEquals((1..20).toList(), result)
    }
}
