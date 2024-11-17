package hr.askzg.routes

import hr.askzg.db.Entity
import hr.askzg.db.ID
import hr.askzg.db.Role
import hr.askzg.security.AuthenticationException
import hr.askzg.security.UserData
import io.ktor.application.ApplicationCall
import io.ktor.auth.principal
import io.ktor.request.receive
import java.time.Year

suspend inline fun <reified T : Entity> ApplicationCall.forAdd(): T = receive<T>().apply { id = null }
suspend inline fun <reified T : Entity> ApplicationCall.forUpdate(): T = receive<T>().apply { id!! }
fun ApplicationCall.year(): Int = request.queryParameters["year"]?.toInt() ?: Year.now().value
fun ApplicationCall.userData(vararg roles: Role = Role.values()): UserData =
    principal<UserData>()?.takeIf { it.role in roles } ?: throw AuthenticationException()

fun ApplicationCall.pathId(): ID = parameters["id"]!!.toInt()
fun ApplicationCall.queryId(): ID = param("id")!!.toInt()
fun ApplicationCall.param(name: String): String? = request.queryParameters[name]
fun ApplicationCall.boolParam(name: String): Boolean = param(name)?.toBoolean() ?: false





