package main.data.impl.jdbc

import main.data.interfaces.BookingRepository
import main.domain_model.booking.Booking
import main.domain_model.booking.Date
import main.errors.BookingsRepositoryDatabaseException
import main.errors.NoBookingExist
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*
import javax.sql.DataSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class JdbcBookingRepository(
    private val dataSource: DataSource,
) : BookingRepository {
    override fun create(value: Booking): Booking =
        withDatabaseHandling("creating booking") {
            dataSource.connection.use { conn ->
                conn
                    .prepareStatement(
                        """
                        insert into booking (id, hid, uid, start_date, end_date)
                        values (?, ?, ?, ?, ?)
                        """.trimIndent(),
                    ).use { stmt ->
                        stmt.setObject(1, toJavaUuid(value.id))
                        stmt.setObject(2, toJavaUuid(value.hid))
                        stmt.setObject(3, toJavaUuid(value.uid))
                        stmt.setDate(4, value.startDate.value)
                        stmt.setDate(5, value.endDate.value)
                        stmt.executeUpdate()
                    }
            }
            value
        }

    override fun save(value: Booking): Booking = update(value)

    override fun update(updated: Booking): Booking =
        withDatabaseHandling("updating booking") {
            getById(updated.id)
            dataSource.connection.use { conn ->
                conn
                    .prepareStatement(
                        """
                        update booking
                        set hid = ?, uid = ?, start_date = ?, end_date = ?
                        where id = ?
                        """.trimIndent(),
                    ).use { stmt ->
                        stmt.setObject(1, toJavaUuid(updated.hid))
                        stmt.setObject(2, toJavaUuid(updated.uid))
                        stmt.setDate(3, updated.startDate.value)
                        stmt.setDate(4, updated.endDate.value)
                        stmt.setObject(5, toJavaUuid(updated.id))
                        val rowsUpdated = stmt.executeUpdate()
                        if (rowsUpdated == 0) throw NoBookingExist("Booking not found.")
                    }
            }
            updated
        }

    override fun getById(key: Uuid): Booking =
        withDatabaseHandling("getting booking by id") {
            dataSource.connection.use { conn ->
                conn
                    .prepareStatement(
                        """
                        select id, hid, uid, start_date, end_date
                        from booking
                        where id = ?
                        """.trimIndent(),
                    ).use { stmt ->
                        stmt.setObject(1, toJavaUuid(key))
                        stmt.executeQuery().use { rs ->
                            if (!rs.next()) throw NoBookingExist("Booking not found.")
                            mapBooking(rs)
                        }
                    }
            }
        }

    override fun getByUserId(uid: Uuid): List<Booking> =
        withDatabaseHandling("listing bookings by user") {
            dataSource.connection.use { conn ->
                conn
                    .prepareStatement(
                        """
                        select id, hid, uid, start_date, end_date
                        from booking
                        where uid = ?
                        order by start_date
                        """.trimIndent(),
                    ).use { stmt ->
                        stmt.setObject(1, toJavaUuid(uid))
                        stmt.executeQuery().use { rs ->
                            val result = mutableListOf<Booking>()
                            while (rs.next()) {
                                result.plusAssign(mapBooking(rs))
                            }
                            result
                        }
                    }
            }
        }

    override fun getAll(): List<Booking> =
        withDatabaseHandling("listing bookings") {
            dataSource.connection.use { conn ->
                conn
                    .prepareStatement(
                        """
                        select id, hid, uid, start_date, end_date
                        from booking
                        """.trimIndent(),
                    ).use { stmt ->
                        stmt.executeQuery().use { rs ->
                            val result = mutableListOf<Booking>()
                            while (rs.next()) {
                                result.plusAssign(mapBooking(rs))
                            }
                            result
                        }
                    }
            }
        }

    override fun deleteById(key: Uuid) {
        withDatabaseHandling("deleting booking") {
            dataSource.connection.use { conn ->
                conn.prepareStatement("delete from booking where id = ?").use { stmt ->
                    stmt.setObject(1, toJavaUuid(key))
                    val deleted = stmt.executeUpdate()
                    if (deleted == 0) throw NoBookingExist("Booking not found.")
                }
            }
        }
    }

    override fun clear() {
        withDatabaseHandling("clearing bookings") {
            dataSource.connection.use { conn ->
                conn.prepareStatement("delete from booking").use { stmt ->
                    stmt.executeUpdate()
                }
            }
        }
    }

    private fun mapBooking(rs: ResultSet): Booking {
        val startDate = rs.getObject("start_date", java.sql.Date::class.java)
        val endDate = rs.getObject("end_date", java.sql.Date::class.java)

        return Booking(
            id = Uuid.parse(rs.getObject("id", UUID::class.java).toString()),
            hid = Uuid.parse(rs.getObject("hid", UUID::class.java).toString()),
            uid = Uuid.parse(rs.getObject("uid", UUID::class.java).toString()),
            startDate = Date.from(startDate),
            endDate = Date.from(endDate),
        )
    }

    private fun <T> withDatabaseHandling(operation: String, block: () -> T): T =
        try {
            block()
        } catch (error: SQLException) {
            throw BookingsRepositoryDatabaseException("Database error while $operation.", error)
        }

    private fun toJavaUuid(value: Uuid): UUID = UUID.fromString(value.toString())
}
