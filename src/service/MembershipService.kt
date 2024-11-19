package hr.askzg.service

import hr.askzg.db.*
import hr.askzg.util.asMap
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

object MembershipService {

    fun getMembershipsByMemberAndYear(id: ID, year: Int) = transaction {
        Memberships.select { Memberships.member eq id and (Memberships.year eq year) }
            .orderBy(Memberships.month).map(Memberships)
    }

    fun getMembershipsByYear(year: Int) = transaction {
        val members = MemberService.getAll()
        val membershipMap =
            Memberships.select { Memberships.member inList members.map { it.id!! } and (Memberships.year eq year) }
                .orderBy(Memberships.month).map(Memberships).groupBy { it.memberId }
        members.asMap { it to (membershipMap[it.id!!] ?: listOf()) }
    }

    fun getAllMemberships(id: ID) = transaction {
        Memberships.select { Memberships.member.eq(id) }
            .orderBy(Memberships.year to SortOrder.ASC, Memberships.month to SortOrder.ASC)
            .map(Memberships)
    }

    fun payMembership(memberId: ID, month: Int, year: Int) = createMembership(memberId, month, year) { it.membership }

    fun forgiveMembership(memberId: ID, month: Int, year: Int) = createMembership(memberId, month, year) { 0 }

    fun delete(id: ID) = transaction {
        Payments.deleteWhere { Payments.membership eq EntityID(id, Memberships) }
        Memberships.deleteWhere { Memberships.id eq id }
        Unit
    }

    private fun createMembership(memberId: ID, month: Int, year: Int, amountSupplier: (Member) -> Int) = transaction {
        if (Memberships.select {
                Memberships.member eq memberId and (Memberships.month eq month) and (Memberships.year eq year)
            }.any()) throw IllegalArgumentException()

        val member = MemberService.get(memberId)
        val membership = Membership().apply {
            this.memberId = memberId
            this.amount = amountSupplier(member)
            this.month = month
            this.year = year

        }
        val id = Memberships.insertAndGetId { mapInsert(it, membership) }.value
        membership.id = id
        if (membership.amount > 0) {
            val payment = Payment().apply {
                date = DateTime.now()
                amount = membership.amount.toBigDecimal().setScale(2)
                membershipId = id
                comment = "Članarina -  ${member.name} ($month.$year)"
            }
            PaymentService.save(payment)
        }
        membership
    }
}
