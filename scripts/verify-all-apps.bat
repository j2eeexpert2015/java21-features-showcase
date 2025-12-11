@echo off
REM ========================================
REM Verify All GC Applications
REM ========================================

echo.
echo ========================================
echo Verifying All GC Applications
echo ========================================
echo.

REM Check G1GC
echo Checking G1GC (port 8080)...
curl -s http://localhost:8080/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo   [OK] G1GC is running
) else (
    echo   [ERROR] G1GC is NOT running
)

REM Check Generational ZGC
echo Checking Generational ZGC (port 8081)...
curl -s http://localhost:8081/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo   [OK] Generational ZGC is running
) else (
    echo   [ERROR] Generational ZGC is NOT running
)

REM Check ZGC
echo Checking ZGC (port 8082)...
curl -s http://localhost:8082/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo   [OK] ZGC is running
) else (
    echo   [ERROR] ZGC is NOT running
)

echo.
echo ========================================
echo Verification Complete
echo ========================================
echo.

pause
