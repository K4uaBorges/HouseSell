package main.domain_model.location

import main.errors.NoLocationValid
import main.errors.NoParentIdValid
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class Location(
    val id: Uuid,
    val name: LocationName,
    val type: LocationType,
    val parentId: Uuid?, // null for root locations (countries)
)

@JvmInline
value class LocationName private constructor(
    val value: String,
) {
    companion object {
        fun of(raw: String): LocationName {
            val trimmed = raw.trim()
            require(trimmed.length in 2..100) { "Name must be between 2 and 100 characters" }
            return LocationName(raw)
        }
    }

    override fun toString(): String = value
}

enum class LocationType(
    val value: Int,
) {
    COUNTRY(0),
    REGION(1),
    DISTRICT(2),
    MUNICIPALITY(3),
    LOCALITY(4),
    ;

    companion object {
        fun of(raw: String): LocationType =
            entries.find { it.name.equals(raw.trim().uppercase(), ignoreCase = true) }
                ?: throw NoLocationValid("Invalid location type: $raw. Valid types: ${entries.joinToString()}")

        val allowedChildren: Map<LocationType, Set<LocationType>> =
            mapOf(
                COUNTRY to setOf(REGION),
                REGION to setOf(DISTRICT),
                DISTRICT to setOf(MUNICIPALITY),
                MUNICIPALITY to setOf(LOCALITY),
                LOCALITY to emptySet(),
            )

        fun isAllowedChild(
            parent: LocationType,
            child: LocationType,
        ): Boolean = allowedChildren[parent]?.contains(child) == true || parent.value > child.value

        fun allowedChildrenOf(type: LocationType): Set<LocationType> = allowedChildren[type].orEmpty()
    }

    fun isHigherThan(other: LocationType): Boolean = this.ordinal < other.ordinal
}

@OptIn(ExperimentalUuidApi::class)
fun getParentsId(parentIdRaw: String): Uuid =
    runCatching { Uuid.parse(parentIdRaw.trim()) }
        .getOrElse { throw NoParentIdValid("Invalid parentId: $parentIdRaw") }
