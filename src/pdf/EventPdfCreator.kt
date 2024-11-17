package hr.askzg.pdf

import com.itextpdf.text.Document
import hr.askzg.db.Event
import hr.askzg.db.EventType
import hr.askzg.df

class EventPdfCreator(private val events: List<Event>, year: Int) : PdfCreator("Događaji-$year") {

    companion object {
        private val HEADERS = listOf("Ime", "Datum", "Tip", "Cijena")
        private val TYPE_MAP = mapOf(
            EventType.TRAINING to "Trening", EventType.EVENT to "Susret", EventType.OTHER to "Ostalo"
        )
        private val MAPPER = { e: Event ->
            listOf(e.name, df.print(e.date), TYPE_MAP[e.type], e.price)
        }
    }

    override fun populate(document: Document) {
        document.add(table(HEADERS, data = events.rows(MAPPER)))
    }
}