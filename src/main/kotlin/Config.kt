package io.github.goquati.shurl

import io.r2dbc.postgresql.client.SSLMode

data object Config {
    val schema: String = System.getenv("SHURL_SCHEMA_NAME")?.validSqlName ?: "shurl"
    val tableUrls: String = System.getenv("SHURL_TABLE_NAME_URLS")?.validSqlName ?: "urls"
    val tableVisits: String = System.getenv("SHURL_TABLE_NAME_VISITS")?.validSqlName ?: "visits"
    val originalUrlMaxSize: Int = System.getenv("SHURL_ORIGINAL_URL_MAX_SIZE")?.toInt() ?: 512
    val urlIdSize: Int = System.getenv("SHURL_URL_ID_SIZE")?.toInt() ?: 6
    val urlCharSet: Set<Char> = System.getenv("SHURL_URL_CHARSET")?.replace("'", "''")?.toSet()
        ?: "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz".toSet()

    object Db {
        val host = System.getenv("SHURL_DB_HOST") ?: "localhost"
        val port = System.getenv("SHURL_DB_PORT")?.toInt() ?: 5432
        val database = System.getenv("SHURL_DB_DATABASE") ?: "postgres"
        val user = System.getenv("SHURL_DB_USER") ?: "postgres"
        val password = System.getenv("SHURL_DB_PASSWORD") ?: "postgres"
        val sslMode = System.getenv("SHURL_DB_SSL_MODE")?.let { SSLMode.fromValue(it) }
    }

    object DbPool {
        val initialSize = System.getenv("SHURL_DB_POOL_INITIAL_SIZE")?.toInt() ?: 1
        val maxSize = System.getenv("SHURL_DB_POOL_MAX_SIZE")?.toInt() ?: 5
    }

    private val String.validSqlName: String
        get() {
            if (!matches(Regex("^[a-zA-Z0-9_]+$"))) error("Invalid SQL name: '$this'")
            return this
        }
}