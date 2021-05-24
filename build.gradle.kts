import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

plugins {
    val kotlinversion = "1.4.31"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    kotlin("jvm") version kotlinversion
    kotlin("plugin.serialization") version kotlinversion
    kotlin("kapt") version kotlinversion
    application

}
val applicationVersion = "0.0.1-SNAPSHOT"

group = "smn.kotlinexample"
version = applicationVersion
java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

repositories {
    mavenCentral()
    jcenter()
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    //Logging
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("ch.qos.logback:logback-core:1.2.3")
    implementation("org.slf4j:slf4j-api:1.7.26")
    implementation("io.github.microutils:kotlin-logging:1.7.8")

    //Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testImplementation("io.mockk:mockk:1.10.6")

    //KTor stuff
    val ktor_version = "1.5.3"

    //KTor
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-locations:$ktor_version")
    implementation("io.ktor:ktor-gson:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-gson:$ktor_version")
    implementation("io.ktor:ktor-serialization:$ktor_version")
    implementation("io.ktor:ktor-jackson:$ktor_version")
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")

    //Note: 2.10.2 is the relevant jackson-version as of ktor-jackson 1.5.2. Keep an eye on this when upgrading ktor.
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.10.2")

    //KTor OpenAPI-generation
    implementation("com.github.papsign:Ktor-OpenAPI-Generator:0.2-beta.16")
}

tasks.test {
    useJUnitPlatform()
    //Tests should run in production-like settings, and we're expecting system timezone to be UTC in production
    systemProperty("user.timezone", "UTC")
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat=org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

application {
    mainClassName = "smn.ktorexample.MainKt"
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    manifest.attributes["Specification-Version"] = applicationVersion
    manifest.attributes["Implementation-Version"] = LocalDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    //Necessary for embedded appservers (among other things) to work from built jar. See: https://stackoverflow.com/questions/48636944/how-to-avoid-a-java-lang-exceptionininitializererror-when-trying-to-run-a-ktor-a
    mergeServiceFiles()
}
