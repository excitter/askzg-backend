package hr.askzg.pdf

import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import hr.askzg.db.Payment
import hr.askzg.util.sumByBigDecimal
import org.joda.time.format.DateTimeFormat

class PaymentPdfCreator(private val pdfData: PaymentPdfData) :
    PdfCreator("Plaćanja-${pdfData.interval.first.year}") {
    companion object {
        private val HEADERS = listOf("Datum", "Iznos", "Komentar")
        private val commentFont = font(9)
        private val formatter = DateTimeFormat.forPattern("d.M")
        private val MAPPER = { p: Payment ->
            listOf(
                DataCell(formatter.print(p.date)), DataCell("${p.amount} kn"), DataCell(p.comment, commentFont)
            )
        }
    }

    override fun populate(document: Document) {
        val df = DateTimeFormat.forPattern("dd.MM.yyyy")
        document.add(status("Stanje ${df.print(pdfData.interval.first.minusDays(1))}: ${pdfData.balance} kn"))
        val sumAll = pdfData.payments.sumByBigDecimal { it.amount }
        val sumFiltered = pdfData.payments.filter(pdfData.filter).sumByBigDecimal { it.amount }
        document.add(status("Stanje ${df.print(pdfData.interval.second)}: ${pdfData.balance + sumAll} kn ($sumAll kn)", 20))
        val data: MutableList<List<DataCell>> = pdfData.payments.filter(pdfData.filter).map(MAPPER).toMutableList()
        if (data.isNotEmpty())
            data.add(
                mutableListOf(DataCell("Sum"), DataCell("$sumFiltered kn"), DataCell(""))
            )
        document.add(table(HEADERS, intArrayOf(1, 1, 2), data))
    }

    private fun status(text: String, spacing: Int = 10) = Paragraph(text, contentFont).apply {
        spacingAfter = spacing.toFloat()
    }
}
