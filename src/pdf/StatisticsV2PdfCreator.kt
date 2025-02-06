package hr.askzg.pdf

import com.itextpdf.text.Document
import hr.askzg.db.EventType
import hr.askzg.routes.EventCountsV2
import hr.askzg.routes.StatisticsV2

class StatisticsV2PdfCreator(private val stats: StatisticsV2, year: Int): PdfCreator("Statistika-$year") {

    companion object {
        private val MEMBER_HEADERS = listOf("Član", "Prisutan", "Odsutan", "Spriječen", "%", "PP%")
        private val MEMBER_HEADERS_WIDTH = intArrayOf(2, 1, 1, 1, 1, 1)
        private val EVENT_HEADERS = listOf("Datum", "Događaj", "Prisutni", "%", "PP%")
        private val EVENT_HEADERS_WIDTH = intArrayOf(1, 2, 3, 1, 1)
        private val MEMBER_COMPARATOR = Comparator<Pair<Int, EventCountsV2>> { oo1, oo2 ->
            val o1 = oo1.second
            val o2 = oo2.second
            val v = o2.attended - o1.attended
            if (v == 0) {
                if(o2.possiblePct != null && o1.possiblePct != null) {
                    val v2 = o2.possiblePct!! == o1.possiblePct!!
                    if (v2) {
                        o1.didntAttend- o2.didntAttend
                    } else (o2.possiblePct!! - o1.possiblePct!!).toInt()
                } else if (o2.possiblePct != null) {
                    1
                } else {
                    o1.didntAttend- o2.didntAttend
                }
            } else v
        }
    }

    private fun memberStats(s: StatisticsV2, et: EventType): List<List<DataCell>> {
        return s.memberEventStatistics.map { it.memberId to (it.attendance[et] ?: EventCountsV2()) }.sortedWith(
            MEMBER_COMPARATOR
        ).map { mes ->
            val attendance = mes.second
            listOf(
                s.members[mes.first]!!.name,
                attendance.attended,
                attendance.didntAttend,
                attendance.couldntAttend,
                attendance.totalPct,
                attendance.possiblePct
            ).map { it.dataCell() }
        }
    }

    private fun eventStats(s: StatisticsV2, et: EventType): List<List<DataCell>> {
        return (s.eventBreakdowns[et] ?: listOf()).map{breakdown ->
            val event = s.events[breakdown.eventId]!!
            listOf(
                "${event.date.dayOfMonth().get()}.${event.date.monthOfYear().get()}.${event.date.year().get()}.",
                event.name,
                breakdown.attendedMemberIds.map { s.members[it]!!.name }.filter { it.isNotEmpty() }.joinToString(" "),
                // breakdown.missedMemberIds.map { s.members[it]!!.name }.filter { it.isNotEmpty() }.joinToString(" "),
                // breakdown.unableToAttendMemberIds.map { s.members[it]!!.name }.filter { it.isNotEmpty() }.joinToString(" "),
                breakdown.attendedPct,
                breakdown.adjustedPct
            ).map { it.dataCell() }
        }
    }

    override fun populate(document: Document) {
        document.add(paragraph("Članovi na treninzima", 10))
        document.add(table(MEMBER_HEADERS, MEMBER_HEADERS_WIDTH, memberStats(stats, EventType.TRAINING)))
        document.newPage()

        document.add(paragraph("Članovi na susretima", 10))
        document.add(table(MEMBER_HEADERS, MEMBER_HEADERS_WIDTH, memberStats(stats, EventType.EVENT)))
        document.newPage()

        document.add(paragraph("Članovi na ostalim aktivnostima", 10))
        document.add(table(MEMBER_HEADERS, MEMBER_HEADERS_WIDTH, memberStats(stats, EventType.OTHER)))
        document.newPage()

        document.add(paragraph("Prisutnost na treninzima", 10))
        document.add(table(EVENT_HEADERS, EVENT_HEADERS_WIDTH, eventStats(stats, EventType.TRAINING)))
        document.newPage()

        document.add(paragraph("Prisutnost na susretima", 10))
        document.add(table(EVENT_HEADERS, EVENT_HEADERS_WIDTH, eventStats(stats, EventType.EVENT)))
        document.newPage()

        document.add(paragraph("Prisutnost na ostalim aktivnostima", 10))
        document.add(table(EVENT_HEADERS, EVENT_HEADERS_WIDTH, eventStats(stats, EventType.OTHER)))
    }

}