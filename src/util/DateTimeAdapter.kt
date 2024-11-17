package hr.askzg.util

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import hr.askzg.df
import org.joda.time.DateTime

class DateTimeAdapter : TypeAdapter<DateTime>() {
    override fun write(writer: JsonWriter, value: DateTime?) {
        writer.value(if (value == null) null else df.print(value))
    }

    override fun read(reader: JsonReader): DateTime? =
        if (!reader.hasNext()) null else df.parseDateTime(reader.nextString())
}