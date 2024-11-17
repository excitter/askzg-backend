package hr.askzg.util

import org.joda.time.DateTime
import java.math.BigDecimal
import java.time.LocalDate

fun DateTime.asLocalDate(): LocalDate = LocalDate.of(year, monthOfYear, dayOfMonth)

inline fun <T, K, V> Iterable<T>.asMap(transform: (T) -> Pair<K, V>): Map<K, V> = map(transform).toMap()

fun yearRange(year: Int): Pair<DateTime, DateTime> = DateTime(year, 1, 1, 1, 0) to DateTime(year, 12, 31, 23, 59, 59)
fun yearRange(year: Int, now: DateTime): Pair<DateTime, DateTime> = with(yearRange(year)) {
    first to minOf(second, now)
}

public inline fun <T> Iterable<T>.sumByBigDecimal(selector: (T) -> BigDecimal): BigDecimal {
    var sum: BigDecimal = BigDecimal.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
