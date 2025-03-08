package org.example;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicInteger;

public class ProgressTracker {

    private long startTime;
    private AtomicInteger totalSteps = new AtomicInteger(0); // Support for dynamic step adjustments
    private AtomicInteger completedSteps = new AtomicInteger(0); // Track completed steps

    /**
     * Starts tracking the progress of a task.
     *
     * @param totalSteps The total number of steps in the process.
     */
    public void startTracking(int totalSteps) {
        this.startTime = System.currentTimeMillis();
        this.totalSteps.set(totalSteps);
        this.completedSteps.set(0);
        System.out.println("Process started with " + totalSteps + " steps.");
    }

    /**
     * Updates the progress for the given step.
     *
     * @param description A description of the task being completed.
     */
    public void updateProgress(String description) {
        int currentStep = completedSteps.incrementAndGet();
        if (currentStep > totalSteps.get()) {
            System.out.println("Error: Current step exceeds total steps.");
            return;
        }

        // Calculate percentage
        double percentage = ((double) currentStep / totalSteps.get()) * 100;
        DecimalFormat df = new DecimalFormat("0.00");

        // Display progress
        System.out.println("Step " + currentStep + "/" + totalSteps.get() + " Completed (" + df.format(percentage) + "%)");
        System.out.println("Task: " + description);
    }

    /**
     * Completes the progress and logs the total time taken.
     */
    public void completeTracking() {
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Process completed in " + formatDuration(duration) + ".");
    }

    /**
     * Dynamically adjusts the total steps during execution.
     *
     * @param newTotalSteps The new total number of steps.
     */
    public void adjustTotalSteps(int newTotalSteps) {
        if (newTotalSteps < completedSteps.get()) {
            System.out.println("Error: New total steps cannot be less than completed steps.");
            return;
        }
        this.totalSteps.set(newTotalSteps);
        System.out.println("Total steps updated to: " + newTotalSteps);
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
     * Logs a custom message for debugging or updates.
     *
     * @param message The message to log.
     */
    public void logMessage(String message) {
        System.out.println("[LOG] " + message);
    }
}
