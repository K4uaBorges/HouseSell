package main.api.utils

data class Paging(val skip: Int, val limit: Int) {
    companion object {
        const val DEFAULT_SKIP = 0
        const val DEFAULT_LIMIT = 20
        const val MAX_LIMIT = 100

        fun of(skipRaw: String?, limitRaw: String?): Paging {
            val skip = skipRaw?.toIntOrNull() ?: DEFAULT_SKIP
            val limit = limitRaw?.toIntOrNull() ?: DEFAULT_LIMIT

            require(skip >= 0) { "skip must be >= 0" }
            require(limit >= 1) { "limit must be >= 1" }
            require(limit <= MAX_LIMIT) { "limit must be <= $MAX_LIMIT" }

            return Paging(skip, limit)
        }
    }
}

fun <T> List<T>.page(p: Paging): List<T> =
    this.drop(p.skip).take(p.limit)