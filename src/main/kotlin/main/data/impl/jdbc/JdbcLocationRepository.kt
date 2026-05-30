package main.data.impl.jdbc

import main.data.interfaces.LocationRepository
import main.domain.location.Location
import main.domain.location.LocationName
import main.domain.location.LocationType
import main.errors.LocationsRepositoryDatabaseException
import main.errors.NoLocationExist
import java.sql.ResultSet
import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class JdbcLocationRepository(
    private val dataSource: DataSource,
) : LocationRepository {
    override fun create(value: Location): Location =
        withDatabaseHandling("creating location") {
            dataSource.connection.use { conn ->
                conn
                    .prepareStatement(
                        """
                        INSERT INTO locations (lid, name, loc_type, parent_lid)
                        VALUES (?, ?, ?, ?)
                        """.trimIndent(),
                    ).use { stmt ->
                        stmt.setObject(1, toJavaUuid(value.id))
                        stmt.setString(2, value.name.value)
                        stmt.setString(3, value.type.name)
                        stmt.setObject(4, value.parentId?.let(::toJavaUuid))
                        stmt.executeUpdate()
                    }
            }
            value
        }

    override fun getById(key: Uuid): Location =
        withDatabaseHandling("getting location by id") {
            dataSource.connection.use { conn ->
                conn
                    .prepareStatement(
                        """
                        SELECT lid, name, loc_type, parent_lid
                        FROM locations
                        WHERE lid = ?
                        """.trimIndent(),
                    ).use { stmt ->
                        stmt.setObject(1, toJavaUuid(key))
                        stmt.executeQuery().use { rs ->
                            if (!rs.next()) throw NoLocationExist("Location not found.")
                            mapLocation(rs)
                        }
                    }
            }
        }

    override fun save(value: Location): Location =
        withDatabaseHandling("updating location") {
            getById(value.id)

            dataSource.connection.use { conn ->
                conn
                    .prepareStatement(
                        """
                        update locations
                        set lid = ?, name = ?, loc_type = ?, parent_lid = ?
                        where lid = ?
                        """.trimIndent(),
                    ).use { stmt ->
                        stmt.setObject(1, toJavaUuid(value.id))
                        stmt.setString(2, value.name.value)
                        stmt.setString(3, value.type.name)
                        stmt.setObject(4, value.parentId?.let(::toJavaUuid))
                        stmt.setObject(5, toJavaUuid(value.id))
                        val updated = stmt.executeUpdate()
                        if (updated == 0) throw NoLocationExist("Location not found.")
                    }
            }
            value
        }

    override fun update(updated: Location): Location = save(updated)

    override fun deleteById(key: Uuid) {
        withDatabaseHandling("deleting location") {
            dataSource.connection.use { conn ->
                conn.prepareStatement("delete from locations where lid = ?").use { stmt ->
                    stmt.setObject(1, toJavaUuid(key))
                    val deleted = stmt.executeUpdate()
                    if (deleted == 0) throw NoLocationExist("Location not found.")
                }
            }
        }
    }

    override fun clear() {
        withDatabaseHandling("clearing locations") {
            dataSource.connection.use { conn ->
                conn.prepareStatement("delete from locations").use { stmt ->
                    stmt.executeUpdate()
                }
            }
        }
    }

    override fun getChildrenAll(parentId: Uuid): List<Location> =
        withDatabaseHandling("getting location children") {
            dataSource.connection.use { conn ->
                conn
                    .prepareStatement(
                        """
                        WITH RECURSIVE all_children_locs AS (
                            SELECT lid, name, loc_type, parent_lid
                            FROM locations
                            WHERE parent_lid = ?
                            
                            UNION ALL
                            
                            SELECT l.lid, l.name, l.loc_type, l.parent_lid
                            FROM locations l
                            INNER JOIN all_children_locs child ON l.parent_lid = child.lid
                        )
                        SELECT * FROM all_children_locs
                        """.trimIndent(),
                    ).use { stmt ->
                        stmt.setObject(1, toJavaUuid(parentId))
                        stmt.executeQuery().use { rs ->
                            val result = mutableListOf<Location>()
                            while (rs.next()) {
                                result.plusAssign(mapLocation(rs))
                            }
                            result
                        }
                    }
            }
        }

    override fun getChildrenDirect(parentId: Uuid): List<Location> =
        withDatabaseHandling("getting location children") {
            dataSource.connection.use { conn ->
                conn.prepareStatement(
                    """
                    SELECT lid, name, loc_type, parent_lid
                    FROM locations
                    WHERE parent_lid = ?
                    """.trimIndent(),
                ).use { stmt ->
                    stmt.setObject(1, toJavaUuid(parentId))
                    stmt.executeQuery().use { rs ->
                        val result = mutableListOf<Location>()
                        while (rs.next()) {
                            result.plusAssign(mapLocation(rs))
                        }
                        result
                    }
                }
            }
        }

    override fun getFullPath(id: Uuid): List<Location> =
        withDatabaseHandling("getting location full path") {
            // Recursive CTE: goes from child up to root, then we reverse
            dataSource.connection.use { conn ->
                conn
                    .prepareStatement(
                        """
                        WITH RECURSIVE path AS (
                            SELECT lid, name, loc_type, parent_lid, 0 as depth
                            FROM locations
                            WHERE lid = ?
                            UNION ALL
                            SELECT l.lid, l.name, l.loc_type, l.parent_lid, p.depth + 1
                            FROM locations l
                            INNER JOIN path p ON l.lid = p.parent_lid
                        )
                        SELECT * FROM path
                        ORDER BY depth DESC
                        """.trimIndent(),
                    ).use { stmt ->
                        stmt.setObject(1, toJavaUuid(id))
                        stmt.executeQuery().use { rs ->
                            val result = mutableListOf<Location>()
                            while (rs.next()) {
                                result.plusAssign(mapLocation(rs))
                            }
                            result
                        }
                    }
            }
        }

    override fun exists(id: Uuid): Boolean =
        try {
            getById(id)
            true
        } catch (_: NoLocationExist) {
            false
        }

    override fun getAll(): List<Location> =
        withDatabaseHandling("listing locations") {
            dataSource.connection.use { conn ->
                conn
                    .prepareStatement(
                        """
                        SELECT lid, name, loc_type, parent_lid
                        FROM locations
                        """.trimIndent(),
                    ).use { stmt ->
                        stmt.executeQuery().use { rs ->
                            val result = mutableListOf<Location>()
                            while (rs.next()) {
                                result.plusAssign(mapLocation(rs))
                            }
                            result
                        }
                    }
            }
        }

    private fun mapLocation(rs: ResultSet): Location {
        val parentId = rs.getObject("parent_lid") as UUID?

        return Location(
            id = Uuid.parse(rs.getObject("lid", UUID::class.java).toString()),
            name = LocationName.of(rs.getString("name")),
            type = LocationType.of(rs.getString("loc_type")),
            parentId = parentId?.toString()?.let { Uuid.parse(it) },
        )
    }

    private fun <T> withDatabaseHandling(
        operation: String,
        block: () -> T,
    ): T =
        try {
            block()
        } catch (error: SQLException) {
            throw LocationsRepositoryDatabaseException("Database error while $operation.", error)
        }

    private fun toJavaUuid(value: Uuid): UUID = UUID.fromString(value.toString())
}
