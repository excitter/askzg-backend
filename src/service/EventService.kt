package hr.askzg.service

import hr.askzg.db.*
import hr.askzg.util.asMap
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import util.RecordUtil

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

    override fun save(entity: Event) = if (entity.id == null) add(entity) else update(entity)

    private fun add(event: Event) = transaction {
        val id = Events.insertAndGetId {
            mapInsert(it, event)
        }.value
        event.participation.forEach { ep ->
            ep.eventId = id
            EventParticipations.insert {
                mapInsert(it, ep)
            }
        }
        get(id)
    }

    private fun update(event: Event) = transaction {
        val id = event.id!!
        Events.update({ Events.id eq id }) {
            mapUpdate(it, event)
        }
        val existing = EventParticipations.select { EventParticipations.event eq id }.map(EventParticipations)
            .asMap { it.id!! to it }
        val result = RecordUtil.analyzeEntities(existing.values, event.participation)
        result.first.forEach {
            it.eventId = id
            it.paid = existing[it.id]?.paid ?: false
        }
        result.first.forEach { ep ->
            if (ep.id == null) {
                EventParticipations.insert { mapInsert(it, ep) }
            } else {
                EventParticipations.update({ EventParticipations.id eq ep.id!! }) {
                    mapUpdate(it, ep)
                }
            }
        }
        EventParticipations.deleteWhere { EventParticipations.id inList result.second.map { it.id!! } }
        get(id)
    }

    fun togglePaid(memberId: ID, eventId: ID) = transaction {
        val participation = EventParticipations.map(
            EventParticipations
                .select { EventParticipations.member eq memberId and (EventParticipations.event eq eventId) }.single()
        )
        EventParticipations.update({ EventParticipations.id eq participation.id!! }) {
            it[paid] = !participation.paid
        }
        Unit
    }
}