package main.data.impl.jdbc

import main.data.impl.caches.HouseInfoCache
import main.data.interfaces.HouseRepository
import main.domain.house.House
import main.domain.house.Title
import main.errors.HousesRepositoryDatabaseException
import main.errors.NoHouseExist
import java.sql.ResultSet
import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class JdbcHouseRepository(
    private val dataSource: DataSource,
    private val cache: HouseInfoCache = HouseInfoCache(0),
) : HouseRepository {
    override fun create(value: House): House =
        withDatabaseHandling("creating house") {
            dataSource.connection.use { conn ->
                conn
                    .prepareStatement(
                        """
                        insert into houses (hid, uid, title, location, areasqmt, pricepernight, description)
                        values (?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                    ).use { stmt ->
                        stmt.setObject(1, toJavaUuid(value.id))
                        stmt.setObject(2, toJavaUuid(value.uid))
                        stmt.setString(3, value.title.value)
                        stmt.setObject(4, toJavaUuid(value.lid))
                        stmt.setInt(5, value.areaSqMt)
                        stmt.setDouble(6, value.pricePerNight)
                        stmt.setString(7, value.description)
                        stmt.executeUpdate()
                    }
            }
            value
        }

    override fun deleteById(key: Uuid) {
        withDatabaseHandling("deleting house") {
            dataSource.connection.use { conn ->
                conn.prepareStatement("delete from houses where hid = ?").use { stmt ->
                    stmt.setObject(1, toJavaUuid(key))
                    val deleted = stmt.executeUpdate()
                    if (deleted == 0) throw NoHouseExist("House not found.")
                }
            }
        }
        cache.removeById(key)
    }

    override fun clear() {
        withDatabaseHandling("clearing houses") {
            dataSource.connection.use { conn ->
                conn.prepareStatement("delete from houses").use { stmt ->
                    stmt.executeUpdate()
                }
            }
        }
        cache.clear()
    }

    override fun update(updated: House): House = save(updated)

    override fun save(value: House): House {
        val value =
            withDatabaseHandling("updating house") {
                getById(value.id)
                dataSource.connection.use { conn ->
                    conn
                        .prepareStatement(
                            """
                            update houses
                            set uid = ?, title = ?, location = ?, areasqmt = ?, pricepernight = ?, description = ?
                            where hid = ?
                            """.trimIndent(),
                        ).use { stmt ->
                            stmt.setObject(1, toJavaUuid(value.uid))
                            stmt.setString(2, value.title.value)
                            stmt.setObject(3, toJavaUuid(value.lid))
                            stmt.setInt(4, value.areaSqMt)
                            stmt.setDouble(5, value.pricePerNight)
                            stmt.setString(6, value.description)
                            stmt.setObject(7, toJavaUuid(value.id))
                            val updated = stmt.executeUpdate()
                            if (updated == 0) throw NoHouseExist("House not found.")
                        }
                }
                value
            }
        cache.put(value.id, value)
        return value
    }

    override fun getById(key: Uuid): House =
        cache.getById(key)
            ?: withDatabaseHandling("getting house by id") {
                dataSource.connection.use { conn ->
                    conn
                        .prepareStatement(
                            """
                            select hid, uid, title, location, areasqmt, pricepernight, description
                            from houses
                            where hid = ?
                            """.trimIndent(),
                        ).use { stmt ->
                            stmt.setObject(1, toJavaUuid(key))
                            stmt.executeQuery().use { rs ->
                                if (!rs.next()) throw NoHouseExist("House not found.")
                                mapHouse(rs).also { cache.put(key, it) }
                            }
                        }
                }
            }

    override fun getAll(): List<House> =
        withDatabaseHandling("listing houses") {
            dataSource.connection.use { conn ->
                conn
                    .prepareStatement(
                        """
                        select hid, uid, title, location, areasqmt, pricepernight, description
                        from houses
                        """.trimIndent(),
                    ).use { stmt ->
                        stmt.executeQuery().use { rs ->
                            val result = mutableListOf<House>()
                            while (rs.next()) {
                                result.plusAssign(mapHouse(rs))
                            }
                            result
                        }
                    }
            }
        }

    private fun mapHouse(rs: ResultSet): House =
        House(
            id = Uuid.parse(rs.getObject("hid", UUID::class.java).toString()),
            uid = Uuid.parse(rs.getObject("uid", UUID::class.java).toString()),
            title = Title.of(rs.getString("title")),
            lid = Uuid.parse(rs.getObject("location", UUID::class.java).toString()),
            areaSqMt = rs.getInt("areasqmt"),
            pricePerNight = rs.getDouble("pricepernight"),
            description = rs.getString("description"),
        )

    private fun <T> withDatabaseHandling(
        operation: String,
        block: () -> T,
    ): T =
        try {
            block()
        } catch (error: SQLException) {
            throw HousesRepositoryDatabaseException("Database error while $operation.", error)
        }

    private fun toJavaUuid(value: Uuid): UUID = UUID.fromString(value.toString())
}
