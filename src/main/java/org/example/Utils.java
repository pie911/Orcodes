package org.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    /**
     * Validates if a directory path exists and is accessible.
     *
     * @param directoryPath The directory path to validate.
     * @return True if the directory exists and is accessible, false otherwise.
     */
    public static boolean validateDirectoryPath(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            System.err.println("[ERROR] Directory path is null or empty.");
            return false;
        }

        File directory = new File(directoryPath);
        if (directory.exists() && directory.isDirectory()) {
            return true;
        } else {
            System.err.println("[ERROR] Invalid directory: " + directoryPath);
            return false;
        }
    }

    /**
     * Validates if a file path exists and is accessible.
     *
     * @param filePath The file path to validate.
     * @return True if the file exists and is readable, false otherwise.
     */
    public static boolean validateFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            System.err.println("[ERROR] File path is null or empty.");
            return false;
        }

        File file = new File(filePath);
        if (file.exists() && file.canRead()) {
            return true;
        } else {
            System.err.println("[ERROR] Invalid or unreadable file: " + filePath);
            return false;
        }
    }

    /**
     * Sanitizes a file name by removing illegal characters and limiting its length.
     *
     * @param name The file name to sanitize.
     * @return A sanitized file name.
     */
    public static String sanitizeFileName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "default_name";
        }

        // Replace invalid characters with underscores and limit length to 255 characters
        String sanitized = name.replaceAll("[^a-zA-Z0-9-_.]", "_");
        return sanitized.length() > 255 ? sanitized.substring(0, 255) : sanitized;
    }

    /**
     * Measures the execution time of a task and logs it.
     *
     * @param task     The task to execute.
     * @param taskName The name of the task for logging.
     */
    public static void measureExecutionTime(Runnable task, String taskName) {
        long startTime = System.currentTimeMillis();
        try {
            task.run();
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("[INFO] " + taskName + " completed successfully in " + duration + " ms.");
        } catch (Exception e) {
            System.err.println("[ERROR] " + taskName + " failed: " + e.getMessage());
        }
    }

    /**
     * Generates a timestamped name.
     *
     * @return A unique name based on the current timestamp.
     */
    public static String generateTimestampedName() {
        return "file_" + System.currentTimeMillis();
    }

    /**
     * Deletes a file or directory. If it's a directory, deletes all contents recursively.
     *
     * @param filePath The path to the file or directory.
     * @return True if deletion is successful, false otherwise.
     */
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);

        if (!file.exists()) {
            System.err.println("[WARN] File or directory does not exist: " + filePath);
            return false;
        }

        if (file.isDirectory()) {
            File[] subFiles = file.listFiles();
            if (subFiles != null) {
                for (File subFile : subFiles) {
                    if (!deleteFile(subFile.getPath())) {
                        System.err.println("[ERROR] Failed to delete: " + subFile.getPath());
                    }
                }
            }
        }

        boolean isDeleted = file.delete();
        if (isDeleted) {
            System.out.println("[INFO] Deleted: " + filePath);
        } else {
            System.err.println("[ERROR] Could not delete: " + filePath);
        }
        return isDeleted;
    }

    /**
     * Creates a directory if it does not already exist.
     *
     * @param directoryPath The path of the directory to create.
     * @throws IOException If the directory cannot be created.
     */
    public static void createDirectory(String directoryPath) throws IOException {
        Path path = Paths.get(directoryPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            System.out.println("[INFO] Directory created: " + directoryPath);
        }
    }

    /**
     * Copies a file from the source path to the destination path.
     *
     * @param sourcePath      The source file path.
     * @param destinationPath The destination file path.
     * @throws IOException If the source file does not exist or cannot be copied.
     */
    public static void copyFile(String sourcePath, String destinationPath) throws IOException {
        if (sourcePath == null || sourcePath.trim().isEmpty() ||
                destinationPath == null || destinationPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Source or destination path cannot be null or empty.");
        }

        Path source = Paths.get(sourcePath);
        Path destination = Paths.get(destinationPath);

        if (!Files.exists(source)) {
            throw new IOException("Source file does not exist: " + sourcePath);
        }

        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[INFO] " + sourcePath + " copied to " + destinationPath);
    }

    /**
     * Lists the contents of a directory.
     *
     * @param directoryPath   The directory to list contents from.
     * @param includeFullPaths If true, returns full paths; otherwise, returns only file names.
     * @return A list of directory contents.
     */
    public static List<String> listDirectoryContents(String directoryPath, boolean includeFullPaths) {
        List<String> contents = new ArrayList<>();
        File directory = new File(directoryPath);

        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    contents.add(includeFullPaths ? file.getAbsolutePath() : file.getName());
                }
            }
        } else {
            System.err.println("[ERROR] Invalid directory: " + directoryPath);
        }
        return contents;
    }

    /**
     * Moves a file from the source path to the destination path.
     *
     * @param sourcePath      The source file path.
     * @param destinationPath The destination file path.
     * @throws IOException If the source file does not exist or cannot be moved.
     */
    public static void moveFile(String sourcePath, String destinationPath) throws IOException {
        if (sourcePath == null || destinationPath == null ||
                sourcePath.trim().isEmpty() || destinationPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Source or destination path cannot be null or empty.");
        }

        Path source = Paths.get(sourcePath);
        Path destination = Paths.get(destinationPath);

        if (!Files.exists(source)) {
            throw new IOException("Source file does not exist: " + sourcePath);
        }

        Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[INFO] Moved " + sourcePath + " to " + destinationPath);
    }

    /**
     * Checks if a directory is writable, throwing an exception if not.
     *
     * @param dirPath The directory path to check.
     * @throws IOException If the directory is not writable.
     */
    public static void validateWritableDirectory(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        if (!Files.isWritable(path)) {
            throw new IOException("[ERROR] Directory is not writable: " + dirPath);
        }
    }
}
