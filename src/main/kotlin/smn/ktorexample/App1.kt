package smn.ktorexample

import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.Request
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.openAPIGen
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.status
import com.papsign.ktor.openapigen.schema.namer.DefaultSchemaNamer
import com.papsign.ktor.openapigen.schema.namer.SchemaNamer
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlin.reflect.KType

fun main() {
    embeddedServer(Netty, port = 9001) {
        helloWorldWithoutOpenApi()
        helloWorldWithOpenApi()
    }.start(wait = true)
}

fun Application.helloWorldWithoutOpenApi() {
    routing {
        route ("/pure") {
            get("{name}") {
                val name = call.parameters["name"]
                call.respondText("Hello, $name!")
            }
        }
    }
}




fun Application.helloWorldWithOpenApi() {
    //This is normal/pure ktor - just tell ktor to use jackson for json-stuff.
    install(ContentNegotiation) {
        jackson { /* We could configure jackson here */ }
    }

    //Install the OpenAPIGen feture - and configure it with some info and a "fix".
    install(OpenAPIGen) {
        addMainOpenApiInfo()
        addOpenApiNamingFix()
    }

    //Normal/pure ktor - serve the OpenApi documentation and SwaggerUI
    routing {
        swaggerRoutes()
    }

    //KtorOpenApiGen routing and endpoint definition
    apiRouting {
        route("/openapi") {
            get<HelloParams, String>(
                status(HttpStatusCode.OK),
                info(
                    "Hello world endpoint",
                    "This endpoint responds with a friendly greeting using the provided name."
                )
            ) { (name) ->
                respond("Hello, $name!")
            }
        }
    }
}

@Request("Request parameters to trigger a hello response.")
@Path("{name}")
data class HelloParams(
    @PathParam("The name which will be used in the hello-response.")
    val name: String
)




fun OpenAPIGen.Configuration.addMainOpenApiInfo() {
    info {
        version = "1"
        title = "Knowit Ktor/Openapi-foredrag"
        description = "Eksempelapplikasjon for knowit."
        contact {
            name = "Sigmund Marius Nilssen"
            email = "smn@knowit.no"
        }
    }

    // describe the servers, add as many as you want
    server("/") {
        description = "This server"
    }
}






fun OpenAPIGen.Configuration.addOpenApiNamingFix() {
    //Schema object namer. Quick&Dirty solution to adhere to OpenAPI naming spec.
    replaceModule(DefaultSchemaNamer, object: SchemaNamer {
        val regex = Regex("[A-Za-z0-9_.]+")
        override fun get(type: KType): String {
            return type.toString().replace(regex) {
                it.value.split(".").last()
            }.replace(Regex(">|<|, "), "_")
        }
    })
}







fun Application.swaggerRoutes() {
    routing {
        get("/openapi.json") {
            call.respond(application.openAPIGen.api.serialize())
        }

        get("/") {
            call.respondRedirect("/swagger-ui/index.html?url=/openapi.json", true)
        }
    }
}
