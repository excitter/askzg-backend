package hr.askzg.service

import hr.askzg.db.*
import hr.askzg.util.asMap
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import util.RecordUtil
import java.math.BigDecimal


fun calculateExpense(event: Event, ts: DateTime): Payment {
    val totalParticipants = event.participation.size
    val eventPrice = event.price ?: 0
    val totalCost = - BigDecimal(totalParticipants * eventPrice)
    val info = "DOGAĐAJ trošak - ${event.name}"

    return Payment().apply {
        amount = totalCost
        date = ts
        comment = info
        membershipId = null
        eventParticipationId = null
        productParticipationId = null
        timestamp= ts.millis
        canEdit = false
        transientExpense = true
    }
}

fun memberPaidShareOfEvent(event: Event, member: Member, ts: DateTime): Payment {
    val memberSlice = event.price ?: 0
    val info = "${event.name} - ${member.name}"
    return Payment().apply{
        amount = BigDecimal(memberSlice)
        date = ts
        comment = info
        membershipId = null
        productParticipationId = null
        timestamp = ts.millis
        canEdit = false
    }
}

object EventService : BasicService<Event, Events>(Events) {

    override fun get(id: ID) = transaction {
        val event = super.get(id)
        event.participation = EventParticipations.select { EventParticipations.event eq id }.map(EventParticipations)
        event
    }

    fun getByYear(year: Int) = transaction {
        val from = DateTime(year, 1, 1, 0, 0)
        val to = from.plusYears(1).minusSeconds(1)
        val events = Events.select { Events.date.between(from, to) }.orderBy(Events.date, SortOrder.DESC).map(Events)
        val participationMap = EventParticipations
            .select { EventParticipations.event inList events.map { it.id!! } }
            .map(EventParticipations).groupBy { it.eventId }
        events.forEach {
            it.participation = participationMap[it.id!!] ?: listOf()
        }
        events
    }

    private fun clearInherentExpenses(id: Int) {
        val oldExpenses = EventExpenses.select { EventExpenses.event eq id and(EventExpenses.autoCalculated eq true)}.map(EventExpenses)
        Payments.deleteWhere { Payments.id inList oldExpenses.map { EntityID<Int>(it.paymentId, Payments) } }
    }

    // NOTE: we explicitly do not delete payments, since this is the responsibility of the user
    override fun delete(id: Int) = transaction {
        clearInherentExpenses(id)
        super.delete(id)
    }

    override fun save(entity: Event) = if (entity.id == null) add(entity) else update(entity)

    private fun getConcurrentEvents(event: Event): List<Event> = transaction {
        // might need to geo index this
        // before | after
        Events.select{ not(Events.endDate less(event.date)) and(not(Events.date greater(event.endDate))) }
            .map(Events).filter { it.id != event.id }
            .toList()
    }

    private fun addInherentExpense(event: Event, eId: Int, now: DateTime) = transaction {
        if (event.price != null && event.price != 0) {
            val cost = calculateExpense(event, now)
            val savedCost = PaymentService.save(cost)
            val expense = EventExpense().apply {
                eventId = eId
                paymentId = savedCost.id!!
                autoCalculated = true
            }
            EventExpenses.insert {
                mapInsert(it, expense)
            }
        }
    }

    private fun addParticipations(eps: Iterable<EventParticipation>) {
        for(ep in eps) {
            if (ep.id == null) {
                EventParticipations.insert { mapInsert(it, ep) }
            } else {
                EventParticipations.update({ EventParticipations.id eq ep.id!! }) {
                    mapUpdate(it, ep)
                }
            }
        }
    }

    private fun updateConcurrentAttendance(event: Event, deleted: List<EventParticipation>) {
        // every user whos participation in this event is ATTENDED
        // will have their participation in the concurrent events set to UNABLE_TO_ATTEND
        exposedLogger.debug("Updating for: ${event.id} ${event.name}")

        val concurrentEvents: Set<Int> = getConcurrentEvents(event).map { it.id!! }.toSet()
        if (concurrentEvents.size == 0) return
        exposedLogger.debug("Concurrent events: ${concurrentEvents}")

        val attendingMemberIDs: Set<Int> = event.participation
            .filter { it.type == ParticipationType.ATTENDED }
            .map { it.memberId }
            .toSet()
        exposedLogger.debug("Attending members: ${attendingMemberIDs}")

        val affectedParticipations = EventParticipations
            .select{ EventParticipations.member inList(attendingMemberIDs) and(EventParticipations.event inList(concurrentEvents))}
            .map(EventParticipations)
        exposedLogger.debug("Affected participations: $affectedParticipations")

        for(affected in affectedParticipations) {
            EventParticipations.update( { EventParticipations.id eq(affected.id) }) {
                affected.type = ParticipationType.UNABLE_TO_ATTEND
                mapUpdate(it, affected)
            }
        }

        val availableMemberIDs: Set<Int> = deleted.map { it.memberId }.toSet()
        EventParticipations
            .deleteWhere{ EventParticipations.member inList(availableMemberIDs) and(EventParticipations.event inList(concurrentEvents)) }
    }

    private fun add(event: Event) = transaction {
        val now = DateTime.now()
        event.validate()
        val id = Events.insertAndGetId {
            mapInsert(it, event)
        }.value
        event.id = id
        updateConcurrentAttendance(event, listOf())
        addInherentExpense(event, id, now)
        addParticipations(event.participation.map { ep ->
            ep.eventId = id
            ep
        })
        get(id)
    }

    private fun update(event: Event) = transaction {
        event.validate()
        val now = DateTime.now()
        val id = event.id!!

        clearInherentExpenses(id)

        Events.update({ Events.id eq id }) {
            mapUpdate(it, event)
        }
        val previousParticipations = EventParticipations.select { EventParticipations.event eq id }.map(EventParticipations)
            .asMap { it.id!! to it }


        // .first are the updated/created EPs, .second are to be deleted
        val result = RecordUtil.analyzeEntities(previousParticipations.values, event.participation)
        updateConcurrentAttendance(event, result.second)

        result.first.forEach {
            it.eventId = id
            it.paid = previousParticipations[it.id]?.paid ?: false
        }

        addParticipations(result.first)
        addInherentExpense(event, id, now)

        EventParticipations.deleteWhere { EventParticipations.id inList result.second.map { it.id!! } }

        get(id)
    }

    fun togglePaid(memberId: ID, eventId: ID) = transaction {
        val participation =
            EventParticipations.select { EventParticipations.member eq memberId and (EventParticipations.event eq eventId) }
                .single().map(EventParticipations)
        val event = get(eventId)
        val member = MemberService.get(participation.memberId)

        val event_price = event.price ?: 0
        if (event_price != 0) {
            if (participation.paid) {
                // toggle off
                Payments.deleteWhere { Payments.eventParticipation eq EntityID<Int>(participation.id!!, EventParticipations) }
            } else {
                // toggle on
                PaymentService.save(Payment().apply {
                    amount = event_price.toBigDecimal().setScale(2)
                    date = DateTime.now()
                    comment = "UDIO ${member.name} - ${event.name}"
                    eventParticipationId = participation.id
                    transientExpense = true
                })
            }
        }
        EventParticipations.update({ EventParticipations.id eq participation.id }) {
            it[paid] = !participation.paid
        }
        Unit
    }
}