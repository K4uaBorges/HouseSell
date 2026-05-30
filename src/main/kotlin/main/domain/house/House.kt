package main.domain.house
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@JvmInline
value class Title private constructor(
    val value: String,
) {
    companion object {
        fun of(raw: String): Title {
            val s = raw.trim()
            require(s.isNotEmpty()) { "Invalid" }
            require(s.length in 3..100) { "The name need between 3 and 100" }
            return Title(s)
        }
    }

    override fun toString(): String = value
}

@OptIn(ExperimentalUuidApi::class)
data class House(
    val id: Uuid,
    // Owner
    val uid: Uuid,
    val title: Title,
    val lid: Uuid,
    val areaSqMt: Int,
    val pricePerNight: Double,
    val description: String,
)
