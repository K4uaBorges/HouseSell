plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.1.10"
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
    application
}

application {
    mainClass.set("main.app.AppKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.slf4j:slf4j-api:2.0.12")

    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")

    // Server
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation(platform("org.http4k:http4k-bom:6.1.0.1"))
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-server-jetty")
    implementation("org.http4k:http4k-server-undertow:5.47.0.0")

    implementation("org.slf4j:slf4j-simple:2.0.9") // for logging

    // PostgreSQL JDBC driver
    implementation("org.postgresql:postgresql:42.7.4")

    // JUnit 5 (unit testing)
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(kotlin("test"))

    // If you want DB tests with a real Postgres automatically:
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.4")
}

tasks.register<Copy>("copyRuntimeDependencies") {
    into("build/libs")
    from(configurations.runtimeClasspath)
}

// /Docker related
val dockerExe =
    when (
        org.gradle.internal.os.OperatingSystem
            .current()
    ) {
        org.gradle.internal.os.OperatingSystem.MAC_OS -> "/usr/local/bin/docker"
        org.gradle.internal.os.OperatingSystem.WINDOWS -> "docker"
        else -> "docker"
    }

tasks.register<Exec>("allUp") {
    commandLine(dockerExe, "compose", "up", "--force-recreate", "-d")
}

tasks.register<Exec>("allDown") {
    commandLine(dockerExe, "compose", "down")
}

tasks.register("rebuildAll") {
    dependsOn("allDown")
    finalizedBy("allUp")
}

tasks.register<JavaExec>("runApp") {
    dependsOn("allUp")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("main.app.AppKt")
}

tasks.test {
    useJUnitPlatform()
}
