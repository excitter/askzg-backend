package hr.askzg.service

import hr.askzg.db.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.math.BigDecimal

object PaymentService : BasicService<Payment, Payments>(Payments) {

    fun getBalance(date: DateTime) = transaction {
        val sum = Payments.amount.sum()
        val amount = Payments.slice(sum).select {
            Payments.date less date
        }.map { it[sum] }.firstOrNull()
        amount ?: BigDecimal("0.00")
    }

    fun getFiltered(from: DateTime, to: DateTime) = transaction {
        Payments.select { Payments.date.between(from, to) }.orderBy(Payments.date, SortOrder.DESC).map(Payments)
    }

    override fun save(entity: Payment): Payment {
        if (entity.id == null) return super.save(entity)
        return transaction {
            val existing = get(entity.id!!)
            existing.amount = entity.amount
            existing.comment = entity.comment
            if (existing.date.toLocalDate() != entity.date.toLocalDate())
                existing.date = entity.date
            Payments.update({ Payments.id eq entity.id!! }) {
                it[amount] = existing.amount
                it[comment] = existing.comment
                it[date] = existing.date
            }
            existing
        }
    }

    override fun delete(id: Int) = transaction {
        val existing = get(id)
        existing.membershipId?.let { m -> Memberships.deleteWhere { Memberships.id eq m } }
        existing.productParticipationId?.let { m ->
            ProductParticipations.update({ ProductParticipations.id eq m }) {
                it[paid] = false
            }
        }
        Payments.deleteWhere { Payments.id eq id }
    }
}
