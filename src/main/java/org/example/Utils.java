package org.example;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Utils {
    private static final int BUFFER_SIZE = 8192; // 8KB buffer size for file operations
    private static final ConcurrentHashMap<String, Long> operationTimings = new ConcurrentHashMap<>();
    private static final AtomicInteger operationCount = new AtomicInteger(0);
    private static final int GC_THRESHOLD = 100;

    /**
     * Validates both source and destination paths.
     *
     * @param sourcePath The source path to validate
     * @param destinationPath The destination path to validate
     * @throws IOException If either path is invalid
     */
    private static void validatePaths(String sourcePath, String destinationPath) throws IOException {
        if (sourcePath == null || sourcePath.trim().isEmpty()) {
            throw new IOException("[ERROR] Source path cannot be null or empty");
        }
        if (destinationPath == null || destinationPath.trim().isEmpty()) {
            throw new IOException("[ERROR] Destination path cannot be null or empty");
        }
    }

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

        try {
            Path path = Paths.get(directoryPath);
            return Files.isDirectory(path) && Files.isReadable(path);
        } catch (SecurityException | InvalidPathException e) {
            System.err.println("[ERROR] Invalid directory access: " + e.getMessage());
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

        try {
            Path path = Paths.get(filePath);
            return Files.isRegularFile(path) && Files.isReadable(path);
        } catch (SecurityException | InvalidPathException e) {
            System.err.println("[ERROR] Invalid file access: " + e.getMessage());
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
        long startTime = System.nanoTime();
        try {
            task.run();
            long duration = (System.nanoTime() - startTime) / 1_000_000; // Convert to milliseconds
            operationTimings.put(taskName, duration);
            
            if (operationCount.incrementAndGet() >= GC_THRESHOLD) {
                System.gc();
                operationCount.set(0);
            }
            
            System.out.printf("[INFO] %s completed in %d ms%n", taskName, duration);
        } catch (Exception e) {
            System.err.printf("[ERROR] %s failed: %s%n", taskName, e.getMessage());
            throw new RuntimeException(e);
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
     * Copies a file from the source path to the destination path using buffered streams.
     *
     * @param sourcePath      The source file path.
     * @param destinationPath The destination file path.
     * @throws IOException If the source file does not exist or cannot be copied.
     */
    public static void copyFile(String sourcePath, String destinationPath) throws IOException {
        validatePaths(sourcePath, destinationPath);

        File sourceFile = new File(sourcePath);
        File destFile = new File(destinationPath);

        // Create parent directories if they don't exist
        if (destFile.getParentFile() != null) {
            Files.createDirectories(destFile.getParentFile().toPath());
        }

        // Use buffered streams for better performance
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile), BUFFER_SIZE);
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destFile), BUFFER_SIZE)) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long totalBytes = 0;
            
            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            
            // Ensure all bytes are written
            bos.flush();
            
            System.out.printf("[INFO] Copied %s to %s (%d bytes)%n", 
                sourcePath, destinationPath, totalBytes);
            
            // Copy file attributes
            Files.copy(sourceFile.toPath(), destFile.toPath(), 
                StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

    /**
     * Lists the contents of a directory using buffered operations.
     *
     * @param directoryPath   The directory to list contents from.
     * @param includeFullPaths If true, returns full paths; otherwise, returns only file names.
     * @return A list of directory contents.
     */
    public static List<String> listDirectoryContents(String directoryPath, boolean includeFullPaths) {
        List<String> results = new ArrayList<>();
        try {
            Path dir = Paths.get(directoryPath);
            return Files.walk(dir, 1)
                .skip(1)
                .map(path -> includeFullPaths ? path.toString() : path.getFileName().toString())
                .sorted()
                .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to list directory contents: " + e.getMessage());
            return results;
        }
    }

    /**
     * Moves a file from the source path to the destination path using buffered operations.
     *
     * @param sourcePath      The source file path.
     * @param destinationPath The destination file path.
     * @throws IOException If the source file does not exist or cannot be moved.
     */
    public static void moveFile(String sourcePath, String destinationPath) throws IOException {
        validatePaths(sourcePath, destinationPath);

        File sourceFile = new File(sourcePath);
        File destFile = new File(destinationPath);

        if (!sourceFile.exists()) {
            throw new IOException("Source file does not exist: " + sourcePath);
        }

        // First try atomic move
        try {
            Files.move(sourceFile.toPath(), destFile.toPath(), 
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            // If atomic move is not supported, fall back to copy and delete
            copyFile(sourcePath, destinationPath);
            if (!deleteFile(sourcePath)) {
                throw new IOException("Failed to delete source file after copy: " + sourcePath);
            }
        }

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

    /**
     * Cleans up resources and performs garbage collection if needed
     */
    public static void cleanup() {
        operationTimings.clear();
        if (operationCount.get() > 0) {
            System.gc();
            operationCount.set(0);
        }
    }
}
