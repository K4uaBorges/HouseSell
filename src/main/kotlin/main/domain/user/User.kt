package main.domain.user
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class UserRole {
    ADMIN,
    USER,
}

@OptIn(ExperimentalUuidApi::class)
data class User(
    // public identifier
    val id: Uuid,
    val name: Name,
    val email: Email,
    // auth credential
    val token: Uuid,
    val role: UserRole = UserRole.USER,
    val passwordHash: String = "",
)

@JvmInline
value class Email private constructor(val value: String) {
    companion object {
        fun of(raw: String): Email {
            val s = raw.trim()
            require(s.isNotEmpty()) { "Email Empty" }
            require('@' in s) { "Invalid email" }
            require(!s.contains(' ')) { "Invalid Email, you can't spaces" }
            return Email(s)
        }
    }

    override fun toString(): String = value
}

@JvmInline
value class Name private constructor(val value: String) {
    companion object {
        fun of(raw: String): Name {
            val s = raw.trim()
            require(s.isNotEmpty()) { "Invalid" }
            require(s.length in 3..50) { "The name need between 3 and 50" }
            return Name(s)
        }
    }

    override fun toString(): String = value
}

@JvmInline
value class Password private constructor(val value: String) {
    companion object {
        fun of(raw: String): Password {
            require(raw.length >= 8) { "Password must have at least 8 characters." }
            require(raw.any { it.isUpperCase() }) { "Password must include an uppercase letter." }
            require(raw.any { it.isLowerCase() }) { "Password must include a lowercase letter." }
            require(raw.any { it.isDigit() }) { "Password must include a number." }
            return Password(raw)
        }
    }

    override fun toString(): String = value
}
