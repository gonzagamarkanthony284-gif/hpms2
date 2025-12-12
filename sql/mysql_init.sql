-- MySQL initialization script for HPMS
-- Run in MySQL client (mysql -u root -p) or using the create_mysql_db.cmd helper

CREATE DATABASE IF NOT EXISTS hpms2_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'hpmsuser'@'localhost' IDENTIFIED BY 'StrongPasswordHere';
GRANT ALL PRIVILEGES ON hpms2_db.* TO 'hpmsuser'@'localhost';
FLUSH PRIVILEGES;
