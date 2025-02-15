package hr.askzg.service

import hr.askzg.db.*
import hr.askzg.util.refractionReportComparator
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.time.Year

object RefractionService : BasicService<Refraction, Refractions>(Refractions) {

    fun getInYear(year: Int): Map<Int, List<Refraction>> = transaction {
        val from = DateTime(year, 1, 1, 0, 0)
        val to = from.plusYears(1).minusSeconds(1)
        Refractions.select { Refractions.createdAt.between(from, to) }.map(Refractions).groupBy { it.memberId }
    }

    fun getReport(onlyActive: Boolean, year: Int): List<RefractionReport> = transaction {
        val refractions = getInYear(year)
        val asYear = Year.of(year)
        MemberService.getMembersTemporalData()
            .filter { if (!onlyActive) true else (it.member.status != Status.INACTIVE ) }
            .map { member ->
                val memberRefractions = (refractions[member.member.id] ?: emptyList())
                    .sortedByDescending { it.createdAt }
                RefractionReport(member.member, memberRefractions)
            }
            .sortedWith(refractionReportComparator)
    }

    fun getMemberRefractions(id: Int) = transaction {
        Refractions
            .select { Refractions.member.eq(id) }
            .orderBy(Refractions.createdAt, SortOrder.DESC)
            .map(Refractions)
    }

    fun pay(memberId: Int) = transaction {
        val member = MemberService.get(memberId)
        val refractions = Refractions
            .select { Refractions.member.eq(memberId) }
            .orderBy(Refractions.createdAt, SortOrder.ASC)
            .map(Refractions)

        refractions.forEachIndexed { index, refraction ->
            if (index % 2 == 1 && !refraction.paid) {
                refraction.paid = true
                save(refraction)
                val payment = Payment().apply {
                    date = DateTime.now()
                    amount = "4".toBigDecimal().setScale(2)
                    comment = "Kazna -  ${member.name}"
                }
                PaymentService.save(payment)
                return@transaction
            }
        }
    }

}

data class RefractionReport(val member: Member, val refractions: List<Refraction>)
