package hr.askzg.service

import hr.askzg.db.*
import hr.askzg.routes.MemberParticipationData
import hr.askzg.routes.MemberStatisticsRequest
import hr.askzg.util.asLocalDate
import hr.askzg.util.asMap
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Year

object StatisticsService {

    fun getTrainingStatistics(year: Int): Map<Event, List<Member>> = transaction {
        val events = EventService.getByYear(year).filter { it.type == EventType.TRAINING && it.includeInStatistics }
        if (events.isEmpty()) return@transaction mapOf()
        val members = MemberService.getAll().asMap { it.id to it }
        val participations = EventParticipations.select {
            EventParticipations.type eq ParticipationType.ATTENDED and (EventParticipations.event inList events.ids())
        }.map(EventParticipations).groupBy { it.eventId }
        events.asMap { event ->
            event to (participations[event.id] ?: listOf()).map { members.getValue(it.memberId) }
        }
    }

    fun getMemberStatistics(request: MemberStatisticsRequest): Map<Year, List<MemberParticipationData>> = transaction {
        val members = MemberService.getMembersTemporalData().filter { request.containsMember(it.member.id!!) }
        request.years.asMap { year ->
            val events = EventService.getByYear(year.value).filter { request.containsEvent(it.type) && it.includeInStatistics }.sortedBy { it.date }
            if (events.isEmpty()) return@asMap year to listOf<MemberParticipationData>()
            val filteredMembers = members.filter { it.wasActiveInYear(year) }
            val participation = EventParticipations
                .select { EventParticipations.event inList events.ids() }.map(EventParticipations)
                .groupBy { it.memberId }
            year to filteredMembers.asSequence().map { data ->
                val memberParticipations = (participation[data.member.id] ?: listOf()).map { it.eventId to it.type }.toMap()
                val attended = events.filter { memberParticipations[it.id!!] == ParticipationType.ATTENDED }
                val unable = events.filter { e ->
                    memberParticipations[e.id!!] == ParticipationType.UNABLE_TO_ATTEND || !data.couldAttendEvent(e.date.asLocalDate())
                }
                val missed =
                    events.filter { e -> memberParticipations[e.id!!] == null && data.couldAttendEvent(e.date.asLocalDate()) }
                MemberParticipationData(data, attended, unable, missed)
            }.toList()
        }
    }
}