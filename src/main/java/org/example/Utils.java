package org.example;

import java.io.File;

public class Utils {

    /**
     * Validates if a given directory path exists and is a directory.
     *
     * @param directoryPath The directory path to validate.
     * @return True if the directory exists and is a directory, false otherwise.
     */
    public static boolean validateDirectoryPath(String directoryPath) {
        File directory = new File(directoryPath);
        if (directory.exists() && directory.isDirectory()) {
            return true;
        } else {
            System.err.println("Invalid directory: " + directoryPath);
            return false;
        }
    }

    /**
     * Validates if a given file path exists and is accessible.
     *
     * @param filePath The file path to validate.
     * @return True if the file exists and is readable, false otherwise.
     */
    public static boolean validateFilePath(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.canRead()) {
            return true;
        } else {
            System.err.println("Invalid or unreadable file: " + filePath);
            return false;
        }
    }

    /**
     * Sanitizes a folder or file name by replacing illegal characters with underscores and handling length restrictions.
     *
     * @param name The original name to sanitize.
     * @return A sanitized name with illegal characters replaced.
     */
    public static String sanitizeFileName(String name) {
        if (name == null || name.isEmpty()) {
            return "default_name";
        }

        // Replace illegal characters with underscores
        String sanitized = name.replaceAll("[^a-zA-Z0-9-_\\.]", "_");

        // Limit the file name to 255 characters to comply with most file systems
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }

        return sanitized;
    }

    /**
     * Measures the time taken for a specific task and logs the result with optional error handling.
     *
     * @param task The task to execute.
     * @param taskName The name of the task for logging purposes.
     */
    public static void measureExecutionTime(Runnable task, String taskName) {
        long startTime = System.currentTimeMillis();
        try {
            task.run();
            long duration = System.currentTimeMillis() - startTime;
            System.out.println(taskName + " completed successfully in " + duration + " ms.");
        } catch (Exception e) {
            System.err.println(taskName + " failed: " + e.getMessage());
        }
    }

    /**
     * Generates a unique timestamp-based string for file naming.
     *
     * @return A unique string based on the current timestamp.
     */
    public static String generateTimestampedName() {
        return String.valueOf(System.currentTimeMillis());
    }

    /**
     * Deletes a file if it exists.
     *
     * @param filePath The path to the file to delete.
     * @return True if the file was deleted, false otherwise.
     */
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }
}
