package main.api.http_server

import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.singlePageApp
import org.http4k.server.Http4kServer
import org.http4k.server.Undertow
import org.http4k.server.asServer

class HousesRouter(
    private val webApi: HousesWebApi,
    private val port: Int = 8080,
) {
    private val spa = singlePageApp(ResourceLoader.Directory("static-content"))

    private val app =
        routes(
            "/api" bind webApi.routes,
            "/" bind spa,
        )

    fun start(): Http4kServer = app.asServer(Undertow(port)).start()
}
