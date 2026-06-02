@echo off
REM Start IPED Configuration Development Servers

setlocal enabledelayedexpansion

echo Starting IPED Configuration Development Environment...
echo.

set JAR_PATH=L:\Workspace\IPED\iped-engine\target\iped-engine-4.4.0-SNAPSHOT.jar

if not exist "%JAR_PATH%" (
    echo ERROR: JAR not found at %JAR_PATH%
    echo Please run: cd L:\Workspace\IPED\iped-engine ^&^& mvn package -DskipTests
    exit /b 1
)

echo Starting ConfigurationServer on port 8080...
start "IPED ConfigurationServer" java -cp "%JAR_PATH%" iped.engine.config.api.ConfigurationServer 8080

echo ConfigurationServer started
echo Waiting 3 seconds for server to start...
timeout /t 3 /nobreak

echo.
echo Starting React Development Server on port 3000...
echo Press Ctrl+C to stop both servers
echo.

cd /d "L:\Workspace\IPED\iped-ui"
npm start

echo.
echo Development environment stopped.
