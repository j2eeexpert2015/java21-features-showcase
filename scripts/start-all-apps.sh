#!/bin/bash

########################################
# Start All 3 GC Applications Together
########################################
#
# Configuration - CHANGE THESE AS NEEDED
########################################

# JAR Configuration
JAR_NAME="java21-features-showcase-1.0-SNAPSHOT.jar"
JAR_PATH="../target/${JAR_NAME}"

# Port Configuration
G1GC_PORT=8080
GENZGC_PORT=8081
ZGC_PORT=8082

# JVM Configuration (Heap Size)
# Examples: 512m, 1g, 2g, 4g, 8g
HEAP_SIZE="2g"

########################################
# Do not change below this line
########################################

echo ""
echo "========================================"
echo "Starting Garbage Collection Demo"
echo "========================================"
echo ""
echo "Configuration:"
echo "  JAR: ${JAR_NAME}"
echo "  Heap Size: ${HEAP_SIZE}"
echo "  G1GC Port: ${G1GC_PORT}"
echo "  Gen ZGC Port: ${GENZGC_PORT}"
echo "  ZGC Port: ${ZGC_PORT}"
echo ""

# Check if JAR file exists
if [ ! -f "${JAR_PATH}" ]; then
    echo "ERROR: JAR file not found!"
    echo "Looking for: ${JAR_PATH}"
    echo ""
    echo "Please run: mvn clean package"
    echo ""
    exit 1
fi

# Detect OS and terminal
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    TERMINAL_CMD="osascript"
elif command -v gnome-terminal &> /dev/null; then
    # Linux with GNOME Terminal
    TERMINAL_CMD="gnome-terminal"
elif command -v konsole &> /dev/null; then
    # Linux with KDE Konsole
    TERMINAL_CMD="konsole"
elif command -v xterm &> /dev/null; then
    # Linux with xterm
    TERMINAL_CMD="xterm"
else
    echo "WARNING: No suitable terminal found. Using background mode."
    TERMINAL_CMD="background"
fi

echo "Starting applications in separate terminals..."
echo ""

# Start G1GC on port ${G1GC_PORT}
echo "[1/3] Starting G1GC on port ${G1GC_PORT}..."
echo "Command: java -Xmx${HEAP_SIZE} -Xms${HEAP_SIZE} -XX:+UseG1GC --enable-preview -jar ${JAR_PATH} --server.port=${G1GC_PORT}"
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    osascript -e "tell app \"Terminal\" to do script \"cd '$(pwd)' && java -Xmx${HEAP_SIZE} -Xms${HEAP_SIZE} -XX:+UseG1GC --enable-preview -jar ${JAR_PATH} --server.port=${G1GC_PORT}\""
elif [ "$TERMINAL_CMD" = "gnome-terminal" ]; then
    # GNOME Terminal
    gnome-terminal --title="G1GC (Port ${G1GC_PORT})" -- bash -c "java -Xmx${HEAP_SIZE} -Xms${HEAP_SIZE} -XX:+UseG1GC --enable-preview -jar ${JAR_PATH} --server.port=${G1GC_PORT}; exec bash"
elif [ "$TERMINAL_CMD" = "konsole" ]; then
    # KDE Konsole
    konsole --title "G1GC (Port ${G1GC_PORT})" -e bash -c "java -Xmx${HEAP_SIZE} -Xms${HEAP_SIZE} -XX:+UseG1GC --enable-preview -jar ${JAR_PATH} --server.port=${G1GC_PORT}; exec bash" &
elif [ "$TERMINAL_CMD" = "xterm" ]; then
    # xterm
    xterm -title "G1GC (Port ${G1GC_PORT})" -e "java -Xmx${HEAP_SIZE} -Xms${HEAP_SIZE} -XX:+UseG1GC --enable-preview -jar ${JAR_PATH} --server.port=${G1GC_PORT}" &
else
    # Background mode
    nohup java -Xmx${HEAP_SIZE} -Xms${HEAP_SIZE} -XX:+UseG1GC --enable-preview -jar ${JAR_PATH} --server.port=${G1GC_PORT} > /dev/null 2>&1 &
fi

# Wait 5 seconds
sleep 5

# Start Generational ZGC on port ${GENZGC_PORT}
echo "[2/3] Starting Generational ZGC on port ${GENZGC_PORT}..."
echo "Command: java -Xmx${HEAP_SIZE} -Xms${HEAP_SIZE} -XX:+UseZGC -XX:+ZGenerational --enable-preview -jar ${JAR_PATH} --server.port=${GENZGC_PORT}"
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    osascript -e "tell app \"Terminal\" to do script \"cd '$(pwd)' && java -Xmx${HEAP_SIZE} -Xms${HEAP_SIZE} -XX:+UseZGC -XX:+ZGenerational --enable-preview -jar ${JAR_PATH} --server.port=${GENZGC_PORT}\""
elif [ "$TERMINAL_CMD" = "gnome-terminal" ]; then
    # GNOME Terminal
    gnome-terminal --title="Generational ZGC (Port ${GENZGC_PORT})" -- bash -c "java -Xmx${HEAP_SIZE} -Xms${HEAP_SIZE} -XX:+UseZGC -XX:+ZGenerational --enable-preview -jar ${JAR_PATH} --server.port=${GENZGC_PORT}; exec bash"
elif [ "$TERMINAL_CMD" = "konsole" ]; then
    # KDE Konsole
    konsole --title "Generational ZGC (Port ${GENZGC_PORT})" -e bash -c "java -Xmx${HEAP_SIZE} -Xms${HEAP_SIZE} -XX:+UseZGC -XX:+ZGenerational --enable-preview -jar ${JAR_PATH} --server.port=${GENZGC_PORT}; exec bash" &
elif [ "$TERMINAL_CMD" = "xterm" ]; then
    # xterm
    xterm -title "Generational ZGC (Port ${GENZGC_PORT})" -e "java -Xmx${HEAP_SIZE} -Xms${HEAP_SIZE} -XX:+UseZGC -XX:+ZGenerational --enable-preview -jar ${JAR_PATH} --server.port=${GENZGC_PORT}" &
else
    # Background mode
    nohup java -Xmx${HEAP_SIZE} -Xms${HEAP_SIZE} -XX:+UseZGC -XX:+ZGenerational --enable-preview -jar ${JAR_PATH} --server.port=${GENZGC_PORT} > /dev/null 2>&1 &
fi

# Wait 5 seconds
sleep 5

# Start ZGC on port ${ZGC_PORT}
echo "[3/3] Starting ZGC on port ${ZGC_PORT}..."
echo "Command: java -Xmx${HEAP_SIZE} -Xms${HEAP_SIZE} -XX:+UseZGC -XX:-ZGenerational --enable-preview -jar ${JAR_PATH} --server.port=${ZGC_PORT}"
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    osascript -e "tell app \"Terminal\" to do script \"cd '$(pwd)' && java -Xmx${HEAP_SIZE} -Xms${HEAP_SIZE} -XX:+UseZGC -XX:-ZGenerational --enable-preview -jar ${JAR_PATH} --server.port=${ZGC_PORT}\""
elif [ "$TERMINAL_CMD" = "gnome-terminal" ]; then
    # GNOME Terminal
    gnome-terminal --title="ZGC (Port ${ZGC_PORT})" -- bash -c "java -Xmx${HEAP_SIZE} -Xms${HEAP_SIZE} -XX:+UseZGC -XX:-ZGenerational --enable-preview -jar ${JAR_PATH} --server.port=${ZGC_PORT}; exec bash"
elif [ "$TERMINAL_CMD" = "konsole" ]; then
    # KDE Konsole
    konsole --title "ZGC (Port ${ZGC_PORT})" -e bash -c "java -Xmx${HEAP_SIZE} -Xms${HEAP_SIZE} -XX:+UseZGC -XX:-ZGenerational --enable-preview -jar ${JAR_PATH} --server.port=${ZGC_PORT}; exec bash" &
elif [ "$TERMINAL_CMD" = "xterm" ]; then
    # xterm
    xterm -title "ZGC (Port ${ZGC_PORT})" -e "java -Xmx${HEAP_SIZE} -Xms${HEAP_SIZE} -XX:+UseZGC -XX:-ZGenerational --enable-preview -jar ${JAR_PATH} --server.port=${ZGC_PORT}" &
else
    # Background mode
    nohup java -Xmx${HEAP_SIZE} -Xms${HEAP_SIZE} -XX:+UseZGC -XX:-ZGenerational --enable-preview -jar ${JAR_PATH} --server.port=${ZGC_PORT} > /dev/null 2>&1 &
fi

echo ""
echo "========================================"
echo "All applications started!"
echo "========================================"
echo ""

if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "You should see 3 new Terminal windows:"
elif [ "$TERMINAL_CMD" = "background" ]; then
    echo "Applications running in background"
else
    echo "You should see 3 new terminal windows:"
fi

echo "  - G1GC (Port ${G1GC_PORT})"
echo "  - Generational ZGC (Port ${GENZGC_PORT})"
echo "  - ZGC (Port ${ZGC_PORT})"
echo ""
echo "Wait ~30 seconds for all apps to fully start"
echo ""
echo "To verify apps are running:"
echo "  curl http://localhost:${G1GC_PORT}/api/memory/health"
echo "  curl http://localhost:${GENZGC_PORT}/api/memory/health"
echo "  curl http://localhost:${ZGC_PORT}/api/memory/health"
echo ""
echo "To stop apps: Close each window or run stop-all-apps.sh"
echo ""
echo "========================================"
