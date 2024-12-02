package io.github.goquati.shurl

import io.github.goquati.shurl.UserData.Companion.USER_DATA_AGENT_MAX_LENGTH
import io.github.goquati.shurl.UserData.Companion.USER_DATA_IP_MAX_LENGTH
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Statement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.withContext
import org.intellij.lang.annotations.Language


suspend fun io.r2dbc.spi.Connection.findUrl(id: UrlId): String = execute(
    "SELECT original_url FROM ${Config.schema}.${Config.tableUrls} WHERE id = ${'$'}1",
    statementBuilder = { bind(0, id.id) }
) { row, _ ->
    row.get(0) as String
}.single()

suspend fun io.r2dbc.spi.Connection.addVisit(id: UrlId, userData: UserData) = execute(
    "INSERT INTO ${Config.schema}.${Config.tableVisits} (url,ip,agent) VALUES (${'$'}1,${'$'}2,${'$'}3)",
    statementBuilder = {
        bind(0, id.id)
        bindNullable(1, userData.ip)
        bindNullable(2, userData.agent)
    }
).collect()

suspend fun io.r2dbc.spi.Connection.initDb() = execute(
    """
    CREATE SCHEMA IF NOT EXISTS ${Config.schema};
    
    CREATE OR REPLACE FUNCTION ${Config.schema}.generate_random_url() RETURNS VARCHAR(${Config.urlIdSize}) AS
    ${'$'}${'$'}
    DECLARE
        char_set  TEXT := '${Config.urlCharSet.joinToString("")}';
        id_length INT  := ${Config.urlIdSize};
        random_id VARCHAR(${Config.urlIdSize});
    BEGIN
        LOOP
            SELECT string_agg(substr(char_set, trunc(random() * length(char_set) + 1)::int, 1), '')
            INTO random_id
            FROM generate_series(1, id_length);
            IF NOT EXISTS (SELECT 1 FROM ${Config.schema}.${Config.tableUrls} WHERE id = random_id) THEN
                EXIT;
            END IF;
        END LOOP;
        RETURN random_id;
    END;
    ${'$'}${'$'} LANGUAGE plpgsql;
    
    CREATE TABLE IF NOT EXISTS ${Config.schema}.${Config.tableUrls}
    (
        id VARCHAR(${Config.urlIdSize}) DEFAULT ${Config.schema}.generate_random_url() NOT NULL
            CONSTRAINT ${Config.schema}_${Config.tableUrls}_pk
                PRIMARY KEY,
        created      TIMESTAMP DEFAULT now() NOT NULL,
        original_url VARCHAR(${Config.originalUrlMaxSize})            NOT NULL
    );
    CREATE TABLE IF NOT EXISTS ${Config.schema}.${Config.tableVisits}
    (
        created TIMESTAMP DEFAULT now()             NOT NULL,
        url     VARCHAR(${Config.urlIdSize})                   NOT NULL
            CONSTRAINT ${Config.schema}_${Config.tableVisits}_${Config.tableUrls}_fk
                REFERENCES ${Config.schema}.${Config.tableUrls} (id),
        ip      VARCHAR($USER_DATA_IP_MAX_LENGTH),
        agent   VARCHAR($USER_DATA_AGENT_MAX_LENGTH)
    );
    """.trimIndent()
).collect()

fun createDbPool(): ConnectionPool {
    val dbConfig = ConnectionFactoryOptions.builder().apply {
        option(ConnectionFactoryOptions.DRIVER, "pool")
        option(ConnectionFactoryOptions.PROTOCOL, "postgresql")
        option(ConnectionFactoryOptions.HOST, Config.Db.host)
        option(ConnectionFactoryOptions.PORT, Config.Db.port)
        option(ConnectionFactoryOptions.DATABASE, Config.Db.database)
        option(ConnectionFactoryOptions.USER, Config.Db.user)
        option(ConnectionFactoryOptions.PASSWORD, Config.Db.password)
    }.build()
    val connectionFactory = ConnectionFactories.get(dbConfig)
    val dbPoolConfig = ConnectionPoolConfiguration.builder(connectionFactory)
        .initialSize(Config.DbPool.initialSize)
        .maxSize(Config.DbPool.maxSize)
        .build()
    return ConnectionPool(dbPoolConfig)
}

suspend fun <T> ConnectionPool.create(block: suspend (io.r2dbc.spi.Connection) -> T): T = withContext(Dispatchers.IO) {
    val connection = create().awaitSingle()
    try {
        block(connection)
    } finally {
        connection.close().awaitFirstOrNull()
    }
}

private fun io.r2dbc.spi.Connection.execute(
    @Language("sql") sql: String,
    statementBuilder: Statement.() -> Unit = {},
) = execute(sql = sql, statementBuilder = statementBuilder) { _, _ -> }

@OptIn(ExperimentalCoroutinesApi::class)
private fun <T : Any> io.r2dbc.spi.Connection.execute(
    @Language("sql") sql: String,
    statementBuilder: Statement.() -> Unit = {},
    block: (io.r2dbc.spi.Row, io.r2dbc.spi.RowMetadata) -> T,
) = flow<T> {
    createStatement(sql, statementBuilder)
        .execute().asFlow()
        .flatMapConcat { it.map(block).asFlow() }
        .collect { emit(it) }
}

private fun io.r2dbc.spi.Connection.createStatement(
    @Language("sql") sql: String,
    block: Statement.() -> Unit,
) = createStatement(sql).apply(block)

inline fun <reified T : Any> Statement.bindNullable(i: Int, value: T?) {
    if (value != null)
        bind(i, value)
    else
        bindNull(i, T::class.java)
}
