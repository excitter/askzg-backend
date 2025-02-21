package hr.askzg

import hr.askzg.db.User
import hr.askzg.db.migration.MigrationUtil
import hr.askzg.routes.*
import hr.askzg.security.SimpleJWT
import hr.askzg.security.UserData
import hr.askzg.security.setupSecurity
import hr.askzg.service.LastActivityService
import hr.askzg.util.DateTimeAdapter
import hr.askzg.util.UserAdapter
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.authenticate
import io.ktor.auth.principal
import io.ktor.features.*
import io.ktor.gson.gson
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import org.jetbrains.exposed.sql.Database
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.postgresql.ds.PGSimpleDataSource

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

val df: DateTimeFormatter = DateTimeFormat.forPattern("d.M.yyyy")
val ddf: DateTimeFormatter = DateTimeFormat.forPattern("d.M.")
val dft: DateTimeFormatter = DateTimeFormat.forPattern("d.M.yyyy HH:mm")

@KtorExperimentalAPI
@Suppress("unused")
@kotlin.jvm.JvmOverloads
fun Application.module(@Suppress("UNUSED_PARAMETER") testing: Boolean = false) {
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }

    install(AutoHeadResponse)

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        method(HttpMethod.Get)
        header(HttpHeaders.Authorization)
        header(HttpHeaders.Origin)
        header(HttpHeaders.Referrer)
        header(HttpHeaders.AccessControlAllowOrigin)
        allowCredentials = true
        allowNonSimpleContentTypes = true
        allowSameOrigin = true
        anyHost()
    }
    val simpleJWT = SimpleJWT(environment.config.property("ktor.auth.key").getString())

    setupSecurity(simpleJWT.verifier)

    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            registerTypeAdapter(DateTime::class.java, DateTimeAdapter())
            registerTypeAdapter(User::class.java, UserAdapter())
        }
    }

    Database.connect(PGSimpleDataSource().apply {
        setURL(environment.config.property("ktor.db.url").getString())
    })

    MigrationUtil.migrate()

    intercept(ApplicationCallPipeline.Call) {
        proceed()
        call.principal<UserData>()?.run {
            LastActivityService.mark(id)
        }
    }

    routing {
        statusPages()
        authentication { simpleJWT.sign(it) }
        check()
        authenticate {
            users()
            payments()
            events()
            members()
            memberships()
            reports()
            statistics()
            exports()
            products()
            refractions()
            quickpay()
        }
    }
}
