@echo off
title Stop SlopeGuard
echo ========================================================
echo          Stopping SlopeGuard Services...
echo ========================================================
echo.

echo Stopping Frontend HTTP Server (port 3000)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":3000" ^| findstr "LISTENING"') do taskkill /F /PID %%a 2>nul

echo Stopping Spring Boot Backend (port 8080)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8080" ^| findstr "LISTENING"') do taskkill /F /PID %%a 2>nul

echo Stopping MySQL Server (port 3306)...
net stop MySQL80 2>nul
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":3306" ^| findstr "LISTENING"') do taskkill /F /PID %%a 2>nul

echo.
echo All SlopeGuard services have been stopped.
pause
