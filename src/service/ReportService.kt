package hr.askzg.service

import hr.askzg.db.*
import hr.askzg.routes.EventReport
import hr.askzg.routes.MemberMembershipData
import hr.askzg.util.asMap
import hr.askzg.util.memberNickComparator
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Month
import java.time.Year

object ReportService {

    private val memberMembershipComparator = Comparator<MemberMembershipData> { o1, o2 ->
        memberNickComparator.compare(o1.member, o2.member)
    }

    fun getMembershipReport(year: Year, onlyActive: Boolean): List<MemberMembershipData> {
        val temporalData = MemberService.getMembersTemporalData().filter {
            if (!it.wasActiveInYear(year)) false else if (!onlyActive) true else it.member.status != Status.INACTIVE
        }
        val memberIds = temporalData.map { it.member.id!! }.toSet()
        val membershipMap = MembershipService.getMembershipsByYear(year.value)
            .filter { it.key.id in memberIds }.values.flatten()
            .groupBy({ mm -> mm.memberId }, { mm -> Month.of(mm.month) })
        return temporalData.map { data ->
            val dueMemberships = data.getDueMemberships(year)
            val paidMonths = Month.values().filter { month ->
                month !in dueMemberships || month in (membershipMap[data.member.id] ?: setOf<Month>())
            }.toSortedSet()
            val unpaidMonths = (dueMemberships - paidMonths).toSortedSet()
            MemberMembershipData(data.member, year.value, paidMonths, unpaidMonths)
        }.sortedWith(memberMembershipComparator)
    }

    fun getEventReport(year: Year) = transaction {
        val events = EventService.getByYear(year.value).filter { it.includeInStatistics && it.price != null && it.price!! > 0 }
        val eventParticipations =
            EventParticipations.select { EventParticipations.event inList events.ids() }.map(EventParticipations)
        val memberIds = eventParticipations.map { it.memberId }.toSet()
        val eventParticipationMap = eventParticipations.groupBy { it.eventId }
        val map = events.asMap { it to (eventParticipationMap[it.id!!] ?: listOf()) }
        val members = MemberService.getAll().filter { it.id!! in memberIds }
        EventReport(members, map)
    }


}