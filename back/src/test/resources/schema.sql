-- Crea el schema "master" en H2 antes de que Hibernate genere las tablas.
-- En PostgreSQL real este schema ya existe; H2 lo necesita de forma explícita.
CREATE SCHEMA IF NOT EXISTS master;
