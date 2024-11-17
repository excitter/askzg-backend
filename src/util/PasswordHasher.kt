package util

import java.math.BigInteger
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec


object PasswordHasher {

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val SALT_BYTE_SIZE = 24
    private const val HASH_BYTE_SIZE = 24
    private const val PBKDF2_ITERATIONS = 4096
    private const val ITERATION_INDEX = 0
    private const val SALT_INDEX = 1
    private const val PBKDF2_INDEX = 2

    fun hash(password: String): String = createHash(password.toCharArray())

    fun isPasswordValid(password: String, correctHash: String): Boolean {
        return try {
            validatePassword(password.toCharArray(), correctHash)
        } catch (e: Exception) {
            false
        }
    }

    private fun createHash(password: CharArray): String {
        val salt = ByteArray(SALT_BYTE_SIZE)
        SecureRandom().nextBytes(salt)
        val hash = pbkdf2(password, salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE)
        return PBKDF2_ITERATIONS.toString() + ":" + toHex(salt) + ":" + toHex(hash)
    }

    private fun validatePassword(password: CharArray, correctHash: String): Boolean {
        val params = correctHash.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val iterations = Integer.parseInt(params[ITERATION_INDEX])
        val salt = fromHex(params[SALT_INDEX])
        val hash = fromHex(params[PBKDF2_INDEX])
        val testHash = pbkdf2(password, salt, iterations, hash.size)
        return slowEquals(hash, testHash)
    }

    private fun slowEquals(a: ByteArray, b: ByteArray): Boolean {
        var diff = a.size xor b.size
        var i = 0
        while (i < a.size && i < b.size) {
            diff = diff or (a[i].toInt() xor b[i].toInt())
            i++
        }
        return diff == 0
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, bytes: Int): ByteArray =
        SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(PBEKeySpec(password, salt, iterations, bytes * 8)).encoded

    private fun fromHex(hex: String): ByteArray {
        val binary = ByteArray(hex.length / 2)
        for (i in binary.indices) {
            binary[i] = Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16).toByte()
        }
        return binary
    }

    private fun toHex(array: ByteArray): String {
        val hex = BigInteger(1, array).toString(16)
        val paddingLength = array.size * 2 - hex.length
        if (paddingLength > 0) {
            return String.format("%0" + paddingLength + "d", 0) + hex
        }
        return hex
    }
}