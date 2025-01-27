package util

import hr.askzg.db.Entity


object RecordUtil {

    fun <T : Entity> analyzeEntities(existing: Iterable<T>, toSave: Iterable<T>): Pair<List<T>, List<T>> {
        val newEntities = toSave.filter { it.id == null }
        val existingIDs = toSave.filter { it.id != null }.map { it.id!! }.toSet()
        val toDelete = existing.filter { !existingIDs.contains(it.id) }
        val toDeleteIds = toDelete.map { it.id!! }
        val remains = toSave.filter { it.id != null && !toDeleteIds.contains(it.id!!) }
        return (newEntities + remains) to toDelete
    }
}