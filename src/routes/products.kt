package hr.askzg.routes

import hr.askzg.db.Product
import hr.askzg.service.ProductService
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*

fun Route.products() {

    route("product") {

        get {
            call.respond(ProductService.getAll())
        }
        get("{id}") {
            call.respond(ProductService.get(call.pathId()))
        }
        post {
            val product = call.forAdd<Product>()
            val saved = ProductService.save(product)
            call.respond(saved)
        }
        put {
            call.respond(ProductService.save(call.forUpdate()))
        }
        put("pay") {
            val request = call.receive<PayProductRequest>()
            call.respond(ProductService.pay(request.productId!!, request.memberId!!))
        }
        delete("{id}") {
            ProductService.delete(call.pathId())
            call.respond(HttpStatusCode.OK)
        }
    }
}

class PayProductRequest {
    var productId: Int? = null
    var memberId: Int? = null
}