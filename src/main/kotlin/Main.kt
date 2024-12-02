package io.github.goquati.shurl

import io.github.goquati.shurl.UserData.Companion.toUserData
import io.ktor.http.*
import io.ktor.server.application.install
import io.ktor.server.cio.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory


suspend fun main() {
    val logger = LoggerFactory.getLogger("shurl")
    val pool = createDbPool()
    pool.create { it.initDb() }

    embeddedServer(CIO, port = 8000) {
        install(ForwardedHeaders)
        install(XForwardedHeaders)
        routing {
            get("/{id}") {
                try {
                    val id = call.parameters["id"].toUrlId()
                    val url = pool.create { conn ->
                        val url = conn.findUrl(id)
                        runCatching {
                            val userData = call.request.toUserData()
                            conn.addVisit(id = id, userData = userData)
                        }.onFailure { logger.warn("failed to save request info") }
                        url
                    }
                    call.respondRedirect(url, permanent = true)
                    logger.info("successfully redirect, id: $id")
                } catch (ex: Throwable) {
                    val code = when (ex) {
                        is ShUrlException -> ex.code
                        is NoSuchElementException -> HttpStatusCode.NotFound
                        else -> HttpStatusCode.InternalServerError
                    }
                    call.response.status(code)
                    logger.error("failed to redirect, code: $code")
                }
            }
        }
    }.start(wait = true)
}
