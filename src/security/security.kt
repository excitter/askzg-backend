package hr.askzg.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import hr.askzg.db.ID
import hr.askzg.db.Role
import hr.askzg.db.User
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.Principal
import io.ktor.auth.jwt.jwt

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
class UserData(val id: ID, val role: Role) : Principal

class SimpleJWT(secret: String) {
    private val algorithm = Algorithm.HMAC256(secret)
    val verifier: JWTVerifier = JWT.require(algorithm).build()
    fun sign(user: User): String = JWT.create()
        .withClaim("id", user.id!!)
        .withClaim("role", user.role.name)
        .sign(algorithm)
}

fun Application.setupSecurity(verifier: JWTVerifier) {
    install(Authentication) {
        jwt {
            verifier(verifier)
            validate {
                UserData(
                    it.payload.getClaim("id").asInt(), enumValueOf(it.payload.getClaim("role").asString())
                )
            }
        }
    }
}