package org.example.service;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

/**
 * Listens to GC notifications and prints detailed event information.
 *
 * Automatically registers with all GC beans on startup and logs:
 * - Registration events
 * - GC pause events (Stop-The-World)
 * - GC cycle events (Concurrent work)
 */
@Service
public class GcNotificationListener implements NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(GcNotificationListener.class);

    /**
     * Automatically register this listener with all GC beans on startup
     */
    @PostConstruct
    public void registerListener() {
        log.info("========================================");
        log.info("GC Notification Listener Starting...");
        log.info("========================================");

        int registeredCount = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gcBean instanceof NotificationEmitter) {
                ((NotificationEmitter) gcBean).addNotificationListener(this, null, null);
                registeredCount++;
                log.info("✓ Registered listener for GC: {}", gcBean.getName());
                log.info("  - Memory Pools: {}", String.join(", ", gcBean.getMemoryPoolNames()));
            }
        }

        log.info("========================================");
        log.info("Registered for {} GC bean(s)", registeredCount);
        log.info("Ready to capture GC events...");
        log.info("========================================");
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
        // Only process GC notifications
        if (!notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
            return;
        }

        GarbageCollectionNotificationInfo info =
                GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());

        GcInfo gcInfo = info.getGcInfo();
        long durationMs = gcInfo.getDuration();
        long startTime = gcInfo.getStartTime();
        long endTime = gcInfo.getEndTime();
        long id = gcInfo.getId();

        String gcName = info.getGcName();
        String gcCause = info.getGcCause();
        String gcAction = info.getGcAction();

        // Classify the event
        boolean isPause = isPauseEvent(gcName, gcAction);
        boolean isCycle = isCycleEvent(gcName, gcAction);
        String eventCategory = isPause ? "PAUSE" : (isCycle ? "CYCLE" : "UNKNOWN");

        // Print detailed GC event information
        log.info("┌─────────────────────────────────────────────────────────");
        log.info("│ GC EVENT #{}", id);
        log.info("├─────────────────────────────────────────────────────────");
        log.info("│ Category:  {}", eventCategory);
        log.info("│ Type:      {}", gcName);
        log.info("│ Action:    {}", gcAction);
        log.info("│ Cause:     {}", gcCause);
        log.info("│ Duration:  {} ms", durationMs);
        log.info("│ Start:     {} ms", startTime);
        log.info("│ End:       {} ms", endTime);
        log.info("└─────────────────────────────────────────────────────────");
    }

    /**
     * Determines if a GC event is a pause (Stop-The-World).
     */
    private boolean isPauseEvent(String gcName, String gcAction) {
        // G1GC pause events
        if (gcName.startsWith("G1 Young") ||
                gcName.startsWith("G1 Old") ||
                gcName.startsWith("G1 Mixed")) {
            return true;
        }

        // ZGC pause events
        if (gcName.contains("Pauses") || gcAction.equals("end of GC pause")) {
            return true;
        }

        return false;
    }

    /**
     * Determines if a GC event is a cycle (concurrent work).
     */
    private boolean isCycleEvent(String gcName, String gcAction) {
        // G1GC concurrent work
        if (gcName.equals("G1 Concurrent GC")) {
            return true;
        }

        // ZGC cycle events
        if (gcName.contains("Cycles") || gcAction.equals("end of GC cycle")) {
            return true;
        }

        return false;
    }
}