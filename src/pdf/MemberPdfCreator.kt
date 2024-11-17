package hr.askzg.pdf

import com.itextpdf.text.Document
import hr.askzg.db.Member
import hr.askzg.df

class MemberPdfCreator(private val members: List<Member>) : PdfCreator("Članovi") {

    companion object {
        private val HEADERS = listOf("Ime", "OIB", "DoB", "Adresa")
        private val FONT = font(8)
        private const val HEIGHT = 17.toFloat()
        private val MAPPER = { m: Member ->
            listOf(
                DataCell("${m.firstName} ${m.lastName}", FONT, HEIGHT),
                DataCell(m.oib, FONT, HEIGHT),
                DataCell(df.print(m.dateOfBirth), FONT, HEIGHT),
                DataCell(m.address, FONT, HEIGHT)
            )
        }
    }

    override fun populate(document: Document) {
        document.add(table(HEADERS, intArrayOf(2, 1, 1, 3), members.map(MAPPER)))
    }
}