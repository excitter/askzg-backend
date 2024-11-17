package hr.askzg.util

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import hr.askzg.db.User
import hr.askzg.dft

class UserAdapter : TypeAdapter<User>() {
    override fun write(writer: JsonWriter, value: User?) {
        if (value == null) return
        writer.beginObject()
            .name("id").value(value.id)
            .name("username").value(value.username)
            .name("page").value(value.page.name)
            .name("role").value(value.role.name)
            .name("lastActivity").value(if (value.lastActivity == null) null else dft.print(value.lastActivity))
            .endObject()
    }

    override fun read(reader: JsonReader): User? = null
}