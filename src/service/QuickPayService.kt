package hr.askzg.service

import hr.askzg.db.ID
import org.jetbrains.exposed.sql.transactions.transaction

object QuickPayService {

    fun pay(memberId: ID, year: Int, memberships: List<ID>, refractions: List<ID>, events: List<ID> ) = transaction {
        memberships.forEach {
            MembershipService.payMembership(memberId, it, year)
        }
        events.forEach {
            EventService.togglePaid(memberId, it)
        }
        refractions.forEach {
            RefractionService.pay(memberId)
        }
    }

}