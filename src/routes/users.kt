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
import kotlin.random.Random

fun passwordGenerator(): String {
    val LENGTH = 16
    val charGroups = listOf('A'..'Z', 'a'..'z', '0'..'9').map { it.joinToString("") } + "!?{}~+\$&#@%"
    val allowed = charGroups.joinToString("") // All symbols above
    var password = "";
    charGroups.forEach {
        password += it.random()
    }
    val remaining = LENGTH - password.length
    for (i in 1..remaining) {
        password += allowed.random()
    }
    return password.toList().shuffled().joinToString("")
}

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
            val password = passwordGenerator()
            val passwordHash = PasswordHasher.hash(password)
            val created = UserService.save(User().apply {
                this.username = username
                this.password = passwordHash
                this.role = Role.USER
                this.page = UserDefaultPage.MEMBERS
            })
            call.respond(UserAddedResponse().apply {
                this.id = created.id!!
                this.username = username
                this.role = created.role.toString()
                this.page = created.page.toString()
                this.password = password
            })
        }

        put("current/password") {
            val request = call.receive<ChangePasswordRequest>()
            val user = UserService.get(call.userData().id).takeIf {
                PasswordHasher.isPasswordValid(request.old, it.password)
            } ?: throw AuthenticationException()
            if (request.new.length < 8) {
                call.respond(HttpStatusCode.BadRequest)
            } else {
                user.password = PasswordHasher.hash(request.new)
                UserService.save(user)
                call.respond(HttpStatusCode.OK)
            }
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

class UserAddedResponse {
    var id: Int = -1
    lateinit var username: String
    lateinit var page: String
    lateinit var role: String
    lateinit var password: String
}

class ChangeUserPageRequest {
    lateinit var page: UserDefaultPage
}

class ChangePasswordRequest {
    lateinit var old: String
    lateinit var new: String
}