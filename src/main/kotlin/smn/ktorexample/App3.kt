package smn.ktorexample


import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.interop.withAPI
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.route
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlin.NoSuchElementException

fun main() {
    embeddedServer(Netty, port = 9003) {
        crudExampleWithStatusPages()
    }.start(wait = true)
}

fun Application.crudExampleWithStatusPages() {
    theSameSetupStuffAsInApp2PlusStatusPages()

    //KtorOpenApiGen routing and endpoint definition
    apiRouting {
        route("/cars") {

            //C
            setupCarCreation()

            //R
            setupSingleCarRetrieval()
            setupMultiCarRetrieval()

            //U
            setupCarUpdating()

            //D
            setupCarDeletion()
        }
    }
}


data class CarRegistryProblem(val msg: String)

fun Application.theSameSetupStuffAsInApp2PlusStatusPages() {
    //This is normal/pure ktor - just tell ktor to use jackson for json-stuff.
    install(ContentNegotiation) {
        jackson {
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
            registerModule(JavaTimeModule())
        }
    }

    //Install the OpenAPIGen feture - and configure it with some info and a "fix".
    val openApiGen = install(OpenAPIGen) {
        addMainOpenApiInfo()
        addOpenApiNamingFix()
    }

    install(StatusPages) {
        withAPI(openApiGen) {
            //This will be in the API documentation for every request
            exception<NoSuchElementException, CarRegistryProblem>(
                HttpStatusCode.NotFound
            ) { e ->
                // Please avoid exposing application technology details in real applications.
                // (https://owasp.org/www-community/Improper_Error_Handling)
                // Such details may well leak in unplanned exceptions. Do not include message in the way
                // we do here without having some safety mechanism.
                CarRegistryProblem(e.localizedMessage)
            }

            //Intentionally un-documented behavior - won't be in the api docs:
            exception<Exception> { e ->
                call.respondText(
                    "An internal problem has occurred. Contact the system administrator and have them check the logs.",
                    ContentType.Text.Plain,
                    HttpStatusCode.InternalServerError
                )
            }
        }
    }

    //Normal/pure ktor - serve the OpenApi documentation and SwaggerUI
    routing {
        swaggerRoutes()
    }
}
