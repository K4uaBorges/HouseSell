package main.api.httpServer

import org.http4k.core.Method
import org.http4k.core.then
import org.http4k.filter.AllowAllOriginPolicy
import org.http4k.filter.CorsPolicy
import org.http4k.filter.ServerFilters
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.singlePageApp
import org.http4k.server.Http4kServer
import org.http4k.server.Undertow
import org.http4k.server.asServer
import java.io.File

class HousesRouter(
    private val webApi: HousesWebApi,
    private val port: Int = 8080,
) {
    private val spa = singlePageApp(ResourceLoader.Directory(resolveStaticContentDirectory()))
    private val corsPolicy =
        CorsPolicy(
            originPolicy = AllowAllOriginPolicy,
            headers = listOf("content-type", "authorization"),
            methods = Method.entries,
            credentials = true,
        )

    private val app =
        ServerFilters
            .Cors(corsPolicy)
            .then(
                routes(
                    "/api" bind webApi.routes,
                    "/" bind spa,
                ),
            )

    private fun resolveStaticContentDirectory(): String {
        val startDir = File(System.getProperty("user.dir")).absoluteFile
        var current: File? = startDir

        while (current != null) {
            val directCandidate = File(current, "static-content")
            if (directCandidate.isDirectory) return directCandidate.path
            current = current.parentFile
        }

        val childCandidate =
            startDir
                .listFiles()
                ?.asSequence()
                ?.filter { it.isDirectory }
                ?.map { File(it, "static-content") }
                ?.firstOrNull { it.isDirectory }

        return childCandidate?.path ?: "static-content"
    }

    fun start(): Http4kServer = app.asServer(Undertow(port)).start()
}
