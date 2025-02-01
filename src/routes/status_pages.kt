package hr.askzg.routes

import hr.askzg.security.AuthenticationException
import hr.askzg.security.AuthorizationException
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import java.time.format.DateTimeParseException
import org.jetbrains.exposed.sql.exposedLogger

fun Routing.statusPages() {
    install(StatusPages) {
        exception<AuthenticationException> {
            call.respond(HttpStatusCode.Unauthorized)
        }
        exception<AuthorizationException> {
            call.respond(HttpStatusCode.Forbidden)
        }
        exception<NoSuchElementException> {
            call.respond(HttpStatusCode.NotFound)
        }
        listOf(
            UninitializedPropertyAccessException::class, KotlinNullPointerException::class,
            DateTimeParseException::class, IllegalStateException::class, IllegalArgumentException::class
        ).forEach { exceptionClass ->
            exception(exceptionClass.java) { cause ->
                exposedLogger.error("Bad Request: ${exceptionClass.simpleName}", cause)
                call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
}