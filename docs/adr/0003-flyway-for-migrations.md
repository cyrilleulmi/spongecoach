# Flyway for schema migrations

Chose Flyway over Liquibase for Postgres schema migrations under Quarkus. Flyway's plain-SQL migrations are simpler to read and write than Liquibase's XML/YAML abstraction, and `quarkus-flyway` is a first-class extension. Liquibase's database-portability feature (its main differentiator) buys nothing here since Postgres is the only target.
