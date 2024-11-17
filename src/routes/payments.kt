package hr.askzg.routes

import hr.askzg.db.Payment
import hr.askzg.service.PaymentService
import hr.askzg.util.yearRange
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.*
import org.joda.time.DateTime
import java.math.BigDecimal


fun Route.payments() {
    route("balance") {
        get {
            call.respond(mapOf("balance" to PaymentService.getBalance(DateTime.now())))
        }
    }

    route("payment") {
        get {
            val (from, to) = yearRange(call.year())
            val report = PaymentYearReport().apply {
                payments = PaymentService.getFiltered(from, to)
                startYearBalance = PaymentService.getBalance(from.minusSeconds(1))
            }
            call.respond(report)
        }
        get("{id}") {
            call.respond(PaymentService.get(call.pathId()))
        }
        post {
            call.respond(PaymentService.save(call.forAdd()))
        }
        put {
            call.respond(PaymentService.save(call.forUpdate()))
        }
        delete("{id}") {
            PaymentService.delete(call.pathId())
            call.respond(HttpStatusCode.OK)
        }

    }
}

class PaymentYearReport {
    var payments: List<Payment> = listOf()
    var startYearBalance: BigDecimal = BigDecimal("0.00")
}
