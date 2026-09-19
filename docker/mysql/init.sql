-- ==============================================================================
-- AdServe — MySQL Database Initialization Script
-- Executed on container creation when mysql_data volume is freshly initialized.
-- Note: Spring Boot Data JPA (Hibernate) manages schema lifecycle via ddl-auto=update.
-- DO NOT add destructive DROP DATABASE or DROP TABLE statements here.
-- ==============================================================================

-- Ensure database exists with utf8mb4 collation
CREATE DATABASE IF NOT EXISTS adserve_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE adserve_db;

-- Database initialization marker
SELECT 'AdServe MySQL Database successfully initialized. Tables will be maintained by Hibernate JPA.' AS status;
