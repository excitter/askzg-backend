package hr.askzg.routes

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route

fun Route.check() {
    route("check") {
        get {
            call.respond(mapOf("running" to true))
        }
    }
}