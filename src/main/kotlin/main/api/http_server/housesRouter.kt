package main.api.http_server

import org.http4k.server.Http4kServer
import org.http4k.server.Undertow
import org.http4k.server.asServer

/**
 * Alterar quando obtiver a certeza da porta de entrar que realmente o servidor irá ter,
 * Por enquanto mantem a porta de entrada da database
 */

class HousesRouter(
    private val webApi: HousesWebApi,
    private val port: Int = 5433,
    ) {
        fun start(): Http4kServer = webApi.routes.asServer(Undertow(port)).start()
    }


