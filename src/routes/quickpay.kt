package hr.askzg.routes

import hr.askzg.service.QuickPayService
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.quickpay() {

    route("quickpay") {
        post("member") {
            val request = call.receive<QuickPay>()
            call.respond(QuickPayService.pay(request.memberId!!, request.year!!, request.membershipMonths, request.refractionIds, request.eventIds))
        }
    }

}

class QuickPay {
    var year: Int? = null
    var memberId: Int? = null
    var membershipMonths: List<Int> = listOf()
    var refractionIds: List<Int> = listOf()
    var eventIds: List<Int> = listOf()
}
