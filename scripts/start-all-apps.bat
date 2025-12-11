@echo off
REM ========================================
REM Start All GC Applications
REM ========================================
REM
REM Configuration - CHANGE THESE AS NEEDED
REM ========================================

REM JAR Configuration
SET JAR_NAME=java21-features-showcase-1.0-SNAPSHOT.jar
SET JAR_PATH=..\target\%JAR_NAME%

REM Port Configuration
SET G1GC_PORT=8080
SET GENZGC_PORT=8081
SET ZGC_PORT=8082

REM JVM Configuration (Heap Size)
REM Examples: 512m, 1g, 2g, 4g, 8g
SET HEAP_SIZE=512m

REM ========================================
REM Do not change below this line
REM ========================================

echo.
echo ========================================
echo Starting Garbage Collection Demo
echo ========================================
echo.
echo Configuration:
echo   JAR: %JAR_NAME%
echo   Heap Size: %HEAP_SIZE%
echo   G1GC Port: %G1GC_PORT%
echo   Gen ZGC Port: %GENZGC_PORT%
echo   ZGC Port: %ZGC_PORT%
echo.

REM Check if JAR exists
if not exist "%JAR_PATH%" (
    echo ERROR: JAR file not found!
    echo Looking for: %JAR_PATH%
    echo.
    echo Please run: mvn clean package
    echo.
    pause
    exit /b 1
)

echo Starting applications...
echo.

REM Start G1GC
echo [1/3] Starting G1GC on port %G1GC_PORT%...
echo Command: java -Xmx%HEAP_SIZE% -Xms%HEAP_SIZE% -XX:+UseG1GC --enable-preview -jar "%JAR_PATH%" --server.port=%G1GC_PORT%
start "G1GC (Port %G1GC_PORT%)" java -Xmx%HEAP_SIZE% -Xms%HEAP_SIZE% -XX:+UseG1GC --enable-preview -jar "%JAR_PATH%" --server.port=%G1GC_PORT%

timeout /t 5 /nobreak >nul

REM Start Generational ZGC
echo [2/3] Starting Generational ZGC on port %GENZGC_PORT%...
echo Command: java -Xmx%HEAP_SIZE% -Xms%HEAP_SIZE% -XX:+UseZGC -XX:+ZGenerational --enable-preview -jar "%JAR_PATH%" --server.port=%GENZGC_PORT%
start "Generational ZGC (Port %GENZGC_PORT%)" java -Xmx%HEAP_SIZE% -Xms%HEAP_SIZE% -XX:+UseZGC -XX:+ZGenerational --enable-preview -jar "%JAR_PATH%" --server.port=%GENZGC_PORT%

timeout /t 5 /nobreak >nul

REM Start ZGC
echo [3/3] Starting ZGC on port %ZGC_PORT%...
echo Command: java -Xmx%HEAP_SIZE% -Xms%HEAP_SIZE% -XX:+UseZGC -XX:-ZGenerational --enable-preview -jar "%JAR_PATH%" --server.port=%ZGC_PORT%
start "ZGC (Port %ZGC_PORT%)" java -Xmx%HEAP_SIZE% -Xms%HEAP_SIZE% -XX:+UseZGC -XX:-ZGenerational --enable-preview -jar "%JAR_PATH%" --server.port=%ZGC_PORT%

echo.
echo ========================================
echo All applications started!
echo ========================================
echo.
echo You should see 3 new windows:
echo   - G1GC (Port %G1GC_PORT%)
echo   - Generational ZGC (Port %GENZGC_PORT%)
echo   - ZGC (Port %ZGC_PORT%)
echo.
echo Wait ~30 seconds for all apps to fully start
echo.
echo To verify apps are running:
echo   curl http://localhost:%G1GC_PORT%/api/memory/health
echo   curl http://localhost:%GENZGC_PORT%/api/memory/health
echo   curl http://localhost:%ZGC_PORT%/api/memory/health
echo.
echo To stop apps: Close each window or run stop-all-apps.bat
echo.
echo ========================================

pause
