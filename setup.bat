@echo off
:: ─────────────────────────────────────────────────────────────────────────────
:: I Love Shopping — Windows setup script
:: Usage:  Double-click setup.bat or run from CMD/PowerShell
:: ─────────────────────────────────────────────────────────────────────────────
setlocal enabledelayedexpansion
title I Love Shopping — Development Setup

echo.
echo ╔══════════════════════════════════════════════════════════════╗
echo ║           I Love Shopping — Development Setup               ║
echo ║         https://github.com/imranshiundu/i-love-shopping1   ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.

:: ── 1. Docker ──────────────────────────────────────────────────────────────────
echo ── 1/7  Checking Docker ──

docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo   Docker is not installed.
    echo.
    echo   Install Docker Desktop for Windows:
    echo     1. Download: https://docs.docker.com/desktop/install/windows-install/
    echo     2. Run the installer and follow the wizard
    echo     3. Restart your computer if prompted
    echo     4. Launch Docker Desktop from the Start Menu
    echo.
    echo   Press any key once Docker Desktop is running, or close to abort...
    pause >nul
)

docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo   Docker is still not available. Install it and re-run this script.
    pause
    exit /b 1
)
echo   ✓ Docker found

docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo   Docker daemon is not running. Starting Docker Desktop...
    start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe" 2>nul
    if %errorlevel% neq 0 (
        start "" "%ProgramFiles%\Docker\Docker\Docker Desktop.exe" 2>nul
    )
    echo   Waiting for Docker daemon (up to 60 seconds^)...
    set /a _retries=0
    :wait_docker
    docker info >nul 2>&1
    if %errorlevel% neq 0 (
        if !\_retries! lss 60 (
            timeout /t 1 /nobreak >nul
            set /a _retries+=1
            goto wait_docker
        )
        echo   Docker daemon still not running after 60 seconds.
        echo   Please open Docker Desktop manually and wait for the whale icon.
        pause
        exit /b 1
    )
)
echo   ✓ Docker daemon is running

:: ── 2. Docker Compose ─────────────────────────────────────────────────────────
echo.
echo ── 2/7  Checking Docker Compose ──

docker compose version >nul 2>&1
if %errorlevel% equ 0 (
    set "DC=docker compose"
    echo   ✓ Docker Compose (plugin) found
    goto :dc_ok
)

docker-compose --version >nul 2>&1
if %errorlevel% equ 0 (
    set "DC=docker-compose"
    echo   ✓ Docker Compose (standalone) found
    goto :dc_ok
)

echo   Docker Compose not found.
echo   Install the Docker Compose plugin via Docker Desktop installer,
echo   or download docker-compose.exe from https://github.com/docker/compose/releases
pause
exit /b 1

:dc_ok

:: ── 3. Java & Maven ──────────────────────────────────────────────────────────
echo.
echo ── 3/7  Checking Java 21 and Maven ──

set JAVA_OK=0
java -version 2>&1 | findstr /R "version " >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
        set "JAVA_VER=%%~g"
    )
    set "JAVA_VER=!JAVA_VER:"=!"
    for /f "delims=.-+" %%a in ("!JAVA_VER!") do set /a JAVA_MAJOR=%%a
    if !JAVA_MAJOR! geq 21 (
        set JAVA_OK=1
        echo   ✓ Java !JAVA_MAJOR! found
    ) else (
        echo   ⚠ Java found but version !JAVA_MAJOR! (need 21+^)
    )
)

if !JAVA_OK! equ 0 (
    echo.
    echo   Java 21+ is required for the backend.
    echo.
    echo   Install options:
    echo     Option A (recommended — SDKMAN):
    echo       Open Git Bash (from Docker Desktop^) and run:
    echo         curl -s 'https://get.sdkman.io' ^| bash
    echo         sdk install java 21.0.3-tem
    echo.
    echo     Option B (manual download):
    echo       https://adoptium.net/temurin/releases/?version=21
    echo       Download the .msi installer for Windows x64
    echo.
    echo   Press any key once Java 21+ is installed, or close to abort...
    pause >nul
    java -version 2>&1 | findstr /R "version " >nul 2>&1
    if %errorlevel% neq 0 (
        echo   Java is still not available.
        pause
        exit /b 1
    )
    echo   ✓ Java confirmed
)

:: Check for mvnw wrapper
if exist ".\backend\mvnw.cmd" (
    echo   ✓ Maven wrapper ready
) else (
    echo   ⚠ Maven wrapper not found — checking system Maven
    mvn --version >nul 2>&1
    if %errorlevel% neq 0 (
        echo   Maven is not installed. Install it or restore the mvnw wrapper.
        pause
        exit /b 1
    )
    echo   ✓ Maven found
)

:: ── 4. Node.js ────────────────────────────────────────────────────────────────
echo.
echo ── 4/7  Checking Node.js 18+ ──

set NODE_OK=0
node --version >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=1 delims=." %%a in ('node --version') do set "NODE_VER=%%a"
    set "NODE_VER=!NODE_VER:v=!"
    if !NODE_VER! geq 18 (
        set NODE_OK=1
        echo   ✓ Node.js found
    ) else (
        echo   ⚠ Node.js found but version is too old (need 18+^)
    )
)

if !NODE_OK! equ 0 (
    echo.
    echo   Node.js 18+ is required for the frontend.
    echo.
    echo   Install options:
    echo     Option A (recommended — nvm-windows):
    echo       https://github.com/coreybutler/nvm-windows/releases
    echo       Then: nvm install 20 ^&^& nvm use 20
    echo.
    echo     Option B (direct download):
    echo       https://nodejs.org/en/download/  (LTS version)
    echo.
    echo   Press any key once Node.js 18+ is installed, or close to abort...
    pause >nul
    node --version >nul 2>&1
    if %errorlevel% neq 0 (
        echo   Node.js is still not available.
        pause
        exit /b 1
    )
    echo   ✓ Node.js confirmed
)

:: ── 5. .env file ──────────────────────────────────────────────────────────────
echo.
echo ── 5/7  Preparing environment file ──

if not exist ".\.env" (
    copy .env.example .env >nul
    echo   ✓ .env created from .env.example
    echo   ⚠ Edit .env later to add real values for email, M-Pesa, OAuth
) else (
    echo   ✓ .env already exists — keeping current settings
)

:: ── 6. Docker services ────────────────────────────────────────────────────────
echo.
echo ── 6/7  Starting Docker services ──
echo.
echo   Ports used by default:
echo     PostgreSQL   5433    RabbitMQ      5672 / 15672
echo     Redis        6380    Mailhog       1025 / 8025
echo.

%DC% -f docker/docker-compose.yml up -d postgres redis mailhog rabbitmq
echo   ✓ Docker services started

echo   Waiting for RabbitMQ...
set /a _retries=0
:wait_rmq
%DC% exec -T rabbitmq rabbitmqctl await_startup_nodes 1 >nul 2>&1
if %errorlevel% neq 0 (
    if !\_retries! lss 30 (
        timeout /t 1 /nobreak >nul
        set /a _retries+=1
        goto wait_rmq
    )
)
echo   ✓ RabbitMQ ready

:: ── 7. Build & run ────────────────────────────────────────────────────────────
echo.
echo ── 7/7  Building and starting the application ──
echo.

echo   Building backend (this may take a few minutes on first run^)...
if exist ".\backend\mvnw.cmd" (
    call .\backend\mvnw.cmd -f backend\pom.xml clean package -DskipTests -q
) else (
    mvn -f backend\pom.xml clean package -DskipTests -q
)
if %errorlevel% neq 0 (
    echo   Backend build failed. Check the output above.
    pause
    exit /b 1
)
echo   ✓ Backend JAR built

echo.
echo   Starting backend server...
start "iLoveShopping-Backend" /min cmd /c "java -jar backend\target\iloveshopping-1.0.0.jar --spring.profiles.active=dev"
echo   ✓ Backend starting on http://localhost:8080

echo.
echo   Installing frontend dependencies...
cd frontend
call npm install --silent 2>nul
cd ..
echo   ✓ Frontend dependencies installed

echo.
echo   Starting frontend dev server...
cd frontend
start "iLoveShopping-Frontend" /min cmd /c "npx next dev"
cd ..
echo   ✓ Frontend starting on http://localhost:3000

:: ── Done ───────────────────────────────────────────────────────────────────────
echo.
echo ╔══════════════════════════════════════════════════════════════╗
echo ║                    Setup Complete!                          ║
echo ╠══════════════════════════════════════════════════════════════╣
echo ║                                                            ║
echo ║  Frontend:  http://localhost:3000                           ║
echo ║  Backend:   http://localhost:8080                           ║
echo ║  Swagger:   http://localhost:8080/api/v1/docs               ║
echo ║  Mailhog:   http://localhost:8025                           ║
echo ║  RabbitMQ:  http://localhost:15672 (guest/guest)            ║
echo ║                                                            ║
echo ║  Test accounts:                                            ║
echo ║    admin@iloveshopping.com / Admin123!                      ║
echo ║    user@iloveshopping.com  / User123!                       ║
echo ║                                                            ║
echo ║  To stop: run setup.bat stop                                ║
echo ║                                                            ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.

:: ── Handle "stop" argument ─────────────────────────────────────────────────────
if "%1"=="stop" (
    echo ── Stopping all services ──
    taskkill /FI "WINDOWTITLE eq iLoveShopping-Backend" /F >nul 2>&1
    echo   ✓ Backend stopped
    taskkill /FI "WINDOWTITLE eq iLoveShopping-Frontend" /F >nul 2>&1
    echo   ✓ Frontend stopped
    %DC% -f docker/docker-compose.yml stop
    echo   ✓ Docker services stopped
    echo.
)

echo Press any key to close...
pause >nul
endlocal
