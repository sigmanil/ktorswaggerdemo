package smn.ktorexample

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.Request
import com.papsign.ktor.openapigen.annotations.Response
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.annotations.properties.description.Description
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.*
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.status
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.time.LocalDate
import java.util.*
import kotlin.NoSuchElementException

fun main() {
    embeddedServer(Netty, port = 9002) {
        crudExample()
    }.start(wait = true)
}

object CarRegistry {
    private var cars: MutableMap<String, Car> = mutableMapOf()

    fun doesCarExist(id: String): Boolean = cars.containsKey(id)
    fun insertOrUpdateCar(car: Car): Car { cars[car.id] = car; return car }
    fun retrieveCarById(id: String): Car? = cars[id]
    fun retrieveAllCars(): List<Car> = cars.values.toList()
    fun deleteCar(id: String): Boolean = cars.remove(id) != null
}

enum class Color { RED, BLUE, GREEN, YELLOW, BLACK, WHITE, BEIGE }

@Response("A car as stored in the system")
data class Car(
    @Description("The system ID of a car, by which it can be retrieved or deleted")
    val id: String,
    @Description("The color of the car, included to demonstrate enums")
    val color: Color,
    @Description("The production year of the car, included to demonstrate dates")
    val productionYear: LocalDate,
    @Description("The nickname of the car, included to demonstrate nullables and default values")
    val nickName: String? = null
)



fun Application.crudExample() {
    //Moved json config, openapigen installation etc to a function to get it out of the way. Added some lines
    //to tell Jackson to treat time sensibly.
    theSameSetupStuffAsInApp1PlusJacksonTimeConfig()

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






// CREATION

fun NormalOpenAPIRoute.setupCarCreation() {
    post<Unit, Car, CarCreationDto>(
        status(HttpStatusCode.OK),
        info(
            "Car data creation",
            "This endpoints lets you insert a new car into the database. An ID will be generated for you."
        )
    ) { _, carToCreate ->
        val id = UUID.randomUUID().toString()
        val car = CarRegistry.insertOrUpdateCar(carToCreate.toCarWithId(id))
        respond(car)
    }
}

@Request("Details of a car to be created")
data class CarCreationDto(
    @Description("The color of the car, included to demonstrate enums")
    val color: Color,
    @Description("The production year of the car, included to demonstrate dates")
    val productionYear: LocalDate,
    @Description("The nickname of the car, included to demonstrate nullables and default values")
    val nickName: String? = null
) {
    fun toCarWithId(id: String): Car {
        return Car(
            id = id,
            color = color,
            productionYear = productionYear,
            nickName = nickName
        )
    }
}





// RETRIEVAL OF A SINGLE CAR

fun NormalOpenAPIRoute.setupSingleCarRetrieval() {
    get<CarByIdParam, Car>(
        status(HttpStatusCode.OK),
        info(
            "Car retrieval by id",
            "This endpoint responds with a representation of the car given the id."
        )
    ) { (id) ->
        val car = CarRegistry.retrieveCarById(id) ?: throw NoSuchElementException("Car with id $id not found.")
        respond(car)
    }
}

@Request("Request parameters to specify a car by its id.")
@Path("{id}")
data class CarByIdParam(
    @PathParam("The ID of a car.")
    val id: String
)





//RETRIEVAL OF MULTIPLE CARS
fun NormalOpenAPIRoute.setupMultiCarRetrieval() {
    get<CarsByColorsParam, List<Car>>(
        status(HttpStatusCode.OK),
        info(
            "Car retrieval",
            "This endpoint responds with a representation of all the cars in the database, unless one or more colors are specified, in which case only cars with the given colors are listed."
        )
    ) { (colors) ->
        val allCars = CarRegistry.retrieveAllCars()

        val carsToReturn = if (colors == null || colors.isEmpty()) {
            allCars
        } else {
            allCars.filter { colors.contains(it.color) }
        }

        respond(carsToReturn)
    }
}

@Request("Request parameters to retrieve cars by their Color.")
data class CarsByColorsParam(
    @QueryParam("The colors to retrieve cars for. Optional. If omitted, all cars will be retrieved.")
    val colors: List<Color>? = emptyList()
)





// CAR UPDATE

fun NormalOpenAPIRoute.setupCarUpdating() {
    put<CarByIdParam, Car, CarCreationDto>(
        status(HttpStatusCode.OK),
        info(
            "Car data update",
            "This endpoints lets you update what is stored about a car. The entire car object is updated according to input data, left out fields will be deleted."
        )
    ) { (id), updatedCar ->

        if (!CarRegistry.doesCarExist(id)) {
            throw NoSuchElementException("Car not found, cannot update.")
        }

        val car = CarRegistry.insertOrUpdateCar(updatedCar.toCarWithId(id))
        respond(car)
    }
}




// CAR DELETION
fun NormalOpenAPIRoute.setupCarDeletion() {
    delete<CarByIdParam, Unit>(
        info(
            "Car retrieval",
            "This endpoint responds with a representation of all the cars in the database, unless one or more colors are specified, in which case only cars with the given colors are listed."
        )
    ) { (id) ->
        val deleted = CarRegistry.deleteCar(id)

        if (deleted) {
            respond(Unit)
        } else {
            throw NoSuchElementException("Car not found, could not be deleted.")
        }
    }
}












fun Application.theSameSetupStuffAsInApp1PlusJacksonTimeConfig() {
    //This is normal/pure ktor - just tell ktor to use jackson for json-stuff.
    install(ContentNegotiation) {
        jackson {
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
            registerModule(JavaTimeModule())
        }
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
}