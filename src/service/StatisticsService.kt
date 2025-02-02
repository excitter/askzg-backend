package hr.askzg.service

import hr.askzg.db.*
import hr.askzg.routes.*
import hr.askzg.util.asLocalDate
import hr.askzg.util.asMap
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.exposedLogger
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Year
import java.util.EnumMap

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

    fun getStatisticsV2(year: Year, eventTypes: Set<EventType>) = transaction{
        val members = MemberService.getMembersTemporalData().filter { it.wasActiveInYear(year) }
        val eventsRaw = EventService.getByYear(year.value)
            .filter { it.id != null }
            .filter { it.type in eventTypes && it.includeInStatistics }
        val events = eventsRaw.map { Pair(it.id, it) }.toMap()

        val participations = EventParticipations
            .select { EventParticipations.event inList events.values.ids() }
            .map(EventParticipations)

        val participationPerMember = participations.groupBy { it.memberId }
        val participationPerEvent = participations.groupBy { it.eventId }
        val eventByType = events.values.groupBy { it.type }

        val eventBreakdownsCalculated = eventByType.map { (eventType, groupedEvents) ->
            eventType to groupedEvents.map { event ->
                val eventParticipations = participationPerEvent[event.id!!] ?: listOf()
                val groupedParticipations = eventParticipations.groupBy { it.type }
                val attendedIds = groupedParticipations[ParticipationType.ATTENDED]
                    ?.map {it.memberId }
                    ?.toSet()
                    ?: setOf()
                val unableIds = groupedParticipations[ParticipationType.UNABLE_TO_ATTEND]
                    ?.map {it.memberId }
                    ?.toSet()
                    ?: setOf()
                val missedIds = members.map { it.member.id!! }.filter { !attendedIds.contains(it) }.toSet()
                EventBreakdownV2().apply {
                    eventId = event.id!!
                    attendedMemberIds = attendedIds
                    missedMemberIds = missedIds
                    unableToAttendMemberIds = unableIds
                    if (members.isNotEmpty()) {
                        attendedPct = attendedIds.size.toFloat() / members.size.toFloat() * 100F
                    }
                    if((members.size - unableIds.size) != 0) {
                        adjustedPct = attendedIds.size.toFloat() / (members.size - unableIds.size).toFloat() * 100F
                    }
                }
            }
        }.toMap()

        val memberEventStatisticsCalculated = participationPerMember.map{(member, participations) ->
            val participationsByType = participations.groupBy { it.type }

            val attendedParticipations = participationsByType[ParticipationType.ATTENDED] ?: listOf()
            val couldntParticipations = participationsByType[ParticipationType.UNABLE_TO_ATTEND] ?: listOf()

            val attendedByType = attendedParticipations.map { events[it.eventId]!! }.groupBy { it.type }
            val couldnAttendByType = couldntParticipations.map { events[it.eventId]!! }.groupBy { it.type }

            val typeToAttendance = arrayOf(EventType.EVENT, EventType.TRAINING, EventType.OTHER)
                .map{et ->
                    val eventsCount = eventByType[et]?.size ?: 0
                    val memberAttended = attendedByType[et]?.size ?: 0
                    val memberCouldntAttended = couldnAttendByType[et]?.size ?: 0
                    if (eventsCount == 0){
                        et to EventCountsV2()
                    }
                    else {
                        et to EventCountsV2().apply {
                            attended = memberAttended
                            didntAttend = eventsCount - (memberAttended + memberCouldntAttended)
                            couldntAttend = memberCouldntAttended
                            totalPct =  memberAttended.toFloat() / eventsCount.toFloat() * 100
                            if (eventsCount != memberCouldntAttended) {
                                possiblePct = memberAttended.toFloat() / (eventsCount - memberCouldntAttended).toFloat() * 100
                            }
                        }
                    }
                }
                .toMap()
            MemberEventStatisticV2().apply{
                memberId = member
                attendance = typeToAttendance
                adjustedAttendance = EventCountsV2()
            }
        }

        StatisticsV2().apply result@{
            this@result.members = members.map { it.member }
            this@result.events = events.values.toList()
            eventBreakdowns = eventBreakdownsCalculated
            memberEventStatistics = memberEventStatisticsCalculated
        }
    }
}