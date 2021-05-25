package smn.ktorexample

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.Request
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.properties.description.Description
import com.papsign.ktor.openapigen.model.Described
import com.papsign.ktor.openapigen.model.security.HttpSecurityScheme
import com.papsign.ktor.openapigen.model.security.SecuritySchemeModel
import com.papsign.ktor.openapigen.model.security.SecuritySchemeType
import com.papsign.ktor.openapigen.modules.providers.AuthProvider
import com.papsign.ktor.openapigen.route.*
import com.papsign.ktor.openapigen.route.path.auth.*
import com.papsign.ktor.openapigen.route.path.normal.*
import com.papsign.ktor.openapigen.route.response.respond
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.pipeline.*
import java.time.LocalDate
import java.util.*
import kotlin.NoSuchElementException

fun main() {
    embeddedServer(Netty, port = 9005) {
        crudExampleWithAuthentication()
    }.start(wait = true)
}

fun Application.crudExampleWithAuthentication() {
    //Same as before, setup jackson.
    install(ContentNegotiation) {
        jackson {
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
            registerModule(JavaTimeModule())
        }
    }

    //Same as before, except we also install a BasicProvider, which gives Ktor-OpenApiGen info about the authentication
    install(OpenAPIGen) {
        addModules(BasicProvider)
        addMainOpenApiInfo()
        addOpenApiNamingFix()
    }

    //Same as before,setup swagger.
    routing {
        swaggerRoutes()
    }

    //This is the usual ktor setup of Basic authentication.
    install(Authentication) {
        basic("auth-basic") {
            realm = "Access to the '/' path"
            validate { credentials ->
                if (credentials.name == "user" && credentials.password == "pass") {
                    UserPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    }

    //KtorOpenApiGen routing and endpoint definition
    apiRouting {
        route("/cars") {
            setupMultiCarRetrieval()
            setupSingleCarRetrieval()

            //This is where you tell Ktor-OpenApiGen what routes should be authenticated
            auth {
                setupAuthenticatedCarCreation()
                setupAuthenticatedCarUpdating()
                setupAuthenticatedCarDeletion()
            }
        }

        auth { setupAuthenticatedHelloWorld() }
    }
}




data class UserPrincipal(
    val userId: String
) : Principal





object BasicProvider : AuthProvider<UserPrincipal> {
    override val security: Iterable<Iterable<AuthProvider.Security<*>>> =
        listOf(listOf(
            AuthProvider.Security(
                SecuritySchemeModel(
                    SecuritySchemeType.http,
                    scheme = HttpSecurityScheme.basic,
                    name = "auth-basic"
                ),
                emptyList<Scopes>()
            )
        ))

    override suspend fun getAuth(pipeline: PipelineContext<Unit, ApplicationCall>): UserPrincipal {
        return pipeline.context.authentication.principal() ?: throw RuntimeException("No principal")
    }

    override fun apply(route: NormalOpenAPIRoute): OpenAPIAuthenticatedRoute<UserPrincipal> {
        val authenticatedKtorRoute = route.ktorRoute.authenticate { }
        return OpenAPIAuthenticatedRoute(authenticatedKtorRoute, route.provider.child(), this)
    }
}





enum class Scopes(override val description: String) : Described





inline fun NormalOpenAPIRoute.auth(route: OpenAPIAuthenticatedRoute<UserPrincipal>.() -> Unit): OpenAPIAuthenticatedRoute<UserPrincipal> {
    val authenticatedKtorRoute = this.ktorRoute.authenticate("auth-basic") { }
    val openAPIAuthenticatedRoute= OpenAPIAuthenticatedRoute(authenticatedKtorRoute, this.provider.child(), authProvider = BasicProvider)
    return openAPIAuthenticatedRoute.apply {
        route()
    }
}



fun OpenAPIAuthenticatedRoute<UserPrincipal>.setupAuthenticatedCarCreation() {
    post<Unit, Car, CarCreationDto, UserPrincipal> { _, carToCreate ->
        val id = UUID.randomUUID().toString()
        val car = CarRegistry.insertOrUpdateCar(carToCreate.toCarWithId(id))
        respond(car)
    }
}

fun OpenAPIAuthenticatedRoute<UserPrincipal>.setupAuthenticatedCarUpdating() {
    put<CarByIdParam, Car, CarCreationDto, UserPrincipal> { (id), updatedCar ->

        if (!CarRegistry.doesCarExist(id)) {
            throw NoSuchElementException("Car not found, cannot update.")
        }

        val car = CarRegistry.insertOrUpdateCar(updatedCar.toCarWithId(id))
        respond(car)
    }
}

fun OpenAPIAuthenticatedRoute<UserPrincipal>.setupAuthenticatedCarDeletion() {
    delete<CarByIdParam, Unit, UserPrincipal> { (id) ->
        val deleted = CarRegistry.deleteCar(id)

        if (deleted) {
            respond(Unit)
        } else {
            throw NoSuchElementException("Car not found, could not be deleted.")
        }
    }
}

fun OpenAPIAuthenticatedRoute<UserPrincipal>.setupAuthenticatedHelloWorld() {
    route("/hello") {
        get<Unit, String, UserPrincipal> {
            val (username) = principal()
            respond("Hello, $username")
        }
    }
}