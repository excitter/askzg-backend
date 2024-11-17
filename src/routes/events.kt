package hr.askzg.routes

import hr.askzg.service.EventService
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*

fun Route.events() {
    route("event") {

        get {
            call.respond(EventService.getByYear(call.year()))
        }
        get("{id}") {
            call.respond(EventService.get(call.pathId()))
        }
        post {
            call.respond(EventService.save(call.forAdd()))
        }
        put {
            call.respond(EventService.save(call.forUpdate()))
        }
        put("paid") {
            val request = call.receive<TogglePaidRequest>()
            EventService.togglePaid(request.memberId!!, request.eventId!!)
            call.respond(HttpStatusCode.OK)
        }
        delete("{id}") {
            EventService.delete(call.pathId())
            call.respond(HttpStatusCode.OK)
        }
    }
}

class TogglePaidRequest {
    var memberId: Int? = null
    var eventId: Int? = null
}