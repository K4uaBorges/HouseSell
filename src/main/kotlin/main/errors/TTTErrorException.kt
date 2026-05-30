package main.errors

open class DomainErrorException(
    msg: String,
    cause: Throwable? = null,
) : IllegalArgumentException(msg, cause) {
    override fun toString(): String = message ?: "Unknown Error"
}

// ------------------------------------------------------------------- //

open class ServerErrorException(
    msg: String,
    cause: Throwable? = null,
) : RuntimeException(msg, cause) {
    override fun toString(): String = message ?: "Unknown Error"
}

open class RepositoryDatabaseException(
    msg: String,
    cause: Throwable? = null,
) : ServerErrorException(msg, cause)
// ------------------------------------------------------------------- //

class UnauthorizedException(
    message: String,
) : ServerErrorException(message)

// ------------------------------------------------------------------- //
class NoParentIdValid(
    msg: String,
) : DomainErrorException(msg)

class NoLocationValid(
    msg: String,
) : DomainErrorException(msg)

class NoLocationExist(
    msg: String,
) : DomainErrorException(msg)

class NoHouseExist(
    msg: String,
) : DomainErrorException(msg)

class NoBookingExist(
    msg: String,
) : DomainErrorException(msg)

class NoUserExist(
    msg: String,
) : DomainErrorException(msg)

class LidNotLocatityException(
    msg: String,
) : DomainErrorException(msg)

class DuplicateHouseException(
    msg: String,
) : DomainErrorException(msg)

// ------------------------------------------------------------------- //

class UsersRepositoryDatabaseException(
    msg: String,
    cause: Throwable,
) : RepositoryDatabaseException(msg, cause)

class HousesRepositoryDatabaseException(
    msg: String,
    cause: Throwable,
) : RepositoryDatabaseException(msg, cause)

class LocationsRepositoryDatabaseException(
    msg: String,
    cause: Throwable,
) : RepositoryDatabaseException(msg, cause)

class BookingsRepositoryDatabaseException(
    msg: String,
    cause: Throwable,
) : RepositoryDatabaseException(msg, cause)
