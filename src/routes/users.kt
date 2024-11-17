package hr.askzg.routes

import hr.askzg.db.Role
import hr.askzg.db.User
import hr.askzg.db.UserDefaultPage
import hr.askzg.security.AuthenticationException
import hr.askzg.service.UserService
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import util.PasswordHasher
import java.time.Year

fun Route.users() {

    route("user") {

        get("current") {
            call.respond(UserService.get(call.userData().id))
        }

        get {
            call.userData(Role.ADMIN)
            call.respond(UserService.getAll().filter { it.role == Role.USER }.sortedByDescending { it.lastActivity })
        }

        post {
            val username = call.receive<AddUserRequest>().username
            val password = PasswordHasher.hash(username + Year.now().value)
            call.respond(UserService.save(User().apply {
                this.username = username
                this.password = password
                this.role = Role.USER
                this.page = UserDefaultPage.MEMBERS
            }))
        }

        put("current/password") {
            val request = call.receive<ChangePasswordRequest>()
            val user = UserService.get(call.userData().id).takeIf {
                PasswordHasher.isPasswordValid(request.old, it.password)
            } ?: throw AuthenticationException()
            user.password = PasswordHasher.hash(request.new)
            UserService.save(user)
            call.respond(HttpStatusCode.OK)

        }

        put("current") {
            val user = UserService.get(call.userData().id)
            user.page = call.receive<ChangeUserPageRequest>().page
            call.respond(UserService.get(UserService.save(user).id!!))
        }

        delete("{id}") {
            call.userData(Role.ADMIN)
            UserService.delete(call.pathId())
            call.respond(HttpStatusCode.OK)
        }
    }
}

class AddUserRequest {
    lateinit var username: String
}

class ChangeUserPageRequest {
    lateinit var page: UserDefaultPage
}

class ChangePasswordRequest {
    lateinit var old: String
    lateinit var new: String
}