package hr.askzg.routes

import hr.askzg.db.User
import hr.askzg.security.AuthenticationException
import hr.askzg.service.UserService
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import util.PasswordHasher

fun Route.authentication(signer: (User) -> String) {

    route("login") {
        post {
            val loginRequest = call.receive<LoginRequest>()
            val user = UserService.getByUsername(loginRequest.username).takeIf {
                PasswordHasher.isPasswordValid(loginRequest.password, it.password)
            } ?: throw AuthenticationException()
            call.respond(mapOf("token" to signer(user)))
        }
    }
}

class LoginRequest {
    lateinit var username: String
    lateinit var password: String
}