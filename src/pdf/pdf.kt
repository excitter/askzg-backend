package hr.askzg.pdf

import com.itextpdf.text.*
import com.itextpdf.text.pdf.*
import hr.askzg.db.Payment
import hr.askzg.df
import org.joda.time.DateTime
import java.io.OutputStream
import java.math.BigDecimal
import java.time.Month

val titleFont: Font = font(36)
val headerFont: Font = font(12, Font.BOLD)
val contentFont: Font = font(10)

abstract class PdfCreator(private val title: String) {

    protected abstract fun populate(document: Document)

    fun streamTo(os: OutputStream) = with(Document(PageSize.A4, 27.toFloat(), 27.toFloat(), 18.toFloat(), 18.toFloat())) {
        PdfWriter.getInstance(this, os).apply {
            pageEvent = MyFooter()
        }
        open()

        add(table(2, intArrayOf(1, 4)).apply {
            spacingAfter = 40.toFloat()
            addCell(PdfPCell(Image.getInstance(javaClass.getResourceAsStream("/images/logo.png").readBytes(), false)).apply {
                fixedHeight = 80.toFloat()
                border = Rectangle.BOTTOM
                borderColor = BaseColor.BLACK
            })
            addCell(PdfPCell().apply {
                addElement(Paragraph(title, titleFont).apply {
                    alignment = Element.ALIGN_LEFT
                })
                verticalAlignment = Element.ALIGN_CENTER
                border = Rectangle.BOTTOM
                borderColor = BaseColor.BLACK
                fixedHeight = 80.toFloat()
            })
        })
        populate(this)
        close()
    }
}

class MyFooter : PdfPageEventHelper() {

    private val time = df.print(DateTime.now())
    private val footerFont = font(10)

    override fun onEndPage(writer: PdfWriter, document: Document) {
        val cb = writer.directContent
        ColumnText.showTextAligned(
            cb, Element.ALIGN_CENTER,
            Phrase(time, footerFont),
            (document.right() - document.left()) / 2 + document.leftMargin(),
            document.bottom() - 10, 0f
        )
    }
}

fun List<DataCell>.addInTable(table: PdfPTable) = forEach { it.addInTable(table) }

fun DataCell.addInTable(table: PdfPTable): PdfPCell = with(PdfPCell(Phrase(text, font))) {
    fixedHeight = this@addInTable.height
    verticalAlignment = Element.ALIGN_CENTER
    horizontalAlignment = Element.ALIGN_CENTER
    padding?.let {
        paddingTop = it
    }
    table.addCell(this)
}

fun table(headers: List<String>, widths: IntArray = IntArray(headers.size) { 1 }, data: List<List<DataCell>> = listOf()) =
    PdfPTable(headers.size).apply {
        widthPercentage = 100.toFloat()
        setWidths(widths)
        headers.map { DataCell(it, headerFont) }.addInTable(this)
        data.forEach { it.addInTable(this) }
    }

data class DataCell(
    val text: String,
    val font: Font = contentFont,
    val height: Float = 25.toFloat(),
    val padding: Float? = null
) {
    constructor(
        value: Any?,
        font: Font = contentFont,
        height: Float = 25.toFloat(),
        padding: Float? = null
    ) : this(value?.toString() ?: "", font, height, padding)
}

fun Any?.dataCell() = DataCell(this)
fun List<Any?>.dataCell() = map { it.dataCell() }

fun table(columns: Int, widths: IntArray = IntArray(columns) { 1 }) = PdfPTable(columns).apply {
    widthPercentage = 100.toFloat()
    setWidths(widths)
}

fun paragraph(text: String, spacingAfter: Int? = null) = Paragraph(Chunk(text, contentFont)).apply {
    if (spacingAfter != null) this.spacingAfter = spacingAfter.toFloat()
}

fun font(size: Int, weight: Int = Font.NORMAL): Font =
    FontFactory.getFont(FontFactory.COURIER, "Cp1250", size.toFloat(), weight, BaseColor.BLACK)

fun <T> List<T>.rows(mapper: (T) -> List<Any?>) = this.map { mapper(it).dataCell() }

fun Set<Month>.join(prefix: String = "", postfix: String = "") =
    if (isEmpty()) "" else joinToString(separator = " ", prefix = prefix, postfix = postfix) { it.value.toString() }

data class PaymentPdfData(
    val payments: List<Payment>,
    val balance: BigDecimal,
    val interval: Pair<DateTime, DateTime>,
    val filter: (Payment) -> Boolean,
    val title: String
)
