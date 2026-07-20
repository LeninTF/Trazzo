-- Hibernate maps AuditEntity.action with @Enumerated(STRING), binding it as a plain varchar
-- bind parameter. PostgreSQL's native enum type (action_enum) refuses to compare against a
-- varchar without an explicit cast, breaking any JPQL query that filters on it. Switch the
-- column to VARCHAR to match how it's actually queried (same approach already used for
-- incidents.state, avoiding native Postgres enums on JPA-mapped columns).
ALTER TABLE audit ALTER COLUMN action TYPE VARCHAR(20) USING action::text;
