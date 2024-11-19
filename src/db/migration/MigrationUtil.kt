package hr.askzg.db.migration

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.exposedLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.io.File

object MigrationUtil {


    fun migrate() {
        checkConnection()
        checkMigrationTable()
        val migrations = transaction { Migration.all().toList().sortedBy { it.version } }
        val lastVersion = migrations.lastOrNull()?.version ?: 0
        val versions = migrations.map { it.version }.toSet()
        val dir = File("resources/migrations")
        if (!dir.exists()) throw Error("Migrations folder does not exist!")
        val listFiles = dir.listFiles() ?: return
        val infos = listFiles.filter { it.extension == "sql" }.map {
            val split = it.nameWithoutExtension.split("__")
            if (split.size != 2 || split[0].toIntOrNull() == null) throw Error("SQL file must be in format VERSION__DESCRIPTION_WITH_WORDS.sql")
            MigrationInfo(it.name, split[0].toInt(), split[1], it.readText())
        }.filter { it.version !in versions && it.version > lastVersion }.sortedBy { it.version }
        exposedLogger.info("Found ${infos.size} files to migrate...")
        infos.forEach(::migrate)
        exposedLogger.info("Migration done!")
    }

    private fun migrate(info: MigrationInfo) = transaction {
        exposedLogger.info("Migrating ${info.fileName}...")
        val start = System.currentTimeMillis()
        TransactionManager.current().exec(info.content)
        val duration = ((System.currentTimeMillis() - start) / 1000).toInt()
        Migration.new {
            version = info.version
            description = info.description
            executed = DateTime.now()
            this.duration = maxOf(duration, 1)
        }
        Unit
    }

    private fun checkConnection() = transaction {
        if (TransactionManager.current().connection.isClosed) throw Error("No connection for migration!")
    }

    private fun checkMigrationTable() = transaction {
        SchemaUtils.createMissingTablesAndColumns(Migrations)
    }
}