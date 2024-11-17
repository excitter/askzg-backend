package hr.askzg.pdf

import com.itextpdf.text.Document
import hr.askzg.db.Event
import hr.askzg.db.EventParticipation
import hr.askzg.routes.EventReport
import hr.askzg.routes.MemberMembershipData

class ReportPdfCreator(
    private val eventReport: EventReport,
    private val membershipReport: List<MemberMembershipData>,
    year: Int
) : PdfCreator("Izvješće-$year") {

    companion object {
        private val MEMBERSHIP_HEADERS = listOf("Član", "Dug (mjeseci)", "Dug (kn)")
        private val MEMBERSHIP_MAPPER = { m: MemberMembershipData ->
            listOf(
                m.member.name,
                m.owedMonths.join() + m.unpaidMonths.filter { it !in m.owedMonths }.toSortedSet().join(" (", ")"),
                m.debt
            )
        }
        private val EVENT_HEADERS = listOf("Događaj", "Cijena", "Članovi")
        private val EVENT_FONT = font(8)
        private const val EVENT_HEIGHT = 40.toFloat()
    }

    private val memberMap = eventReport.members.map { it.id to it.name }.toMap()

    override fun populate(document: Document) {
        document.add(paragraph("Članarine"))
        if (membershipReport.isEmpty()) {
            document.add(paragraph("Nema dugova", 10))
        } else {
            document.add(paragraph("Ukupan dug: ${membershipReport.map { it.debt }.sum()} kn", 10))
            document.add(table(MEMBERSHIP_HEADERS, intArrayOf(1, 2, 1), membershipReport.rows(MEMBERSHIP_MAPPER)).apply {
                spacingAfter = 10.toFloat()
            })

            document.newPage()
        }

        document.add(paragraph("Događaji", 10))
        if (eventReport.eventParticipation.isEmpty()) {
            document.add(paragraph("Nema događaja"))
        } else {
            document.add(
                table(
                    EVENT_HEADERS, intArrayOf(2, 1, 3),
                    eventReport.eventParticipation.entries.sortedBy { it.key.date }.map { it.row() })
            )
        }
    }

    private fun Map.Entry<Event, List<EventParticipation>>.row() = listOf(
        key.name, key.price, value.joinToString(" ") { memberMap.getValue(it.memberId) }
    ).map { DataCell(it, EVENT_FONT, EVENT_HEIGHT) }


}