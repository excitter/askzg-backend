package hr.askzg.pdf

import com.itextpdf.text.Document
import hr.askzg.db.Refraction
import hr.askzg.service.RefractionReport
import java.util.concurrent.TimeUnit

class RefractionsPdfCreator(private val stats: List<RefractionReport>, private val year: Int) : PdfCreator("Kazne - $year") {
    companion object {
        private val MEMBER_HEADERS = listOf("Član", "Broj kazni")
        private val MEMBER_HEADER_WIDTHS = intArrayOf(1, 1)
        private val MEMBER_COMPARATOR = Comparator<RefractionReport> { rr1, rr2 ->
            rr2.refractions.size - rr1.refractions.size
        }
        private val LIST_HEADERS = listOf("Datum", "Razlog", "Platio")
        private val LIST_HEADER_WIDTHS = intArrayOf(1, 1, 1)
        private val DATE_COMPARATOR = Comparator<Refraction> { r1, r2 ->
            val t2 = TimeUnit.MILLISECONDS.toSeconds(r2.createdAt.millis)
            val t1 = TimeUnit.MILLISECONDS.toSeconds(r1.createdAt.millis)
            (t2 - t1).toInt()
        }
    }

    fun memberRefractions(stats: List<RefractionReport>): List<List<DataCell>> {
        return stats.sortedWith(MEMBER_COMPARATOR).map {
            listOf(
                it.member.name,
                it.refractions.size
            )
        }.map { it.dataCell() }
    }

    fun memberRefractionList(r: RefractionReport): List<List<DataCell>> {
        return r.refractions.sortedWith(DATE_COMPARATOR).map {
            listOf(
                "${it.createdAt.dayOfMonth().get()}.${it.createdAt.monthOfYear().get()}.${it.createdAt.year().get()}.",
                it.comment,
                if (it.paid) "Da" else "Ne"
            )
        }.map { it.dataCell() }
    }

    override fun populate(document: Document) {
        document.add(titleParagraph("Kazne", 10))
        document.add(table(MEMBER_HEADERS, MEMBER_HEADER_WIDTHS, memberRefractions(stats)))
        document.newPage()

        stats.filter { it.refractions.isNotEmpty() }.forEach {
            document.add(titleParagraph(it.member.name, 10))
            document.add(table(LIST_HEADERS, LIST_HEADER_WIDTHS, memberRefractionList(it)))
            document.newPage()
        }
    }

}
