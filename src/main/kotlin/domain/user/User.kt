package domain.user

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class User(
    val id: Uuid,     // public identifier
    val name: String,
    val email: Email,
    val token: Uuid,  // auth credential
)

@JvmInline
value class Email private constructor(val value: String){
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


