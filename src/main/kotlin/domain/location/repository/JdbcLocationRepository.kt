package domain.location.repository

import domain.house.House
import domain.house.Title
import domain.location.Location
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class JdbcLocationRepository(
    private val dataSource: DataSource,
    ) : LocationRepository {

    override fun create(location: Location): Location {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                insert into houses (hid, uid, title, location, areasqmt, pricepernight, description)
                values (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { stmt ->
                stmt.setObject(1, UUID.fromString(house.id.toString()))
                stmt.setObject(2, UUID.fromString(house.uid.toString()))
                stmt.setString(3, house.title.value)
                stmt.setString(4, house.location)
                stmt.setInt(5, house.areaSqMt)
                stmt.setDouble(6, house.pricePerNight)
                stmt.setString(7, house.description)
                stmt.executeUpdate()
            }
        }
        return house
    }

    override fun delete(id: String) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("delete from houses where hid = ?").use { stmt ->
                stmt.setObject(1, UUID.fromString(id.trim()))
                stmt.executeUpdate()
            }
        }
    }


    override fun update(location: Location): se {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                update houses
                set uid = ?, title = ?, location = ?, areasqmt = ?, pricepernight = ?, description = ?
                where hid = ?
                """.trimIndent()
            ).use { stmt ->
                stmt.setObject(1, UUID.fromString(house.uid.toString()))
                stmt.setString(2, house.title.value)
                stmt.setString(3, house.location)
                stmt.setInt(4, house.areaSqMt)
                stmt.setDouble(5, house.pricePerNight)
                stmt.setString(6, house.description)
                stmt.setObject(7, UUID.fromString(house.id.toString()))
                stmt.executeUpdate()
            }
        }
        return house
    }

    override fun getById(id: String): House? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                select hid, uid, title, location, areasqmt, pricepernight, description
                from houses
                where hid = ?
                """.trimIndent()
            ).use { stmt ->
                stmt.setObject(1, UUID.fromString(id.trim()))
                stmt.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return mapHouse(rs)
                }
            }
        }
    }

    override fun getAll(): List<House> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                select hid, uid, title, location, areasqmt, pricepernight, description
                from houses
                """.trimIndent()
            ).use { stmt ->
                stmt.executeQuery().use { rs ->
                    val result = mutableListOf<House>()
                    while (rs.next()) {
                        result += mapHouse(rs)
                    }
                    return result
                }
            }
        }
    }

    private fun mapHouse(rs: ResultSet): House {
        return House(
            id = Uuid.parse(rs.getObject("hid", UUID::class.java).toString()),
            uid = Uuid.parse(rs.getObject("uid", UUID::class.java).toString()),
            title = Title.of(rs.getString("title")),
            location = rs.getString("location"),
            areaSqMt = rs.getInt("areasqmt"),
            pricePerNight = rs.getDouble("pricepernight"),
            description = rs.getString("description"),
        )
    }
}
