@echo off
setlocal
title AI Info Station - Launcher

rem ============================================================
rem  AI Info Station one-click launcher (Windows)
rem  - Backend : Spring Boot (port 8080)
rem  - Frontend: Vite        (port 5173)
rem  Double-click to start; each runs in its own window.
rem
rem  NOTE: kept ASCII-only on purpose. A .bat saved as UTF-8 is
rem  misread by the Chinese (GBK) console and breaks. Messages
rem  are in English so it runs on any code page.
rem ============================================================

cd /d "%~dp0"

echo.
echo  ^>_ AI Info Station - starting...
echo  ------------------------------------------------------------
echo   Backend  http://localhost:8080
echo   Frontend http://localhost:5173
echo  ------------------------------------------------------------
echo.

rem ---- environment checks ----
where java >nul 2>nul || (echo [ERROR] java not found. Install JDK 21+ and add it to PATH. & pause & exit /b 1)
where mvn  >nul 2>nul || (echo [ERROR] mvn not found. Install Maven 3.9+ and add it to PATH. & pause & exit /b 1)
where node >nul 2>nul || (echo [ERROR] node not found. Install Node.js 18+ and add it to PATH. & pause & exit /b 1)

rem ---- frontend deps (auto-install on first run) ----
if not exist "frontend\node_modules" (
  echo [frontend] first run, installing deps via npm install ...
  pushd frontend
  call npm install || (echo [ERROR] npm install failed & popd & pause & exit /b 1)
  popd
)

rem ---- start backend in its own window ----
rem  start /D sets the working dir cleanly, avoiding nested-quote bugs.
echo [backend] launching Spring Boot ...
start "AI Station - Backend :8080" /D "%~dp0backend" cmd /k mvn spring-boot:run

rem ---- start frontend in its own window ----
echo [frontend] launching Vite dev server ...
start "AI Station - Frontend :5173" /D "%~dp0frontend" cmd /k npm run dev

echo.
echo  Both services are starting in separate windows.
echo  First backend start auto-creates tables + seed data (~20-40s).
echo  When ready, open: http://localhost:5173
echo  Default admin: admin / admin123
echo.
echo  Close a service window to stop it. You can close this window.
echo.
pause
endlocal
