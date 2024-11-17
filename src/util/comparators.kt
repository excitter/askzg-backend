package hr.askzg.util

import hr.askzg.db.Member
import hr.askzg.service.RefractionReport
import java.text.Collator
import java.util.*

private val collator = Collator.getInstance(Locale("hr", "HR"))

val memberNameComparator = Comparator<Member> { a, b ->
    var cmp = collator.compare(a.lastName ?: "", b.lastName ?: "")
    if (cmp == 0) cmp = collator.compare(a.firstName ?: "", b.firstName ?: "")
    cmp
}
val memberNickComparator = Comparator<Member> { o1, o2 -> collator.compare(o1.name, o2.name) }
val refractionReportComparator = Comparator<RefractionReport> { o1, o2 -> collator.compare(o1.member.name, o2.member.name) }
