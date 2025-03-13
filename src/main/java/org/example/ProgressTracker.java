package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ProgressTracker implements AutoCloseable {
    private static final int LOG_BUFFER_SIZE = 8192;
    private static final String LOG_FILE = "application.log";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DecimalFormat PROGRESS_FORMAT = new DecimalFormat("0.00");
    
    private final ReentrantLock lock = new ReentrantLock();
    private long startTime;
    private final AtomicInteger totalSteps = new AtomicInteger(0);
    private final AtomicInteger completedSteps = new AtomicInteger(0);
    private volatile boolean isTracking = false;
    private final StringBuilder logBuffer = new StringBuilder(LOG_BUFFER_SIZE);
    private int bufferCount = 0;

    /**
     * Starts tracking the progress of a task.
     *
     * @param totalSteps The total number of steps in the process.
     */
    public synchronized void startTracking(int totalSteps) {
        if (isTracking) {
            logError("Tracking already in progress!");
            return;
        }

        if (totalSteps <= 0) {
            logError("Total steps must be greater than zero!");
            return;
        }

        this.startTime = System.currentTimeMillis(); // Record the start time
        this.totalSteps.set(totalSteps);
        this.completedSteps.set(0);
        this.isTracking = true;
        logMessage("Process started with " + totalSteps + " steps.");
    }

    /**
     * Updates the progress for the given step.
     *
     * @param description A description of the task being completed.
     */
    public synchronized void updateProgress(String description) {
        if (!isTracking) {
            logError("Cannot update progress. Tracking has not been started.");
            return;
        }

        int currentStep = completedSteps.incrementAndGet();
        if (currentStep > totalSteps.get()) {
            logError("Current step exceeds total steps.");
            return;
        }

        double percentage = ((double) currentStep / totalSteps.get()) * 100;
        logMessage(String.format("Step %d/%d Completed (%s%%) - %s", 
            currentStep, 
            totalSteps.get(), 
            PROGRESS_FORMAT.format(percentage),
            description));
    }

    /**
     * Completes the tracking process and logs the total time taken.
     */
    public synchronized void completeTracking() {
        if (!isTracking) {
            logError("Cannot complete tracking. Tracking has not been started.");
            return;
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        logMessage("Process completed in " + formatDuration(duration) + ".");
        isTracking = false;
    }

    /**
     * Dynamically adjusts the total number of steps during execution.
     *
     * @param newTotalSteps The new total number of steps.
     */
    public synchronized void adjustTotalSteps(int newTotalSteps) {
        if (!isTracking) {
            logError("Cannot adjust steps. Tracking has not been started.");
            return;
        }

        if (newTotalSteps < completedSteps.get()) {
            logError("New total steps cannot be less than completed steps.");
            return;
        }

        this.totalSteps.set(newTotalSteps);
        logMessage("Total steps updated to: " + newTotalSteps);
    }

    /**
     * Formats the duration into a human-readable format (HH:MM:SS).
     *
     * @param duration The duration in milliseconds.
     * @return A formatted string representing the duration.
     */
    private String formatDuration(long duration) {
        long seconds = (duration / 1000) % 60;
        long minutes = (duration / (1000 * 60)) % 60;
        long hours = (duration / (1000 * 60 * 60)) % 24;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Provides the percentage of progress completed.
     *
     * @return The progress percentage as a formatted string.
     */
    public synchronized String getProgressPercentage() {
        if (!isTracking) {
            return "0.00%";
        }

        double percentage = ((double) completedSteps.get() / totalSteps.get()) * 100;
        return PROGRESS_FORMAT.format(percentage) + "%";
    }

    /**
     * Checks if the process is complete.
     *
     * @return True if all steps are completed, false otherwise.
     */
    public synchronized boolean isComplete() {
        return completedSteps.get() >= totalSteps.get() && isTracking;
    }

    /**
     * Resets the tracker to allow for a new process.
     */
    public synchronized void resetTracker() {
        this.startTime = 0;
        this.totalSteps.set(0);
        this.completedSteps.set(0);
        this.isTracking = false;
        logMessage("Tracker has been reset.");
    }

    /**
     * Logs a custom informational message.
     *
     * @param message The message to log.
     */
    public void logMessage(String message) {
        lock.lock();
        try {
            String formattedMessage = String.format("[%s][INFO] %s%n", 
                LocalDateTime.now().format(DATE_FORMAT), 
                message);
            
            System.out.print(formattedMessage);
            
            logBuffer.append(formattedMessage);
            bufferCount++;
            
            if (bufferCount >= 10) { // Flush after 10 messages
                flushLogBuffer();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Logs an error message.
     *
     * @param errorMessage The error message to log.
     */
    public void logError(String errorMessage) {
        lock.lock();
        try {
            String formattedMessage = String.format("[%s][ERROR] %s%n", 
                LocalDateTime.now().format(DATE_FORMAT), 
                errorMessage);
            
            System.err.print(formattedMessage);
            
            logBuffer.append(formattedMessage);
            flushLogBuffer(); // Always flush errors immediately
        } finally {
            lock.unlock();
        }
    }

    /**
     * Logs a warning message and writes it to a log file.
     *
     * @param message The warning message to log.
     */
    public void logWarning(String message) {
        lock.lock();
        try {
            if (message == null || message.trim().isEmpty()) {
                message = "An unspecified warning was triggered.";
            }

            String formattedMessage = String.format("[%s][WARNING] %s%n", 
                LocalDateTime.now().format(DATE_FORMAT), 
                message);
            
            System.out.print(formattedMessage);
            
            logBuffer.append(formattedMessage);
            bufferCount++;
            
            if (bufferCount >= 5) { // Flush more frequently for warnings
                flushLogBuffer();
            }
        } finally {
            lock.unlock();
        }
    }

    private void flushLogBuffer() {
        if (logBuffer.length() > 0) {
            try {
                Path logPath = Paths.get(LOG_FILE);
                if (!Files.exists(logPath)) {
                    Files.createFile(logPath);
                }
                
                Files.write(
                    logPath, 
                    logBuffer.toString().getBytes(),
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.APPEND
                );
                logBuffer.setLength(0);
                bufferCount = 0;
            } catch (IOException e) {
                System.err.println("[ERROR] Failed to flush log buffer: " + e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            flushLogBuffer();
            if (isTracking) {
                completeTracking();
            }
        } finally {
            lock.unlock();
        }
    }

    // Method to ensure resource cleanup
    public void cleanupResources() {
        close();
    }
}