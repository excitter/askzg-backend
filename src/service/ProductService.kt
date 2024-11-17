package hr.askzg.service

import hr.askzg.db.*
import hr.askzg.util.asMap
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import util.RecordUtil

object ProductService : BasicService<Product, Products>(Products) {

    override fun get(id: Int) = transaction {
        val product = super.get(id)
        product.participations = ProductParticipations.select { ProductParticipations.product eq id }.map(ProductParticipations)
        product
    }

    override fun save(entity: Product) = if (entity.id == null) add(entity) else update(entity)

    private fun add(product: Product) = transaction {
        val id = Products.insertAndGetId {
            mapInsert(it, product)
        }.value
        product.participations.forEach { pp ->
            pp.productId = id
            ProductParticipations.insert {
                mapInsert(it, pp)
            }
        }
        get(id)
    }

    private fun update(product: Product) = transaction {
        val id = product.id!!
        Products.update({ Products.id eq id }) {
            mapUpdate(it, product)
        }
        val existing = ProductParticipations.select { ProductParticipations.product eq id }.map(ProductParticipations)
            .asMap { it.id!! to it }
        val result = RecordUtil.analyzeEntities(existing.values, product.participations)
        result.first.forEach {
            it.productId = id
            it.paid = existing[it.id]?.paid ?: false
        }
        result.first.filter { it.id == null }.forEach { pp ->
            ProductParticipations.insert { mapInsert(it, pp) }
        }
        val toDeleteIds = result.second.map { it.id!! }
        toDeleteIds.map { EntityID(it, ProductParticipations) }.forEach { pp ->
            Payments.deleteWhere { Payments.productParticipation eq pp }
        }
        ProductParticipations.deleteWhere { ProductParticipations.id inList toDeleteIds }
        get(id)
    }

    fun pay(productId: ID, memberId: Int) = transaction {
        val participation =
            ProductParticipations.select { ProductParticipations.product eq productId and (ProductParticipations.member eq memberId) }
                .single().map(ProductParticipations)
        val product = get(productId)
        val member = MemberService.get(participation.memberId)

        if (participation.paid) {
            Payments.deleteWhere { Payments.productParticipation eq EntityID(participation.id, ProductParticipations) }
        } else {
            PaymentService.save(Payment().apply {
                amount = product.price.toBigDecimal().setScale(2)
                date = DateTime.now()
                comment = "${product.name} - ${member.name}"
                productParticipationId = participation.id
            })
        }
        ProductParticipations.update({ ProductParticipations.id eq participation.id }) {
            it[paid] = !participation.paid
        }
        Unit
    }

}
