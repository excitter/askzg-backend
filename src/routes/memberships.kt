package hr.askzg.routes

import hr.askzg.db.Member
import hr.askzg.db.Membership
import hr.askzg.db.Role
import hr.askzg.service.MembershipService
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import java.time.Month
import java.time.YearMonth

fun Route.memberships() {

    route("membership") {

        route("member") {

            get {
                call.respond(MembershipService.getMembershipsByMemberAndYear(call.queryId(), call.year()))
            }
            get("all") {
                val memberships = MembershipService.getMembershipsByYear(call.year()).map {
                    MembershipExtended().apply {
                        member = it.key
                        this.memberships = it.value
                    }
                }
                call.respond(memberships)
            }
            post {
                val request = call.receive<PayMembership>()
                call.respond(MembershipService.payMembership(request.memberId!!, request.month!!, request.year!!))
            }
            post("forgive") {
                val request = call.receive<PayMembership>()
                call.respond(MembershipService.forgiveMembership(request.memberId!!, request.month!!, request.year!!))
            }
            delete("{id}") {
                MembershipService.delete(call.pathId())
                call.respond(HttpStatusCode.OK)
            }
        }

        route("forgive") {
            post("last-unpaid") {
                call.userData(Role.ADMIN)
                val memberId = call.receive<PayMembership>().memberId!!
                val memberships = MembershipService.getAllMemberships(memberId)
                    .groupBy { (it.month to it.year) }

                var temp = YearMonth.of(2015, Month.JANUARY)

                while (true) {
                    val key = temp.monthValue to temp.year
                    if (!memberships.containsKey(key)) {
                        MembershipService.forgiveMembership(memberId, key.first, key.second)
                        break
                    }
                    temp = temp.plusMonths(1)
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

class MembershipExtended {
    lateinit var member: Member
    lateinit var memberships: List<Membership>
}

class PayMembership {
    var memberId: Int? = null
    var month: Int? = null
    var year: Int? = null
}
