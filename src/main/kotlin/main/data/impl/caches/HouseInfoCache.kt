package main.data.impl.caches

import main.data.interfaces.Cache
import main.domain.house.House
import java.util.ArrayDeque
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class HouseInfoCache(
    val limit: Int,
) : Cache<Uuid, House> {
    init {
        require(limit > 0) { "Cache limit must be greater than zero." }
    }

    private val cache = ArrayDeque<Pair<Uuid, House>>(limit)
    private var hits: Long = 0
    private var misses: Long = 0

    override fun getById(id: Uuid): House? {
        val entry = cache.find { it.first == id }
        if (entry == null) {
            misses += 1
            return null
        }

        // Access refresh: move element to the end to preserve last-N semantics.
        cache.remove(entry)
        cache.addLast(entry)
        hits += 1
        return entry.second
    }

    override fun put(
        id: Uuid,
        value: House,
    ) {
        cache.find { it.first == id }?.let {
            cache.remove(it)
            cache.addLast(Pair(id, value))
            return
        }

        if (cache.size >= limit) {
            cache.removeFirst()
        }
        cache.addLast(Pair(id, value))
    }

    override fun clear() {
        cache.clear()
        hits = 0
        misses = 0
    }

    fun removeById(id: Uuid) {
        cache.removeIf { it.first == id }
    }

    fun stats(): HouseCacheStats =
        HouseCacheStats(
            limit = limit,
            size = cache.size,
            hits = hits,
            misses = misses,
        )
}

data class HouseCacheStats(
    val limit: Int,
    val size: Int,
    val hits: Long,
    val misses: Long,
)
