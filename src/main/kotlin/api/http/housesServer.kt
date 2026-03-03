package api.http

import org.http4k.server.Http4kServer
import org.http4k.server.Undertow
import org.http4k.server.asServer

class HousesServer(
    private val webApi: HousesWebApi,
    private val port: Int = 8080,
) {
    fun start(): Http4kServer = webApi.routes.asServer(Undertow(port)).start()
}
