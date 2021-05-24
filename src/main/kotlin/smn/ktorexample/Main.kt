package smn.ktorexample

import io.ktor.server.engine.*
import io.ktor.server.tomcat.*

fun main(args: Array<String>) {
    embeddedServer(Tomcat, commandLineEnvironment(args)).start(wait = true)
}
