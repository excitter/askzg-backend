package hr.askzg.pdf

import com.itextpdf.text.Document
import hr.askzg.db.Event
import hr.askzg.db.EventType
import hr.askzg.db.ParticipationType
import hr.askzg.df
import hr.askzg.routes.MemberParticipationData
import hr.askzg.util.memberNickComparator
import java.math.BigDecimal
import java.math.RoundingMode

class StatisticsPdfCreator(private val memberStats: List<MemberParticipationData>, private val events: List<Event>, year: Int) :
    PdfCreator("Statistika-$year") {

    companion object {
        private val TRAINING_HEADERS = listOf("Član", "+", "-", "N/A", "%")
        private val EVENTS_HEADERS = listOf("Datum", "Ime", "Članovi")
        private val EVENTS_FONT = font(8)
        private const val EVENTS_HEIGHT = 35.toFloat()
        private val COMPARATOR = Comparator<MemberParticipationData> { o1, o2 ->
            val v = o2.attended.size - o1.attended.size
            if (v == 0) {
                val v2 = o2.percentage - o1.percentage
                if (v2 == 0) o1.member.member.name.compareTo(o2.member.member.name) else v2
            } else v
        }
    }

    override fun populate(document: Document) {
        document.add(paragraph("Članovi", 10))
        document.add(table(TRAINING_HEADERS, intArrayOf(2, 1, 1, 1, 1), memberStats.sortedWith(COMPARATOR).map { it.row() }))

        document.newPage()

        val events = events.filter { it.type == EventType.TRAINING }.sortedBy { it.date }
        val sum = events.map { e -> e.participation.count { it.type == ParticipationType.ATTENDED } }.sum()
        val average = if (events.isEmpty())
            BigDecimal.ZERO
        else
            sum.toBigDecimal().setScale(1, RoundingMode.HALF_UP)
                .divide(events.size.toBigDecimal(), 1, RoundingMode.HALF_UP)

        val data = events.map { event ->
            val row = ArrayList<DataCell>()
            row.add(DataCell(df.print(event.date), height = EVENTS_HEIGHT, padding = 10.toFloat()))
            row.add(DataCell(event.name, height = EVENTS_HEIGHT, padding = 10.toFloat()))
            val members = memberStats.filter { it.hasAttended(event) }.map { it.member.member }.sortedWith(memberNickComparator)
                .joinToString { it.name }
            row.add(DataCell(members, EVENTS_FONT, EVENTS_HEIGHT, padding = 2.toFloat()))
            row
        }.toList()

        document.add(paragraph("Treninzi", 10))
        document.add(paragraph("Broj treninga: ${events.size}  Prosjek: $average", 10))
        document.add(table(EVENTS_HEADERS, intArrayOf(1, 1, 3), data))
    }

    private fun MemberParticipationData.row() = listOf(
        member.member.name, attended.size, missed.size, unableToAttend.size, percentage
    ).map { it.dataCell() }
}