package io.github.goquati.shurl

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.origin
import io.ktor.server.routing.RoutingRequest
import kotlin.text.take

class ShUrlException(val code: HttpStatusCode, msg: String) : Throwable(message = msg)

@JvmInline
value class UrlId(val id: String) {
    override fun toString() = id
}

fun String?.toUrlId(): UrlId = this
    ?.takeIf { it.length in 1..Config.urlIdSize }
    ?.takeIf { (it.toSet() - Config.urlCharSet).isEmpty() }
    ?.let { UrlId(it) }
    ?: throw ShUrlException(HttpStatusCode.BadRequest, "invalid id")


data class UserData(
    val agent: String?,
    val ip: String?,
) {
    companion object {
        const val USER_DATA_AGENT_MAX_LENGTH = 512
        const val USER_DATA_IP_MAX_LENGTH = 40

        fun RoutingRequest.toUserData(): UserData {
            val ip = runCatching { origin.remoteAddress }.getOrNull()
            val agent = headers["User-Agent"]
            return UserData(
                agent = agent?.take(USER_DATA_AGENT_MAX_LENGTH),
                ip = ip?.take(USER_DATA_IP_MAX_LENGTH),
            )
        }
    }
}