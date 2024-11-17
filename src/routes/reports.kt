package hr.askzg.routes

import hr.askzg.db.Event
import hr.askzg.db.EventParticipation
import hr.askzg.db.Member
import hr.askzg.db.ParticipationType
import hr.askzg.service.ReportService
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.util.*

fun Route.reports() {

    route("report") {
        get("membership") {
            val report = ReportService.getMembershipReport(Year.of(call.year()), false)
                .map {
                    MemberMembershipReport().apply {
                        this.member = it.member
                        this.paidMonths = it.paidMonths.map { m -> m.value }
                        this.owedMonths = it.unpaidMonths.map { m -> m.value }
                        this.debt = it.debt
                    }
                }
            call.respond(report)
        }
        get("event") {
            val report = ReportService.getEventReport(Year.of(call.year()))
            val data = EventReportData().apply {
                members = report.members
                participations = report.eventParticipation.map { ep ->
                    EventReportParticipationData().apply {
                        this.event = ep.key
                        this.paidMemberIds = ep.value.filter { it.paid }.map { it.memberId }
                        this.unpaidMemberIds = ep.value.filter { !it.paid }.map { it.memberId }
                    }
                }
            }
            call.respond(data)
        }
    }
}

data class MemberMembershipData(
    val member: Member,
    val year: Int,
    val paidMonths: Set<Month>,
    val unpaidMonths: Set<Month>
) {
    val owedMonths: Set<Month> = when (year.compareTo(Year.now().value)) {
        -1 -> unpaidMonths
        0 -> unpaidMonths.filter { it < LocalDate.now().month }.toSortedSet()
        else -> setOf()
    }
    val debt: Int = owedMonths.size * member.membership
}

data class EventReport(val members: List<Member>, val eventParticipation: Map<Event, List<EventParticipation>>)

data class MemberParticipationData(
    val member: MemberTemporalData,
    val attended: List<Event>,
    val unableToAttend: List<Event>,
    val missed: List<Event>
) {
    val percentage = (100.0 * attended.size / (attended.size + missed.size)).toInt()

    fun asParticipationMap(): Map<Event, ParticipationType> {
        val map = TreeMap<Event, ParticipationType>(Comparator<Event> { o1, o2 -> o1.date.compareTo(o2.date) })
        attended.forEach { map[it] = ParticipationType.ATTENDED }
        unableToAttend.forEach { map[it] = ParticipationType.UNABLE_TO_ATTEND }
        missed.forEach { map[it] = ParticipationType.NOT_ATTENDED }
        return map
    }

    fun hasAttended(event: Event) = attended.contains(event)
}

class MemberMembershipReport {
    lateinit var member: Member
    var paidMonths: List<Int> = listOf()
    var owedMonths: List<Int> = listOf()
    var debt: Int = 0
}

class EventReportData {
    var members: List<Member> = listOf()
    var participations: List<EventReportParticipationData> = listOf()
}

class EventReportParticipationData {
    lateinit var event: Event
    var paidMemberIds: List<Int> = listOf()
    var unpaidMemberIds: List<Int> = listOf()

}