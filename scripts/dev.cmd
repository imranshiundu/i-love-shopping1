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
echo [i-love-shopping] Docker and Docker Compose are ready.
echo.
echo [i-love-shopping] How do you want to run the project?
echo   1) Everything in Docker (PostgreSQL + Redis + Mailhog + API)
echo   2) Dependencies in Docker + run the Spring Boot API locally
set /p choice=Choose an option [1/2]: 

if "%choice%"=="1" goto all_docker
if "%choice%"=="2" goto local_backend
echo [i-love-shopping] Invalid choice. Run the script again and pick 1 or 2.
exit /b 1

:all_docker
echo [i-love-shopping] Starting PostgreSQL, Redis, Mailhog and the API in the foreground.
echo [i-love-shopping] API: http://localhost:8080/api/v1   Swagger UI: http://localhost:8080/api/v1/docs
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
echo [i-love-shopping] Starting PostgreSQL, Redis and Mailhog with Docker Compose...
cd /d "%REPO_DIR%"
%COMPOSE% -f "%COMPOSE_FILE%" up -d postgres redis mailhog

set /a cnt=0
:wait_pg
%COMPOSE% -f "%COMPOSE_FILE%" ps postgres | findstr /C:"healthy" >nul 2>&1
if not errorlevel 1 goto pg_ok
set /a cnt+=1
if %cnt% gtr 30 (
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
if %cnt% gtr 30 (
  echo [i-love-shopping] Timed out waiting for Redis.
  exit /b 1
)
timeout /t 3 /nobreak >nul
goto wait_redis
:redis_ok

echo [i-love-shopping] Dependencies are up.
set "DATABASE_URL=jdbc:postgresql://localhost:5433/iloveshopping?stringtype=unspecified"
set "DATABASE_USER=iloveshopping"
set "DATABASE_PASSWORD=iloveshopping"
set "REDIS_HOST=localhost"
set "REDIS_PORT=6380"
set "RECAPTCHA_SECRET_KEY=dev-test-secret"
set "RECAPTCHA_SITE_KEY=dev-test-site"
set "JWT_ACCESS_SECRET=dev-access-secret-min-32-chars-long-for-test"
set "JWT_REFRESH_SECRET=dev-refresh-secret-min-32-chars-long-for-test"
set "MAIL_HOST=localhost"
set "MAIL_PORT=1025"
set "FRONTEND_URL=http://localhost:3000"
set "CORS_ALLOWED_ORIGINS=http://localhost:3000"
echo [i-love-shopping] Starting the Spring Boot API in the foreground. Press Ctrl+C to stop.
echo [i-love-shopping] API: http://localhost:8080/api/v1   Swagger UI: http://localhost:8080/api/v1/docs   Mailhog UI: http://localhost:8025
cd /d "%REPO_DIR%\backend"
call "%MAVEN%" spring-boot:run
echo [i-love-shopping] Stopping development containers...
cd /d "%REPO_DIR%"
%COMPOSE% -f "%COMPOSE_FILE%" down
exit /b 0