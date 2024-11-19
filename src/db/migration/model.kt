package hr.askzg.db.migration

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.jodatime.datetime

object Migrations : IntIdTable("migrations") {
    val version = integer("version").uniqueIndex("migrations_version_unique_idx")
    val description = varchar("description", 100)
    val executed = datetime("executed")
    val duration = integer("duration")
}

class Migration(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Migration>(Migrations)

    var version by Migrations.version
    var description by Migrations.description
    var executed by Migrations.executed
    var duration by Migrations.duration
}

data class MigrationInfo(val fileName: String, val version: Int, val description: String, val content: String)
