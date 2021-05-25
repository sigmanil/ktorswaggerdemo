package smn.ktorexample

import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.throws
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlin.NoSuchElementException

fun main() {
    embeddedServer(Netty, port = 9004) {
        crudExampleWithSpecificErrorHandling()
    }.start(wait = true)
}

fun Application.crudExampleWithSpecificErrorHandling() {
    theSameSetupStuffAsInApp1PlusJacksonTimeConfig()

    //KtorOpenApiGen routing and endpoint definition
    apiRouting {
        route("/cars") {
            setupCarCreation()
            setupMultiCarRetrieval()

            handle404 {
                setupSingleCarRetrieval()
                setupCarUpdating()
                setupCarDeletion()
            }
        }
    }
}

inline fun NormalOpenAPIRoute.handle404(crossinline setup: NormalOpenAPIRoute.() -> Unit) {
    throws<NormalOpenAPIRoute, NoSuchElementException>(
        status = HttpStatusCode.NotFound.description("The requested car was not found.")
    ) {
        setup()
    }
}
