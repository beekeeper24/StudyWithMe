# PostgreSQL, Flyway, Member Baseline

Use Docker Compose for the local PostgreSQL dependency.

```bash
docker compose up -d postgres
```

The app defaults to:

- app port `8081`
- `jdbc:postgresql://localhost:15432/studywithme`
- username `studywithme`
- password `studywithme`

Override those values with `SERVER_PORT`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` outside local development.

The Compose host port intentionally uses `15432` to avoid conflicts with a PostgreSQL process already using the default `5432` port on the developer machine.

Flyway is the schema owner. Keep Hibernate `ddl-auto` at `validate` for the main app so entity/schema drift fails early.

The initial member identity rule is:

- OAuth account uniqueness: `(oauth_provider, oauth_subject)`
- nickname uniqueness: `nickname`
- roles live in `member_roles`
