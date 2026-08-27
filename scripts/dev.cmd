@echo off
setlocal enabledelayedexpansion

:: ─── I Love Shopping — Development launcher (Windows) ────────────────────────
:: Usage:
::   start.cmd              Interactive menu (prompts for option 1 or 2)
::   start.cmd --auto       Fully automated — option 2, no prompts, installs everything
::   start.cmd --stop       Stop all services and clean up containers
:: ──────────────────────────────────────────────────────────────────────────────

set "REPO_DIR=%~dp0.."
set "COMPOSE_FILE=%REPO_DIR%\docker\docker-compose.yml"
set "COMPOSE=docker compose"

:: ── Parse flags ──────────────────────────────────────────────────────────────
set "AUTO=false"
set "STOP=false"
for %%a in (%*) do (
  if "%%a"=="--auto" set "AUTO=true"
  if "%%a"=="--stop" set "STOP=true"
)

:: ── Stop mode ────────────────────────────────────────────────────────────────
if "%STOP%"=="true" (
  echo [i-love-shopping] Stopping all services...
  %COMPOSE% -f "%COMPOSE_FILE%" down -v --remove-orphans 2>nul
  taskkill /FI "WINDOWTITLE eq iLoveShopping*" /F >nul 2>&1
  taskkill /IM java.exe /FI "WINDOWTITLE eq *iloveshopping*" /F >nul 2>&1
  echo [i-love-shopping] All services stopped.
  exit /b 0
)

echo.
echo ╔══════════════════════════════════════════════════════════╗
echo ║          I Love Shopping — Development Setup            ║
echo ║    https://github.com/imranshiundu/i-love-shopping1    ║
echo ╚══════════════════════════════════════════════════════════╝
echo.
echo [i-love-shopping] Repository: %REPO_DIR%
echo.

:: ── Docker ───────────────────────────────────────────────────────────────────
where docker >nul 2>&1
if errorlevel 1 (
  echo [i-love-shopping] Docker is not installed — attempting to install...
  where choco >nul 2>&1
  if errorlevel 1 (
    echo [i-love-shopping] Chocolatey is not installed. Installing it first...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))" 2>nul
    if errorlevel 1 (
      echo [i-love-shopping] Chocolatey install failed.
      echo [i-love-shopping] Install Docker Desktop manually: https://docs.docker.com/desktop/install/windows-install/
      echo [i-love-shopping] Then re-run this script.
      exit /b 1
    )
  )
  choco install docker-desktop -y
  if errorlevel 1 (
    echo [i-love-shopping] Docker Desktop install failed.
    echo [i-love-shopping] Download manually: https://docs.docker.com/desktop/install/windows-install/
    echo [i-love-shopping] Install, restart your PC, open Docker Desktop, then re-run this script.
    exit /b 1
  )
  echo [i-love-shopping] Docker Desktop installed. You may need to restart your PC.
  echo [i-love-spacing] If Docker doesn't work, restart and re-run this script.
)

:: Start daemon if not running
docker info >nul 2>&1
if errorlevel 1 (
  echo [i-love-shopping] Docker daemon is not running — starting Docker Desktop...
  start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe" 2>nul
  echo [i-love-shopping] Waiting for Docker daemon...
  set /a _dretries=0
  :wait_docker_start
  docker info >nul 2>&1
  if not errorlevel 1 goto docker_running
  set /a _dretries+=1
  if !_dretries! gtr 90 (
    echo [i-love-shopping] Docker daemon did not start within 90 seconds.
    echo [i-love-shopping] Open Docker Desktop manually, wait for the whale icon, then re-run.
    exit /b 1
  )
  timeout /t 1 /nobreak >nul
  goto wait_docker_start
  :docker_running
  echo [i-love-shopping] Docker daemon is running.
)

:: Docker Compose
docker compose version >nul 2>&1
if errorlevel 1 (
  where docker-compose >nul 2>&1
  if errorlevel 1 (
    echo [i-love-shopping] Docker Compose not found — installing...
    where choco >nul 2>&1
    if not errorlevel 1 choco install docker-compose -y
    docker compose version >nul 2>&1
    if errorlevel 1 (
      where docker-compose >nul 2>&1
      if errorlevel 1 (
        echo [i-love-shopping] Docker Compose install failed.
        echo [i-love-shopping] Install manually: https://docs.docker.com/compose/install/
        exit /b 1
      )
      set "COMPOSE=docker-compose"
    )
  ) else (
    set "COMPOSE=docker-compose"
  )
)
echo [i-love-shopping] Docker and Docker Compose ready.
echo.

:: ── Java ─────────────────────────────────────────────────────────────────────
where java >nul 2>&1
if errorlevel 1 (
  echo [i-love-shopping] Java 21+ not found — attempting to install...
  where choco >nul 2>&1
  if errorlevel 1 (
    echo [i-love-shopping] Chocolatey not available. Installing it first...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))" 2>nul
  )
  where choco >nul 2>&1
  if not errorlevel 1 (
    choco install temurin21 -y 2>nul
  )
  where java >nul 2>&1
  if errorlevel 1 (
    echo [i-love-shopping] Java 21+ could not be auto-installed.
    echo [i-love-shopping] Install manually: https://adoptium.net/temurin/releases/?version=21
    echo [i-love-shopping] Then re-run this script.
    exit /b 1
  )
)
echo [i-love-shopping] Java found.
echo.

:: ── Maven ────────────────────────────────────────────────────────────────────
set "MAVEN=mvn"
where mvn >nul 2>&1
if errorlevel 1 (
  if exist "%REPO_DIR%\backend\mvnw.cmd" (
    set "MAVEN=%REPO_DIR%\backend\mvnw.cmd"
    echo [i-love-shopping] Using Maven wrapper.
  ) else (
    echo [i-love-shopping] Maven not found — installing...
    where choco >nul 2>&1
    if not errorlevel 1 choco install maven -y
    where mvn >nul 2>&1
    if errorlevel 1 (
      echo [i-love-shopping] Maven could not be auto-installed.
      echo [i-love-shopping] Install manually: https://maven.apache.org/install.html
      exit /b 1
    )
    echo [i-love-shopping] Maven installed.
  )
) else (
  echo [i-love-shopping] Maven found.
)
echo.

:: ── Node.js ──────────────────────────────────────────────────────────────────
where node >nul 2>&1
if errorlevel 1 (
  echo [i-love-shopping] Node.js 18+ not found — attempting to install...
  where choco >nul 2>&1
  if errorlevel 1 (
    echo [i-love-shopping] Chocolatey not available. Installing it first...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))" 2>nul
  )
  where choco >nul 2>&1
  if not errorlevel 1 (
    choco install nodejs-lts -y 2>nul
  )
  where node >nul 2>&1
  if errorlevel 1 (
    echo [i-love-shopping] Node.js 18+ could not be auto-installed.
    echo [i-love-shopping] Install manually: https://nodejs.org
    exit /b 1
  )
)
for /f "tokens=1 delims=v." %%a in ('node --version 2^>nul') do set "NODE_MAJOR=%%a"
if !NODE_MAJOR! lss 18 (
  echo [i-love-shopping] Node.js version is too old (v!NODE_MAJOR!, need 18+^).
  echo [i-love-shopping] Update: https://nodejs.org
  exit /b 1
)
echo [i-love-shopping] Node.js v!NODE_MAJOR! found.
echo.

:: ── .env file ────────────────────────────────────────────────────────────────
if not exist "%REPO_DIR%\.env" (
  if exist "%REPO_DIR%\.env.example" (
    copy "%REPO_DIR%\.env.example" "%REPO_DIR%\.env" >nul
    echo [i-love-shopping] .env created from .env.example.
    echo [i-love-shopping] Edit it later to add real email, M-Pesa, and OAuth keys.
  ) else (
    echo [i-love-shopping] No .env.example found — using environment defaults.
  )
) else (
  echo [i-love-shopping] .env already exists — keeping current settings.
)
echo.

:: ── Choose mode ──────────────────────────────────────────────────────────────
echo [i-love-shopping] Docker, Docker Compose, Java, Maven and Node.js are ready.
echo.
echo [i-love-shopping] How do you want to run the project?
echo   1) Everything in Docker (API + Frontend included) — only Docker needed
echo   2) Dependencies in Docker, API ^& Frontend run locally — needs Java 21, Maven, Node.js
set /p choice=Choose [1/2]:

if "%choice%"=="1" goto all_docker
if "%choice%"=="2" goto local_backend
echo [i-love-shopping] Invalid choice. Run again and pick 1 or 2.
exit /b 1

:: ── Port resolution ──────────────────────────────────────────────────────────
:resolve_ports
call :findfree "%POSTGRES_PORT%" PGPORT
set "POSTGRES_PORT=!PGPORT!"
call :findfree "%REDIS_PORT%" RDPORT
set "REDIS_PORT=!RDPORT!"
call :findfree "%MAILHOG_SMTP_PORT%" MHSMTP
set "MAILHOG_SMTP_PORT=!MHSMTP!"
call :findfree "%MAILHOG_UI_PORT%" MHUI
set "MAILHOG_UI_PORT=!MHUI!"
call :findfree "%RABBITMQ_AMQP_PORT%" RMQ
set "RABBITMQ_AMQP_PORT=!RMQ!"
call :findfree "%RABBITMQ_UI_PORT%" RMQUI
set "RABBITMQ_UI_PORT=!RMQUI!"
call :findfree "%API_PORT%" APID
set "API_PORT=!APID!"
call :findfree "%FRONTEND_PORT%" FEPORT
set "FRONTEND_PORT=!FEPORT!"
goto :eof

:findfree
set /a fp=%~1
:ff_loop
netstat -an | findstr /r /c:":%fp% .*LISTENING" >nul 2>&1
if not errorlevel 1 (
  set /a fp+=1
  goto ff_loop
)
set "%~2=%fp%"
goto :eof

:: ── Option 1: everything in Docker ───────────────────────────────────────────
:all_docker
set "POSTGRES_PORT=5433"
set "REDIS_PORT=6380"
set "MAILHOG_SMTP_PORT=1025"
set "MAILHOG_UI_PORT=8025"
set "RABBITMQ_AMQP_PORT=5672"
set "RABBITMQ_UI_PORT=15672"
set "API_PORT=8080"
set "FRONTEND_PORT=3000"
call :resolve_ports
echo [i-love-shopping] Starting everything in Docker (foreground)...
echo [i-love-shopping] Frontend: http://localhost:!FRONTEND_PORT!
echo [i-love-shopping] API: http://localhost:!API_PORT!/api/v1   Swagger UI: http://localhost:!API_PORT!/api/v1/docs
echo [i-love-shopping] Mailhog UI: http://localhost:!MAILHOG_UI_PORT!   RabbitMQ UI: http://localhost:!RABBITMQ_UI_PORT! (iloveshopping/iloveshopping)
echo [i-love-shopping] Press Ctrl+C to stop all services.
cd /d "%REPO_DIR%"
%COMPOSE% -f "%COMPOSE_FILE%" up
exit /b 0

:: ── Option 2: deps in Docker, API + frontend local ───────────────────────────
:local_backend
set "POSTGRES_PORT=5433"
set "REDIS_PORT=6380"
set "MAILHOG_SMTP_PORT=1025"
set "MAILHOG_UI_PORT=8025"
set "RABBITMQ_AMQP_PORT=5672"
set "RABBITMQ_UI_PORT=15672"
set "API_PORT=8080"
set "FRONTEND_PORT=3000"
call :resolve_ports

echo [i-love-shopping] Starting PostgreSQL, Redis, Mailhog and RabbitMQ...
cd /d "%REPO_DIR%"
%COMPOSE% -f "%COMPOSE_FILE%" up -d postgres redis mailhog rabbitmq

set /a cnt=0
:wait_pg
%COMPOSE% -f "%COMPOSE_FILE%" ps postgres | findstr /C:"healthy" >nul 2>&1
if not errorlevel 1 goto pg_ok
set /a cnt+=1
if !cnt! gtr 40 (
  echo [i-love-shopping] Timed out waiting for PostgreSQL.
  exit /b 1
)
timeout /t 3 /nobreak >nul
goto wait_pg
:pg_ok

set /a cnt=0
:wait_redis
%COMPOSE% -f "%COMPOSE_FILE%" ps redis | findstr /C:"healthy" >nul 2>&1
if not errorlevel 1 goto redis_ok
set /a cnt+=1
if !cnt! gtr 40 (
  echo [i-love-shopping] Timed out waiting for Redis.
  exit /b 1
)
timeout /t 3 /nobreak >nul
goto wait_redis
:redis_ok

set /a cnt=0
:wait_rabbit
%COMPOSE% -f "%COMPOSE_FILE%" ps rabbitmq | findstr /C:"healthy" >nul 2>&1
if not errorlevel 1 goto rabbit_ok
set /a cnt+=1
if !cnt! gtr 40 (
  echo [i-love-shopping] Timed out waiting for RabbitMQ.
  exit /b 1
)
timeout /t 3 /nobreak >nul
goto wait_rabbit
:rabbit_ok

echo [i-love-shopping] All dependencies are up.

:: Set env vars for backend and frontend
set "DATABASE_URL=jdbc:postgresql://localhost:!POSTGRES_PORT!/iloveshopping?stringtype=unspecified"
set "DATABASE_USER=iloveshopping"
set "DATABASE_PASSWORD=iloveshopping"
set "REDIS_HOST=localhost"
set "RECAPTCHA_SECRET_KEY=dev-test-secret"
set "RECAPTCHA_SITE_KEY=dev-test-site"
set "JWT_ACCESS_SECRET=dev-access-secret-min-32-chars-long-for-test"
set "JWT_REFRESH_SECRET=dev-refresh-secret-min-32-chars-long-for-test"
set "MAIL_HOST=localhost"
set "MAIL_PORT=!MAILHOG_SMTP_PORT!"
set "MAIL_SMTP_AUTH=false"
set "MAIL_SMTP_STARTTLS=false"
set "SERVER_PORT=!API_PORT!"
set "FRONTEND_URL=http://localhost:!FRONTEND_PORT!"
set "CORS_ALLOWED_ORIGINS=http://localhost:!FRONTEND_PORT!"
set "RABBITMQ_HOST=localhost"
set "RABBITMQ_PORT=!RABBITMQ_AMQP_PORT!"
set "RABBITMQ_USERNAME=iloveshopping"
set "RABBITMQ_PASSWORD=iloveshopping"
set "NEXT_PUBLIC_API_URL=http://localhost:!API_PORT!/api/v1"
set "BACKEND_INTERNAL_URL=http://localhost:!API_PORT!"
set "PORT=!FRONTEND_PORT!"

echo.
echo [i-love-shopping] Frontend:            http://localhost:!FRONTEND_PORT!
echo [i-love-shopping] API:                 http://localhost:!API_PORT!/api/v1
echo [i-love-shopping] Swagger UI:          http://localhost:!API_PORT!/api/v1/docs
echo [i-love-shopping] Mailhog Web UI:      http://localhost:!MAILHOG_UI_PORT!
echo [i-love-shopping] RabbitMQ Management: http://localhost:!RABBITMQ_UI_PORT! (iloveshopping/iloveshopping)
echo [i-love-shopping] Seeded accounts: admin@iloveshopping.com / Admin123!   user@iloveshopping.com / User123!
echo [i-love-shopping] Press Ctrl+C to stop the API, the frontend and the containers.
echo.

start "iLoveShopping-Frontend" cmd /c "cd /d %REPO_DIR%\frontend && npm install && npm run dev"
cd /d "%REPO_DIR%\backend"
call "%MAVEN%" spring-boot:run

echo [i-love-shopping] Shutting down...
taskkill /FI "WINDOWTITLE eq iLoveShopping-Frontend" /F >nul 2>&1
cd /d "%REPO_DIR%"
%COMPOSE% -f "%COMPOSE_FILE%" rm -sf postgres redis mailhog rabbitmq
exit /b 0
