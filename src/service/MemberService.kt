package hr.askzg.service

import hr.askzg.db.*
import hr.askzg.routes.MemberTemporalData
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object MemberService : BasicService<Member, Members>(Members) {

    fun save(entity: Member, memberStatusPeriods: List<MemberStatusPeriod>) = transaction {
        val periods = memberStatusPeriods.sortedBy { it.start }
        if (periods.isEmpty() || entity.id == null && periods.any { it.id != null })
            throw IllegalArgumentException("Wrong period definitions!")

        var lastStatus = periods[0].status
        for (period in periods.subList(1, periods.size)) {
            if (period.status == lastStatus) throw IllegalStateException("Wrong period definitions!")
            lastStatus = period.status
        }
        entity.status = lastStatus
        save(entity)
        periods.forEach { it.memberId = entity.id!! }
        periods.forEach {
            MemberStatusPeriodService.save(it)
        }
        MemberTemporalData(entity, periods)
    }

    fun getMemberTemporalData(id: ID) = transaction {
        MemberTemporalData(
            get(id),
            MemberStatusPeriods.select { MemberStatusPeriods.member eq id }.map(MemberStatusPeriods).sortedBy { it.start }
        )
    }

    fun getMembersTemporalData() = transaction {
        val members = getAll()
        val periods = MemberStatusPeriods.selectAll().map(MemberStatusPeriods).groupBy { it.memberId }
        members.map { MemberTemporalData(it, periods.getValue(it.id!!)) }
    }
}