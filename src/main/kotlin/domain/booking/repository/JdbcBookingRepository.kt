package domain.booking.repository

import domain.booking.Booking
import domain.booking.Date
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class JdbcBookingRepository(
    private val dataSource: DataSource,
) : BookingRepository {

    override fun create(booking: Booking): Booking {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                insert into booking (id, hid, uid, start_date, end_date)
                values (?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { stmt ->
                stmt.setObject(1, UUID.fromString(booking.id.toString()))
                stmt.setObject(2, UUID.fromString(booking.hid))
                stmt.setObject(3, UUID.fromString(booking.uid.toString()))
                stmt.setObject(4, booking.startDate.value)
                stmt.setObject(5, booking.endDate.value)
                stmt.executeUpdate()
            }
        }
        return booking
    }

    override fun getById(id: String): Booking? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                select id, hid, uid, start_date, end_date
                from booking
                where id = ?
                """.trimIndent()
            ).use { stmt ->
                stmt.setObject(1, UUID.fromString(id.trim()))
                stmt.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return mapBooking(rs)
                }
            }
        }
    }

    override fun getAll(): List<Booking> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                select id, hid, uid, start_date, end_date
                from booking
                """.trimIndent()
            ).use { stmt ->
                stmt.executeQuery().use { rs ->
                    val result = mutableListOf<Booking>()
                    while (rs.next()) {
                        result += mapBooking(rs)
                    }
                    return result
                }
            }
        }
    }

    override fun delete(booking: Booking): Booking? {
        val existing = getById(booking.id.toString()) ?: return null

        dataSource.connection.use { conn ->
            conn.prepareStatement("delete from booking where id = ?").use { stmt ->
                stmt.setObject(1, UUID.fromString(booking.id.toString()))
                stmt.executeUpdate()
            }
        }

        return existing
    }

    private fun mapBooking(rs: ResultSet): Booking {
        val startDate = rs.getObject("start_date", LocalDate::class.java)
        val endDate = rs.getObject("end_date", LocalDate::class.java)

        return Booking(
            id = Uuid.parse(rs.getObject("id", UUID::class.java).toString()),
            hid = rs.getObject("hid", UUID::class.java).toString(),
            uid = Uuid.parse(rs.getObject("uid", UUID::class.java).toString()),
            startDate = Date.from(startDate),
            endDate = Date.from(endDate),
        )
    }
}
