-- MateCloud Database Initialization
-- Run this script as MySQL root user to create all databases

CREATE DATABASE IF NOT EXISTS `mate_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS `mate_notice` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- Grant permissions (adjust user/password as needed)
-- CREATE USER IF NOT EXISTS 'mate'@'%' IDENTIFIED BY 'mate123';
-- GRANT ALL PRIVILEGES ON `mate_admin`.* TO 'mate'@'%';
-- GRANT ALL PRIVILEGES ON `mate_system`.* TO 'mate'@'%';
-- GRANT ALL PRIVILEGES ON `mate_notice`.* TO 'mate'@'%';
-- FLUSH PRIVILEGES;

-- Note: Table creation and seed data are handled by Flyway on first startup.
-- Just create the databases, then start the services.
