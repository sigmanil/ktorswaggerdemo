package smn.ktorexample

import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class App1Test {
    @Test
    fun testHelloWorldWithoutOpenApi() {
        withTestApplication({
            helloWorldWithoutOpenApi()
        }) {
            handleGet("/pure/Sigmund") {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Hello, Sigmund!", response.content)
            }
        }
    }

    @Test
    fun testHelloWorldWithOpenApi() {
        withTestApplication({
            helloWorldWithOpenApi()
        }) {
            handleGet("/openapi/Sigmund") {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Hello, Sigmund!", response.content)
            }
        }
    }
}




//Helper functions to be reused across all tests
// - would be in separate file in a real code base.

fun TestApplicationEngine.handleGet(
    requestUri: String,
    handleResult: TestApplicationCall.() -> Unit
) {
    with(handleRequest {
        this.uri = requestUri
        this.method = HttpMethod.Get
    }) {
        handleResult()
    }
}