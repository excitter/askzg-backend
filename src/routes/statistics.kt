package hr.askzg.routes

import hr.askzg.db.EventType
import hr.askzg.db.Member
import hr.askzg.db.Event
import hr.askzg.db.ParticipationType
import hr.askzg.ddf
import hr.askzg.service.MemberService
import hr.askzg.service.StatisticsService
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import org.joda.time.DateTime
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Year
import java.util.*

fun Route.statistics() {

    route("statistics") {
        route("training") {
            get("time") {
                val stats = StatisticsService.getTrainingStatistics(call.year())
                val data = stats.keys.sortedByDescending { e -> e.date }.map { event ->
                    TrainingStatisticsData().apply {
                        this.date = event.date
                        this.location = event.name
                        this.members = stats.getValue(event).map { it.name }.sorted()
                    }
                }
                call.respond(data)
            }

            get("member") {
                val y = Year.of(call.year())
                val request = MemberStatisticsRequest(setOf(), setOf(EventType.TRAINING), setOf(y))
                val data = StatisticsService.getMemberStatistics(request).getValue(y)
                    .filter { it.attended.size + it.missed.size > 0 }.map {
                        MemberStatisticsData().apply {
                            member = it.member.member
                            events = it.asParticipationMap().map { e ->
                                AttendanceData().apply {
                                    name = ddf.print(e.key.date) + " " + e.key.name
                                    type = e.value
                                }
                            }
                        }
                    }
                call.respond(data)
            }

            get("monthly") {
                val year = call.year()
                val now = LocalDate.now()
                val maxMonth = if (now.year == year) now.monthValue else 12
                val stats = StatisticsService.getTrainingStatistics(year)
                val data = (1..maxMonth).map { month ->
                    val monthly = stats.filter { it.key.date.monthOfYear == month }
                    val average = monthly.values.map { it.size }.average()
                    MonthlyTrainingStatistics().apply {
                        this.month = month
                        count = monthly.size
                        this.average = if (!average.isFinite()) BigDecimal.ZERO else average.toBigDecimal().setScale(
                            1,
                            RoundingMode.HALF_UP
                        )
                    }
                }
                call.respond(data)
            }
        }

        route("total") {

            get("statisticsV2") {
                val y = Year.of(call.year())
                val data = StatisticsService
                    .getStatisticsV2(y, setOf(EventType.TRAINING, EventType.EVENT, EventType.OTHER))
                call.respond(data)
            }

            get("member/{id}") {
                val id = call.pathId()
                val years = (2014..Year.now().value).map { Year.of(it) }.toSortedSet()
                val request = MemberStatisticsRequest(setOf(id), setOf(EventType.TRAINING, EventType.EVENT), years)
                val total = StatisticsService.getMemberStatistics(request)
                val participations = total.filter { it.value.isNotEmpty() }.map { e ->
                    val events = e.value[0]
                    MemberYearParticipation().apply {
                        this.year = e.key.value
                        this.attendedTrainings = events.attended.count { it.type == EventType.TRAINING }
                        this.numOfTrainings = attendedTrainings + events.missed.count { it.type == EventType.TRAINING }
                        this.attendedEvents = events.attended.count { it.type == EventType.EVENT }
                        this.numOfEvents = attendedEvents + events.missed.count { it.type == EventType.EVENT }
                    }
                }
                val member = MemberService.getMemberTemporalData(id)
                val stat = MemberTotalStatistics().apply {
                    this.member = MemberExtendedData().apply {
                        this.member = member.member
                        this.periods = member.periods
                    }
                    this.activeDays = member.getActiveDays()
                    this.participations = participations
                }
                call.respond(stat)
            }
        }
    }
}

data class MemberStatisticsRequest(val memberIds: Set<Int>, val eventTypes: Set<EventType>, val years: Set<Year>) {
    fun containsMember(id: Int) = memberIds.isEmpty() || id in memberIds
    fun containsEvent(type: EventType) = type in eventTypes
}

class TrainingStatisticsData {
    lateinit var date: DateTime
    lateinit var location: String
    var members: List<String> = listOf()
}

class MonthlyTrainingStatistics {
    var month: Int = 0
    var count: Int = 0
    var average: BigDecimal = BigDecimal.ZERO
}

class MemberStatisticsData {
    lateinit var member: Member
    var events: List<AttendanceData> = listOf()
}

class AttendanceData {
    lateinit var name: String
    lateinit var type: ParticipationType
}

class MemberYearParticipation {
    var year: Int = 0
    var attendedTrainings: Int = 0
    var numOfTrainings: Int = 0
    var attendedEvents: Int = 0
    var numOfEvents: Int = 0
}

class MemberTotalStatistics {
    var member: MemberExtendedData? = null
    var activeDays: Int = 0
    var participations: List<MemberYearParticipation> = listOf()
}

class EventCountsV2 {
    // event type breakdown per user
    var attended: Int = 0
    var didntAttend: Int = 0
    var couldntAttend: Int = 0
    var totalPct: Float? = null
    var possiblePct: Float? = null
}

class MemberEventStatisticV2 {
    var memberId: Int = 0
    var attendance: Map<EventType, EventCountsV2> = EnumMap(hr.askzg.db.EventType::class.java)
}

class EventBreakdownV2 {
    var eventId: Int = 0
    var attendedMemberIds: Set<Int> = setOf()
    var missedMemberIds: Set<Int> = setOf()
    var unableToAttendMemberIds: Set<Int> = setOf()
    var attendedPct: Float? = null
    var adjustedPct: Float? = null
}

class StatisticsV2 {
    var members: List<Member> = listOf()
    var events: List<Event> = listOf()
    var eventBreakdowns: Map<EventType, List<EventBreakdownV2>> = EnumMap(hr.askzg.db.EventType::class.java)
    var memberEventStatistics: List<MemberEventStatisticV2> = listOf()
}
