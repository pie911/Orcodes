package org.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class FolderManager {
    // Add counter for directory operations
    private final AtomicInteger directoryOperations = new AtomicInteger(0);
    private static final int GC_THRESHOLD = 50; // Trigger GC after 50 directory operations

    /**
     * Creates the main user directory at the specified base path with proper permissions.
     *
     * @param basePath The base path where the user directory should be created.
     * @param userName The name of the user directory.
     * @return The path to the created user directory.
     * @throws IOException If the directory cannot be created or lacks permissions.
     */
    public String createUserDirectory(String basePath, String userName) throws IOException {
        // Sanitize user-provided folder name
        String userDirPath = basePath + (basePath.endsWith("/") || basePath.endsWith("\\") ? "" : "/") + sanitizeFolderName(userName);
        Path userDir = Path.of(userDirPath);

        // Create directories if they do not exist
        if (!Files.exists(userDir)) {
            Files.createDirectories(userDir);
            System.out.println("[INFO] User directory created: " + userDirPath);
            setDirectoryPermissions(userDir); // Apply permissions
        } else {
            System.out.println("[INFO] User directory already exists: " + userDirPath);
        }

        // Verify write access
        validateWritableDirectory(userDirPath);

        // Improved garbage collection management
        checkAndRunGC("User directory creation");

        return userDirPath;
    }

    /**
     * Creates a folder for the specified document under the user directory.
     *
     * @param userDir  The path to the user's directory.
     * @param fileName The name of the document folder.
     * @return The path to the document's directory.
     * @throws IOException If the folder cannot be created or lacks permissions.
     */
    public String createDocumentDirectory(String userDir, String fileName) throws IOException {
        String docDirPath = userDir + "/" + sanitizeFolderName(fileName);
        Path docDir = Path.of(docDirPath);

        // Create document directory if it doesn't exist
        if (!Files.exists(docDir)) {
            Files.createDirectory(docDir);
            System.out.println("[INFO] Document directory created: " + docDirPath);
            setDirectoryPermissions(docDir); // Apply permissions
        } else {
            System.out.println("[INFO] Document directory already exists: " + docDirPath);
        }

        // Verify write access
        validateWritableDirectory(docDirPath);

        // Improved garbage collection management
        checkAndRunGC("Document directory creation");

        return docDirPath;
    }

    /**
     * Creates a folder for a specific page under the document directory to store QR codes.
     *
     * @param documentDir The path to the document's directory.
     * @param pageNo      The page number for which the folder is created.
     * @return The path to the page's QR code directory.
     * @throws IOException If the folder cannot be created or lacks permissions.
     */
    public String createPageDirectory(String documentDir, int pageNo) throws IOException {
        String pageDirPath = documentDir + "/QrCodes/PageNo" + pageNo;
        Path pageDir = Path.of(pageDirPath);

        // Create page directory if it doesn't exist
        if (!Files.exists(pageDir)) {
            Files.createDirectories(pageDir);
            System.out.println("[INFO] Page directory created for page " + pageNo + ": " + pageDirPath);
            setDirectoryPermissions(pageDir); // Apply permissions
        } else {
            System.out.println("[INFO] Page directory already exists for page " + pageNo + ": " + pageDirPath);
        }

        // Verify write access
        validateWritableDirectory(pageDirPath);

        // Improved garbage collection management
        checkAndRunGC("Page directory creation");

        return pageDirPath;
    }

    /**
     * Utility method to sanitize folder names by removing illegal characters.
     *
     * @param name The folder name to sanitize.
     * @return A sanitized folder name.
     */
    public static String sanitizeFolderName(String name) {
        if (name == null || name.isEmpty()) {
            return "default_name";
        }
        return name.replaceAll("[^a-zA-Z0-9-_]", "_");
    }

    /**
     * Grants directory permissions dynamically based on the operating system.
     *
     * @param path The path to the directory.
     * @throws IOException If permissions cannot be applied.
     */
    private void setDirectoryPermissions(Path path) throws IOException {
        String osName = System.getProperty("os.name").toLowerCase();
        
        try {
            if (osName.contains("win")) {
                setWindowsPermissions(path);
            } else if (osName.contains("linux") || osName.contains("mac")) {
                setPosixPermissions(path);
            }
        } catch (SecurityException e) {
            System.out.println("[WARN] Security restrictions prevent setting permissions: " + e.getMessage());
            validateBasicWriteAccess(path);
        } catch (UnsupportedOperationException e) {
            System.out.println("[WARN] File system does not support permission changes: " + e.getMessage());
            validateBasicWriteAccess(path);
        } catch (IOException e) {
            System.out.println("[ERROR] Failed to set permissions: " + e.getMessage());
            throw new IOException("Permission setting failed for: " + path, e);
        }
    }

    private void setWindowsPermissions(Path path) throws IOException {
        File file = path.toFile();
        boolean success = file.setWritable(true, false) &&
                         file.setReadable(true, false) &&
                         file.setExecutable(true, false);
        
        if (!success) {
            throw new IOException("[ERROR] Failed to set Windows permissions for: " + path);
        }
        System.out.println("[INFO] Windows permissions applied: " + path);
    }

    private void setPosixPermissions(Path path) throws IOException {
        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxrwxr--");
        Files.setPosixFilePermissions(path, permissions);
        System.out.println("[INFO] POSIX permissions applied: " + path);
    }

    private void validateBasicWriteAccess(Path path) throws IOException {
        if (!Files.isWritable(path)) {
            throw new IOException("[ERROR] Basic write access check failed for: " + path);
        }
        System.out.println("[INFO] Basic write access verified for: " + path);
    }

    /**
     * Validates if a directory is writable, throwing an IOException if it is not.
     *
     * @param dirPath The directory path to validate.
     * @throws IOException If the directory is not writable.
     */
    private void validateWritableDirectory(String dirPath) throws IOException {
        Path path = Path.of(dirPath);
        if (!Files.isWritable(path)) {
            throw new IOException("[ERROR] Write permission denied for directory: " + dirPath);
        }
    }

    /**
     * Improved garbage collection management.
     *
     * @param operation The operation after which garbage collection is checked.
     */
    private void checkAndRunGC(String operation) {
        int operations = directoryOperations.incrementAndGet();
        if (operations >= GC_THRESHOLD) {
            System.gc();
            System.out.println("[INFO] Garbage collection triggered after " + GC_THRESHOLD + " " + operation + " operations");
            directoryOperations.set(0);
        }
    }
}
