package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicInteger;

public class ProgressTracker {

    private long startTime; // Tracks the start time of the process
    private final AtomicInteger totalSteps = new AtomicInteger(0); // Total steps in the process
    private final AtomicInteger completedSteps = new AtomicInteger(0); // Completed steps count
    private boolean isTracking = false; // Tracks whether the process has started

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

        // Calculate progress percentage
        double percentage = ((double) currentStep / totalSteps.get()) * 100;
        DecimalFormat df = new DecimalFormat("0.00");

        // Display the progress update
        logMessage("Step " + currentStep + "/" + totalSteps.get() + " Completed (" + df.format(percentage) + "%)");
        logMessage("Task: " + description);
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
        DecimalFormat df = new DecimalFormat("0.00");
        return df.format(percentage) + "%";
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
    public synchronized void logMessage(String message) {
        System.out.println("[INFO] " + message);
    }

    /**
     * Logs an error message.
     *
     * @param errorMessage The error message to log.
     */
    public synchronized void logError(String errorMessage) {
        System.err.println("[ERROR] " + errorMessage);
    }

    /**
     * Logs a warning message and writes it to a log file.
     *
     * @param message The warning message to log.
     */
    public void logWarning(String message) {
        if (message == null || message.trim().isEmpty()) {
            message = "An unspecified warning was triggered.";
        }

        String formattedMessage = "[WARNING] " + message;
        System.out.println(formattedMessage);

        // Save the warning to a log file
        try (FileWriter writer = new FileWriter("application.log", true)) {
            writer.write(formattedMessage + "\n");
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to write warning to log file: " + e.getMessage());
        }
    }
}