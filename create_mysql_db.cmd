@echo off
REM Helper to create hpms2_db database and user using MySQL CLI. Requires mysql on PATH and root password input.
REM Usage: create_mysql_db.cmd

echo Creating database hpms2_db and user hpmsuser...
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS hpms2_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE USER IF NOT EXISTS 'hpmsuser'@'localhost' IDENTIFIED BY 'StrongPasswordHere'; GRANT ALL PRIVILEGES ON hpms2_db.* TO 'hpmsuser'@'localhost'; FLUSH PRIVILEGES;"
if %ERRORLEVEL% NEQ 0 (
    echo Failed to execute MySQL commands. Ensure mysql client is installed and on PATH.
    exit /b %ERRORLEVEL%
)

echo Done.
pause
