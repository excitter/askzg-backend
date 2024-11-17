package hr.askzg.routes

import hr.askzg.service.RefractionService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.refractions() {

    route("refractions") {

        get("report") {
            call.respond(RefractionService.getReport(call.boolParam("onlyActive")))
        }

        get("report/member/{id}") {
            call.respond(RefractionService.getMemberRefractions(call.pathId()))
        }

        post {
            call.respond(RefractionService.save(call.forAdd()))
        }

        post("member/{id}/pay") {
            call.respond(RefractionService.pay(call.pathId()))
        }

        delete("{id}") {
            RefractionService.delete(call.pathId())
            call.respond(HttpStatusCode.OK)
        }

    }

}
