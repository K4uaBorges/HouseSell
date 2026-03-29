package main.domain_model.user

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class User(
    val id: Uuid,     // public identifier
    val name: Name,
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


@JvmInline
value class Name private constructor(val value: String){
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
