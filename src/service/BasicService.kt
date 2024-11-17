package hr.askzg.service

import hr.askzg.db.AppTable
import hr.askzg.db.Entity
import hr.askzg.db.map
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

open class BasicService<E : Entity, T : AppTable<E>>(private val table: T) {

    open fun getAll() = transaction {
        table.selectAll().map(table)
    }

    open fun get(id: Int) = transaction { table.select { table.id eq id }.single().map(table) }

    open fun save(entity: E): E = transaction {
        if (entity.id == null) {
            entity.id = table.insertAndGetId {
                table.mapInsert(it, entity)
            }.value
        } else {
            val count = table.update({ table.id eq entity.id!! }) {
                table.mapUpdate(it, entity)
            }
            if (count != 1) throw NoSuchElementException()
        }
        get(entity.id!!)
    }

    open fun delete(id: Int) = transaction {
        table.deleteWhere { table.id eq id }
    }
}