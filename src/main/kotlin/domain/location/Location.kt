package domain.location

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class LocationType(val value: Int) {
    COUNTRY(0), REGION(1), DISTRICT(2), MUNICIPALITY(3), LOCALITY(4);

    companion object {
        val allowedChildren: Map<LocationType, Set<LocationType>> = mapOf(
            COUNTRY to setOf(REGION),
            REGION to setOf(DISTRICT),
            DISTRICT to setOf(MUNICIPALITY),
            MUNICIPALITY to setOf(LOCALITY),
            LOCALITY to emptySet()
        )

        fun isAllowedChild(parent: LocationType, child: LocationType): Boolean =
            allowedChildren[parent]?.contains(child) == true || parent.value > child.value

        fun allowedChildrenOf(type: LocationType): Set<LocationType> =
            allowedChildren[type].orEmpty()
    }
}


@OptIn(ExperimentalUuidApi::class)
data class Location(
    val id: Uuid,
    val name: String,
    val type: LocationType,
    val parentId: Uuid? = null
)
