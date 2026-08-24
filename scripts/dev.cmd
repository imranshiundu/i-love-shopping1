@echo off
setlocal enabledelayedexpansion

set "REPO_DIR=%~dp0.."
set "COMPOSE_FILE=%REPO_DIR%\docker\docker-compose.yml"
set "COMPOSE=docker compose"

echo [i-love-shopping] Development environment
echo [i-love-shopping] Repository: %REPO_DIR%
echo.

where docker >nul 2>&1
if errorlevel 1 (
  echo [i-love-shopping] Docker is not installed. Install Docker Desktop from https://docs.docker.com/get-docker/ and run this script again.
  exit /b 1
)
docker info >nul 2>&1
if errorlevel 1 (
  echo [i-love-shopping] The Docker daemon is not running. Start Docker Desktop and run this script again.
  exit /b 1
)
docker compose version >nul 2>&1
if errorlevel 1 (
  where docker-compose >nul 2>&1
  if errorlevel 1 (
    echo [i-love-shopping] Docker Compose is not installed. Install it and run this script again.
    exit /b 1
  )
  set "COMPOSE=docker-compose"
)
where node >nul 2>&1
if errorlevel 1 (
  echo [i-love-shopping] Node.js is not installed. Install Node.js 20+ from https://nodejs.org and run this script again.
  exit /b 1
)
echo [i-love-shopping] Docker, Docker Compose and Node.js are ready.
echo.
echo [i-love-shopping] How do you want to run the project?
echo   1) Everything in Docker (PostgreSQL + Redis + Mailhog + RabbitMQ + API + Frontend) - only Docker
echo   2) Dependencies in Docker + run API ^& Frontend locally - requires Java 21 and Maven too
set /p choice=Choose an option [1/2]: 

if "%choice%"=="1" goto all_docker
if "%choice%"=="2" goto local_backend
echo [i-love-shopping] Invalid choice. Run the script again and pick 1 or 2.
exit /b 1

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
echo [i-love-shopping] Starting PostgreSQL, Redis, Mailhog, RabbitMQ, API and Frontend in the foreground.
echo [i-love-shopping] Frontend: http://localhost:!FRONTEND_PORT!
echo [i-love-shopping] API: http://localhost:!API_PORT!/api/v1   Swagger UI: http://localhost:!API_PORT!/api/v1/docs
echo [i-love-shopping] Mailhog UI: http://localhost:!MAILHOG_UI_PORT!   RabbitMQ UI: http://localhost:!RABBITMQ_UI_PORT! (iloveshopping/iloveshopping)
echo [i-love-shopping] Press Ctrl+C to stop all services.
cd /d "%REPO_DIR%"
%COMPOSE% -f "%COMPOSE_FILE%" up
exit /b 0

:local_backend
where java >nul 2>&1
if errorlevel 1 (
  echo [i-love-shopping] Java 21 is not installed. Install JDK 21 and run this script again.
  exit /b 1
)
set "MAVEN=mvn"
where mvn >nul 2>&1
if errorlevel 1 (
  if exist "%REPO_DIR%\backend\mvnw.cmd" (
    set "MAVEN=%REPO_DIR%\backend\mvnw.cmd"
  ) else (
    echo [i-love-shopping] Maven is not installed. Install Maven and run this script again.
    exit /b 1
  )
)
set "POSTGRES_PORT=5433"
set "REDIS_PORT=6380"
set "MAILHOG_SMTP_PORT=1025"
set "MAILHOG_UI_PORT=8025"
set "RABBITMQ_AMQP_PORT=5672"
set "RABBITMQ_UI_PORT=15672"
set "API_PORT=8080"
set "FRONTEND_PORT=3000"
call :resolve_ports

echo [i-love-shopping] Starting PostgreSQL, Redis, Mailhog and RabbitMQ with Docker Compose...
cd /d "%REPO_DIR%"
%COMPOSE% -f "%COMPOSE_FILE%" up -d postgres redis mailhog rabbitmq

set /a cnt=0
:wait_pg
%COMPOSE% -f "%COMPOSE_FILE%" ps postgres | findstr /C:"healthy" >nul 2>&1
if not errorlevel 1 goto pg_ok
set /a cnt+=1
if %cnt% gtr 40 (
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
if %cnt% gtr 40 (
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
if %cnt% gtr 40 (
  echo [i-love-shopping] Timed out waiting for RabbitMQ.
  exit /b 1
)
timeout /t 3 /nobreak >nul
goto wait_rabbit
:rabbit_ok

echo [i-love-shopping] Dependencies are up.
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

start "i-love-shopping frontend" cmd /c "cd /d %REPO_DIR%\frontend && npm install && npm run dev"
cd /d "%REPO_DIR%\backend"
call "%MAVEN%" spring-boot:run
echo [i-love-shopping] Stopping development containers...
cd /d "%REPO_DIR%"
%COMPOSE% -f "%COMPOSE_FILE%" rm -sf postgres redis mailhog rabbitmq
exit /b 0
