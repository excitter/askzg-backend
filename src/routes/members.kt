package hr.askzg.routes

import hr.askzg.db.Member
import hr.askzg.db.MemberStatusPeriod
import hr.askzg.df
import hr.askzg.service.MemberService
import hr.askzg.util.asLocalDate
import hr.askzg.util.memberNickComparator
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.temporal.ChronoUnit
import java.util.*

fun Route.members() {

    route("member") {

        get {
            call.respond(MemberService.getAll().sortedWith(memberNickComparator))
        }

        get("{id}") {
            val data = MemberService.getMemberTemporalData(call.pathId())
            call.respond(MemberExtendedData().apply {
                member = data.member
                periods = data.periods
            })
        }

        get("attend-date") {
            val date = df.parseDateTime(call.request.queryParameters["date"]!!).asLocalDate()
            val data = MemberService.getMembersTemporalData().filter { it.couldAttendEvent(date) }
            call.respond(data.map { it.member }.sortedWith(memberNickComparator))
        }

        post {
            val data = call.receive<MemberExtendedData>()
            call.respond(MemberService.save(data.member, data.periods))
        }

        put {
            val data = call.receive<MemberExtendedData>()
            call.respond(MemberService.save(data.member, data.periods))
        }
    }
}

class MemberExtendedData {
    lateinit var member: Member
    var periods: List<MemberStatusPeriod> = listOf()
}


data class MemberTemporalData(val member: Member, val periods: List<MemberStatusPeriod>) {

    private val intervals = ArrayList<ClosedRange<LocalDate>>()
    private val yearIntervals: List<ClosedRange<Int>>

    init {
        val p = periods.sortedBy { it.start }
        (0 until p.size - 1).forEach { idx ->
            if (p[idx].status.paysMembership)
                intervals.add(p[idx].start.asLocalDate()..p[idx + 1].start.minusDays(1).asLocalDate())
        }
        with(p.last()) {
            if (status.paysMembership) intervals.add(start.asLocalDate()..LocalDate.MAX.minusDays(1))
        }
        yearIntervals = intervals.map { it.start.year..it.endInclusive.year }
    }

    fun getDueMemberships(year: Year): Set<Month> {
        var temp = LocalDate.of(year.value, 1, 1)
        val end = LocalDate.of(year.value, 12, 31)
        val result = TreeSet<Month>()
        while (temp <= end) {
            temp = if (intervals.any { temp in it }) {
                result.add(temp.month)
                temp.withDayOfMonth(1).plusMonths(1)
            } else
                temp.plusDays(1)
        }
        return result
    }

    fun couldAttendEvent(date: LocalDate): Boolean = intervals.any { date in it }

    fun wasActiveInYear(year: Year): Boolean = yearIntervals.any { year.value in it }

    fun getActiveDays(): Int {
        val now = LocalDate.now()
        return intervals.map {
            ChronoUnit.DAYS.between(it.start, minOf(now, it.endInclusive)).toInt()
        }.sum()
    }
}