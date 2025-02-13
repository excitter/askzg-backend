package hr.askzg.db

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.joda.time.DateTime
import java.math.BigDecimal
import org.jetbrains.exposed.sql.jodatime.datetime

typealias ID = Int

interface IsMappable<E : Entity> {
    fun map(row: ResultRow): E
    fun mapInsert(stmt: UpdateBuilder<*>, entity: E)
    fun mapUpdate(stmt: UpdateBuilder<*>, entity: E) = mapInsert(stmt, entity)
}

abstract class AppTable<T : Entity>(name: String) : IntIdTable(name), IsMappable<T>

enum class UserDefaultPage {
    MEMBERS, PAYMENTS, EVENTS, REPORT, STATISTICS
}

enum class Status(val paysMembership: Boolean = true) {
    RECRUIT, MEMBER, INACTIVE(false)
}

enum class EventType {
    TRAINING, EVENT, OTHER
}

enum class ParticipationType {
    ATTENDED, UNABLE_TO_ATTEND, NOT_ATTENDED
}

enum class Role {
    USER, ADMIN;
}

open class Entity {
    var id: ID? = null
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Entity
        return (id == other.id)
    }

    override fun hashCode(): Int = id ?: 0
}

fun <E : Entity> SizedIterable<ResultRow>.map(table: IsMappable<E>) = map { table.map(it) }
fun <E : Entity> ResultRow.map(table: IsMappable<E>) = table.map(this)

fun Iterable<Entity>.ids() = mapNotNull { it.id }.toSet()

object Users : AppTable<User>("users") {
    val username = varchar("username", 50)
    val password = varchar("password", 200)
    val page = enumerationByName("page", 30, UserDefaultPage::class)
    val role = enumerationByName("role", 20, Role::class)
    val lastActivity = datetime("last_activity").nullable()

    override fun map(row: ResultRow) = User().apply {
        this.id = row[Users.id].value
        this.username = row[Users.username]
        this.password = row[Users.password]
        this.page = row[Users.page]
        this.role = row[Users.role]
        this.lastActivity = row[Users.lastActivity]
    }

    override fun mapInsert(stmt: UpdateBuilder<*>, entity: User) {
        stmt[username] = entity.username
        stmt[password] = entity.password
        stmt[page] = entity.page
        stmt[role] = entity.role
        stmt[lastActivity] = entity.lastActivity
    }
}

class User : Entity() {
    lateinit var username: String
    lateinit var password: String
    lateinit var page: UserDefaultPage
    lateinit var role: Role
    var lastActivity: DateTime? = null
}

object Members : AppTable<Member>("members") {

    val name = varchar("name", 50)
    val status = enumerationByName("status", 200, Status::class)
    val membership = integer("membership")
    val oib = varchar("oib", 30).nullable()
    val idCardNumber = varchar("id_card_number", 30).nullable()
    val firstName = varchar("first_name", 30).nullable()
    val lastName = varchar("last_name", 30).nullable()
    val dateOfBirth = datetime("date_of_birth").nullable()
    val phone = varchar("phone", 40).nullable()
    val address = varchar("address", 100).nullable()

    override fun map(row: ResultRow) = Member().apply {
        id = row[Members.id].value
        name = row[Members.name]
        status = row[Members.status]
        membership = row[Members.membership]
        oib = row[Members.oib]
        idCardNumber = row[Members.idCardNumber]
        firstName = row[Members.firstName]
        lastName = row[Members.lastName]
        dateOfBirth = row[Members.dateOfBirth]
        phone = row[Members.phone]
        address = row[Members.address]
    }

    override fun mapInsert(stmt: UpdateBuilder<*>, entity: Member) {
        stmt[name] = entity.name
        stmt[status] = entity.status
        stmt[membership] = entity.membership
        stmt[oib] = entity.oib
        stmt[idCardNumber] = entity.idCardNumber
        stmt[firstName] = entity.firstName
        stmt[lastName] = entity.lastName
        stmt[dateOfBirth] = entity.dateOfBirth
        stmt[phone] = entity.phone
        stmt[address] = entity.address
    }
}

class Member : Entity() {
    lateinit var name: String
    lateinit var status: Status
    var membership: Int = 100
    var oib: String? = null
    var idCardNumber: String? = null
    var firstName: String? = null
    var lastName: String? = null
    var dateOfBirth: DateTime? = null
    var phone: String? = null
    var address: String? = null
}

object MemberStatusPeriods : AppTable<MemberStatusPeriod>("member_status_periods") {
    val member = reference("member_id", Members, ReferenceOption.CASCADE, ReferenceOption.CASCADE)
    val status = enumerationByName("status", 50, Status::class)
    val start = datetime("start")

    override fun map(row: ResultRow) = MemberStatusPeriod().apply {
        id = row[MemberStatusPeriods.id].value
        memberId = row[member].value
        status = row[MemberStatusPeriods.status]
        start = row[MemberStatusPeriods.start]
    }

    override fun mapInsert(stmt: UpdateBuilder<*>, entity: MemberStatusPeriod) {
        stmt[member] = EntityID(entity.memberId, Members)
        stmt[status] = entity.status
        stmt[start] = entity.start
    }
}

class MemberStatusPeriod : Entity() {
    var memberId: Int = -1
    lateinit var status: Status
    lateinit var start: DateTime
}

object Memberships : AppTable<Membership>("memberships") {
    val member = reference("member_id", Members, ReferenceOption.CASCADE, ReferenceOption.CASCADE)
    val amount = integer("amount")
    val month = integer("month")
    val year = integer("year")


    override fun map(row: ResultRow) = Membership().apply {
        id = row[Memberships.id].value
        memberId = row[member].value
        amount = row[Memberships.amount]
        month = row[Memberships.month]
        year = row[Memberships.year]
    }

    override fun mapInsert(stmt: UpdateBuilder<*>, entity: Membership) {
        stmt[member] = EntityID(entity.memberId, Members)
        stmt[amount] = entity.amount
        stmt[month] = entity.month
        stmt[year] = entity.year
    }
}

class Membership : Entity() {
    var memberId: Int = -1
    var amount: Int = 0
    var month: Int = 1
    var year: Int = 2015
}

fun getGreatherOrEqualPriority(et: EventType): Set<EventType> {
    return when(et) {
        EventType.EVENT -> setOf(EventType.EVENT)
        EventType.TRAINING -> setOf(EventType.EVENT, EventType.TRAINING)
        EventType.OTHER -> setOf(EventType.EVENT, EventType.TRAINING, EventType.OTHER)
    }
}

fun getLessOrEqualPriority(et: EventType): Set<EventType> {
    return when(et) {
        EventType.EVENT -> setOf(EventType.EVENT, EventType.TRAINING, EventType.OTHER)
        EventType.TRAINING -> setOf(EventType.TRAINING, EventType.OTHER)
        EventType.OTHER -> setOf(EventType.OTHER)
    }
}

object Events : AppTable<Event>("events") {

    val name = varchar("name", 50)
    val type = enumerationByName("type", 20, EventType::class)
    val date = datetime("date")
    val endDate = datetime("end_date")
    val price = integer("price").nullable()
    val includeInStatistics = bool("include_in_statistics")

    override fun map(row: ResultRow) = Event().apply {
        id = row[Events.id].value
        name = row[Events.name]
        type = row[Events.type]
        date = row[Events.date]
        endDate = row[Events.endDate]
        price = row[Events.price]
        includeInStatistics = row[Events.includeInStatistics]
    }

    override fun mapInsert(stmt: UpdateBuilder<*>, entity: Event) {
        stmt[name] = entity.name
        stmt[type] = entity.type
        stmt[date] = entity.date
        stmt[endDate] = entity.endDate
        stmt[price] = entity.price
        stmt[includeInStatistics] = entity.includeInStatistics
    }
}

class Event : Entity() {
    lateinit var name: String
    lateinit var type: EventType
    lateinit var date: DateTime
    lateinit var endDate: DateTime
    var price: Int? = null
    var participation: List<EventParticipation> = listOf()
    var includeInStatistics: Boolean = true

    fun validate() {
        require(endDate >= date) { "End date must be greater than or equal to start date" }
    }
}


class EventExpense : Entity() {
    var eventId: Int = -1
    var paymentId: Int = -1
    var autoCalculated: Boolean = true
}

object EventExpenses: AppTable<EventExpense>("event_expenses") {
    val event = reference("event_id", Events, ReferenceOption.CASCADE, ReferenceOption.CASCADE)
    val payment = reference("payment_id", Payments, ReferenceOption.CASCADE, ReferenceOption.CASCADE)
    val autoCalculated = bool("auto_calculated")

    override fun map(row: ResultRow) = EventExpense().apply {
        id = row[EventExpenses.id].value
        eventId = row[event].value
        paymentId = row[payment].value
        autoCalculated = row[EventExpenses.autoCalculated]
    }

    override fun mapInsert(stmt: UpdateBuilder<*>, entity: EventExpense) {
        stmt[event] = EntityID(entity.eventId, Events)
        stmt[payment] = EntityID(entity.paymentId, Payments)
        stmt[autoCalculated] = entity.autoCalculated
    }
}

object EventParticipations : AppTable<EventParticipation>("event_participation") {

    val member = reference("member_id", Members, ReferenceOption.CASCADE, ReferenceOption.CASCADE)
    val event = reference("event_id", Events, ReferenceOption.CASCADE, ReferenceOption.CASCADE)
    val type = enumerationByName("type", 50, ParticipationType::class)
    val paid = bool("paid")


    override fun map(row: ResultRow) = EventParticipation().apply {
        id = row[EventParticipations.id].value
        memberId = row[member].value
        eventId = row[event].value
        type = row[EventParticipations.type]
        paid = row[EventParticipations.paid]
    }

    override fun mapInsert(stmt: UpdateBuilder<*>, entity: EventParticipation) {
        stmt[member] = EntityID(entity.memberId, Members)
        stmt[event] = EntityID(entity.eventId, Events)
        stmt[type] = entity.type
        stmt[paid] = entity.paid
    }
}

class EventParticipation : Entity() {
    var memberId: Int = -1
    var eventId: Int = -1
    lateinit var type: ParticipationType
    var paid: Boolean = false
}

object Products : AppTable<Product>("products") {
    val name = varchar("name", 200)
    val price = integer("price")

    override fun map(row: ResultRow) = Product().apply {
        id = row[Products.id].value
        name = row[Products.name]
        price = row[Products.price]
    }

    override fun mapInsert(stmt: UpdateBuilder<*>, entity: Product) {
        stmt[name] = entity.name
        stmt[price] = entity.price
    }

    override fun mapUpdate(stmt: UpdateBuilder<*>, entity: Product) {
        stmt[name] = entity.name
    }
}

class Product : Entity() {
    lateinit var name: String
    var price: Int = 0
    var participations: List<ProductParticipation> = listOf()
}

object ProductParticipations : AppTable<ProductParticipation>("product_participations") {
    val product = reference("product_id", Products, ReferenceOption.CASCADE, ReferenceOption.CASCADE)
    val member = reference("member_id", Members, ReferenceOption.CASCADE, ReferenceOption.CASCADE)
    val paid = bool("paid")

    override fun map(row: ResultRow): ProductParticipation = ProductParticipation().apply {
        id = row[ProductParticipations.id].value
        productId = row[product].value
        memberId = row[member].value
        paid = row[ProductParticipations.paid]
    }

    override fun mapInsert(stmt: UpdateBuilder<*>, entity: ProductParticipation) {
        stmt[product] = EntityID(entity.productId, Products)
        stmt[member] = EntityID(entity.memberId, Members)
        stmt[paid] = entity.paid
    }

    override fun mapUpdate(stmt: UpdateBuilder<*>, entity: ProductParticipation) {
        stmt[paid] = entity.paid
    }
}

class ProductParticipation : Entity() {
    var productId: Int = -1
    var memberId: Int = -1
    var paid: Boolean = false
}


object Payments : AppTable<Payment>("payments") {
    val amount = decimal("amount", 19, 2)
    val date = datetime("date")
    val comment = varchar("comment", 200)
    val membership = reference("membership_id", Memberships, ReferenceOption.SET_NULL, ReferenceOption.SET_NULL).nullable()
    val productParticipation = reference(
        "product_participation_id",
        ProductParticipations,
        ReferenceOption.SET_NULL,
        ReferenceOption.SET_NULL
    ).nullable()
    val eventParticipation = reference("event_participation_id", EventParticipations, ReferenceOption.SET_NULL, ReferenceOption.SET_NULL).nullable()
    val transientExpense = bool("transient_expense")

    override fun map(row: ResultRow) = Payment().apply {
        id = row[Payments.id].value
        amount = row[Payments.amount]
        date = row[Payments.date]
        comment = row[Payments.comment]
        membershipId = row[membership]?.value
        productParticipationId = row[productParticipation]?.value
        eventParticipationId = row[eventParticipation]?.value
        timestamp = row[Payments.date].millis
        transientExpense = row[Payments.transientExpense]
        canEdit = membershipId == null && productParticipationId == null && eventParticipationId == null
    }

    override fun mapInsert(stmt: UpdateBuilder<*>, entity: Payment) {
        stmt[amount] = entity.amount
        stmt[date] = entity.date
        stmt[comment] = entity.comment
        stmt[transientExpense] = entity.transientExpense
        entity.membershipId?.let {
            stmt[membership] = EntityID(it, Memberships)
        }
        entity.productParticipationId?.let {
            stmt[productParticipation] = EntityID(it, ProductParticipations)
        }
        entity.eventParticipationId?.let {
            stmt[eventParticipation] = EntityID(it, EventParticipations)
        }
    }
}

class Payment : Entity() {
    var amount: BigDecimal = BigDecimal.ZERO
    lateinit var date: DateTime
    var comment = ""
    var membershipId: Int? = null
    var productParticipationId: Int? = null
    var eventParticipationId: Int? = null
    var timestamp: Long? = null
    var transientExpense: Boolean = false
    var canEdit: Boolean = false
}

object Refractions : AppTable<Refraction>("refractions") {
    val member = reference("member_id", Members, ReferenceOption.CASCADE, ReferenceOption.CASCADE)
    val createdAt = datetime("created_at")
    val comment = varchar("comment", 255)
    val paid = bool("paid")

    override fun map(row: ResultRow) = Refraction().apply {
        id = row[Refractions.id].value
        memberId = row[Refractions.member].value
        createdAt = row[Refractions.createdAt]
        comment = row[Refractions.comment]
        paid = row[Refractions.paid]
    }

    override fun mapInsert(stmt: UpdateBuilder<*>, entity: Refraction) {
        stmt[member] = EntityID(entity.memberId, Members)
        stmt[createdAt] = entity.createdAt
        stmt[comment] = entity.comment
        stmt[paid] = entity.paid
    }

}

class Refraction : Entity() {
    var memberId: Int = -1
    lateinit var createdAt: DateTime
    var comment: String = ""
    var paid: Boolean = false
}
