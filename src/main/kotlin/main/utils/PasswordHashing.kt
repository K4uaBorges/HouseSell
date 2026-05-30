package main.utils

import main.domain.user.Password
import java.security.MessageDigest

fun hashPassword(password: Password): String {
    val digest = MessageDigest.getInstance("SHA-512").digest(password.value.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { byte -> "%02x".format(byte) }
}

fun hashPassword(raw: String): String = hashPassword(Password.of(raw))
