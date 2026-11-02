@echo off
title SlopeGuard Launcher
setlocal EnableDelayedExpansion

echo ========================================================
echo       Starting SlopeGuard Disaster Management System
echo ========================================================
echo.

:: Detect root directory
set "BASE_DIR=%~dp0"
if exist "%BASE_DIR%backend" (
    set "BACKEND_DIR=%BASE_DIR%backend"
    set "FRONTEND_DIR=%BASE_DIR%frontend"
) else (
    set "BACKEND_DIR=%BASE_DIR%Land_Slide\backend"
    set "FRONTEND_DIR=%BASE_DIR%Land_Slide\frontend"
)

echo [1/3] Starting MySQL Database Server on port 3306...
start "SlopeGuard - MySQL Server (Port 3306)" "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe" --datadir="C:\Users\sakth\mysql_local_data" --port=3306 --console

echo Waiting for MySQL to initialize (3s)...
timeout /t 3 /nobreak >nul

echo [2/3] Starting Spring Boot REST Backend on port 8080...
start "SlopeGuard - Spring Boot Backend (Port 8080)" cmd /k "cd /d "%BACKEND_DIR%" && .\mvnw.cmd spring-boot:run"

echo Waiting for Spring Boot Backend to initialize (8s)...
timeout /t 8 /nobreak >nul

echo [3/3] Starting Frontend Web Server on port 3000...
start "SlopeGuard - Frontend Server (Port 3000)" cmd /k "cd /d "%FRONTEND_DIR%" && python -m http.server 3000"

timeout /t 2 /nobreak >nul

echo.
echo ========================================================
echo       SlopeGuard Services Successfully Launched!
echo ========================================================
echo.
echo Web Application: http://localhost:3000/login.html
echo REST Backend:    http://localhost:8080/actuator/health
echo MySQL Database:  localhost:3306 (slopeguard)
echo.
echo Opening browser...
start http://localhost:3000/login.html
echo.
echo You can keep these terminal windows open while working.
echo To stop all services, run STOP_SLOPEGUARD.bat.
pause
