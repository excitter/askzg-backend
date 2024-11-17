package hr.askzg.service

import hr.askzg.db.ID
import hr.askzg.db.Role
import hr.askzg.db.User
import hr.askzg.db.Users
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.joda.time.DateTime

object UserService : BasicService<User, Users>(Users) {

    fun getByUsername(username: String) = transaction {
        Users.map(Users.select { Users.username eq username }.single())
    }

    fun updateLastActivity(id: ID, time: DateTime) = transaction {
        Users.update({ Users.id eq id }) {
            it[lastActivity] = time
        }
    }

    override fun delete(id: Int): Int = transaction {
        Users.deleteWhere { Users.id eq id and (Users.role eq Role.USER) }
    }
}