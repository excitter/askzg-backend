package hr.askzg.routes

import hr.askzg.db.EventType
import hr.askzg.db.Member
import hr.askzg.db.Payment
import hr.askzg.db.Status
import hr.askzg.pdf.*
import hr.askzg.service.*
import hr.askzg.util.memberNameComparator
import hr.askzg.util.yearRange
import io.ktor.application.call
import io.ktor.response.header
import io.ktor.response.respondOutputStream
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import org.joda.time.DateTime
import java.math.BigDecimal
import java.time.Year

fun Route.exports() {

    route("export") {

        get("members") {
            val filter = memberFilter(call.boolParam("member"), call.boolParam("recruit"), call.boolParam("inactive"))
            call.response.header("Content-Disposition", "attachment; filename=clanovi.pdf")
            call.respondOutputStream {
                MemberPdfCreator(MemberService.getAll().filter(filter).sortedWith(memberNameComparator)).streamTo(this)
            }
        }

        get("events") {
            val year = call.year()
            call.response.header("Content-Disposition", "attachment; filename=dogadaji-$year.pdf")
            call.respondOutputStream {
                EventPdfCreator(EventService.getByYear(year), year).streamTo(this)
            }
        }

        get("payments") {
            val year = call.year()
            val income = call.boolParam("income")
            val expense = call.boolParam("expense")
            val format = call.param("word")

            var title = "promet"
            if (income && !expense) title = "promet"
            if (!income && expense) title = "promet"

            val filter = paymentFilter(income, expense, format)
            val interval = yearRange(year, DateTime.now())
            val payments = PaymentService.getFiltered(interval.first, interval.second)
            val balance = PaymentService.getBalance(interval.first)

            call.response.header("Content-Disposition", "attachment; filename=$title-$year.pdf")
            call.respondOutputStream {
                PaymentPdfCreator(PaymentPdfData(payments, balance, interval, filter, title)).streamTo(this)
            }
        }

        get("report") { _ ->
            val year = call.year()
            val onlyActive = call.request.queryParameters["onlyActive"]?.toBoolean() ?: false
            val eventReport = ReportService.getEventReport(Year.of(year))
            val membershipReport = ReportService.getMembershipReport(Year.of(year), onlyActive).filter {
                it.paidMonths.size != 12 && (onlyActive || it.member.status != Status.INACTIVE || it.unpaidMonths.isNotEmpty())
            }
            call.response.header("Content-Disposition", "attachment; filename=izvjesce-$year.pdf")
            call.respondOutputStream {
                ReportPdfCreator(eventReport, membershipReport, year).streamTo(this)
            }
        }

        get("statistics") { _ ->
            // TODO: change this to a table export per event type such as statistics page
            val year = call.year()
            val onlyActive = call.request.queryParameters["income"]?.toBoolean() ?: false
            val request = MemberStatisticsRequest(setOf(), setOf(EventType.TRAINING), setOf(Year.of(year)))
            val memberStats = StatisticsService.getMemberStatistics(request).map { e ->
                e.key to e.value.filter { (!onlyActive || it.member.member.status != Status.INACTIVE) && it.attended.size + it.missed.size > 0 }
            }.toMap()[Year.of(year)] ?: listOf()
            call.response.header("Content-Disposition", "attachment; filename=statistika-$year.pdf")
            call.respondOutputStream {
                StatisticsPdfCreator(memberStats, EventService.getByYear(year).filter { it.includeInStatistics }, year).streamTo(this)
            }
        }

    }
}

fun memberFilter(member: Boolean, recruit: Boolean, inactive: Boolean): (Member) -> Boolean = {
    (member && it.status == Status.MEMBER) || (recruit && it.status == Status.RECRUIT) || (inactive && it.status == Status.INACTIVE)
}

fun paymentFilter(income: Boolean, expense: Boolean, word: String?): (Payment) -> Boolean = {
    (income && it.amount >= BigDecimal.ZERO || expense && it.amount < BigDecimal.ZERO) && (word == null || it.comment.toLowerCase().contains(word.toLowerCase()))
}
