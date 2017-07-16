package org.neo4j.graphalgo.core.utils;

import org.neo4j.logging.Log;

import java.util.function.DoubleSupplier;

/**
 * @author mknblch
 */
public class ProgressLoggerAdapter implements ProgressLogger {

    private final Log log;

    private static int logInterval = 10_000; // 10s log interval by default

    private volatile long lastLog = 0;

    public ProgressLoggerAdapter(Log log) {
        this.log = log;
    }

    @Override
    public void logProgress(double percentDone) {
        final long currentTime = System.currentTimeMillis();
        if (currentTime > lastLog + logInterval) {
            log.info("[%s] %d%% done", Thread.currentThread().getName(), percentDone * 100);
            lastLog = currentTime;
        }
    }
}
