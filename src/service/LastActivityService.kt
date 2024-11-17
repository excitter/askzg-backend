package hr.askzg.service

import hr.askzg.db.ID
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import java.util.concurrent.ConcurrentHashMap

object LastActivityService {

    private val map = ConcurrentHashMap<ID, DateTime>()

    init {
        GlobalScope.launch {
            while (true) {
                delay(5000L)
                process()
            }
        }.start()
    }

    fun mark(id: ID) {
        map[id] = DateTime.now()
    }

    private fun process() {
        val temp = map.toMap()
        map.clear()
        temp.forEach { (id, time) ->
            UserService.updateLastActivity(id, time)
        }
    }
}