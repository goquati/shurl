# Shurl: A URL Shortener Built with Ktor and PostgreSQL (R2DBC)

Shurl is a lightweight URL shortener service built using [Ktor](https://ktor.io/) and PostgreSQL
with [R2DBC](https://r2dbc.io/). URLs are managed directly via SQL inserts, making it suitable for environments where
programmatic URL shortening is handled outside of the application.

---

## Features

- **Reactive Database Backend**: Powered by R2DBC for non-blocking operations.
- **Customizable Configuration**: Configure schema, table names, URL ID length, and character set via environment
  variables.
- **Minimal API Surface**: Focused only on redirecting to the original URL based on the shortened URL ID.

---

## Configuration

You can customize Shurl's behavior using environment variables.

### Available Variables

| Variable Name                 | Description                                | Default Value                                                |
|-------------------------------|--------------------------------------------|--------------------------------------------------------------|
| `SHURL_SCHEMA_NAME`           | Database schema name                       | `shurl`                                                      |
| `SHURL_TABLE_NAME_URLS`       | Table name for storing URLs                | `urls`                                                       |
| `SHURL_ORIGINAL_URL_MAX_SIZE` | Maximum size for original URLs             | `512`                                                        |
| `SHURL_URL_ID_SIZE`           | Length of the generated short URL IDs      | `6`                                                          |
| `SHURL_URL_CHARSET`           | Character set for generating short URL IDs | `"23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz"` |
| `SHURL_DB_HOST`               | Database host                              | `localhost`                                                  |
| `SHURL_DB_PORT`               | Database port                              | `5432`                                                       |
| `SHURL_DB_DATABASE`           | Database name                              | `postgres`                                                   |
| `SHURL_DB_USER`               | Database user                              | `postgres`                                                   |
| `SHURL_DB_PASSWORD`           | Database password                          | `postgres`                                                   |

---

## Example Workflow

1. **Run Shurl with Docker**
   ```bash
   docker run -p 8000:8000 goquati/shurl:latest
   ```

2. **Insert a URL into the Database**
   ```sql
   INSERT INTO shurl.urls (original_url) VALUES ('https://quati.de') RETURNING id;
   ```

3. **Access the Shortened URL**  
   Open your browser and visit:  
   `http://localhost:8000/<id>`  
   This will redirect you to `https://quati.de`.

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

## Contributions

Contributions, issues, and feature requests are welcome! Feel free to open an issue or submit a pull request.
