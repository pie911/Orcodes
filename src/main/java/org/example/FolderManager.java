package org.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public class FolderManager {

    /**
     * Creates the main user directory at the specified base path with proper permissions.
     *
     * @param basePath The base path where the user directory should be created.
     * @param userName The name of the user directory.
     * @return The path to the created user directory.
     * @throws IOException If the directory cannot be created or lacks permissions.
     */
    public String createUserDirectory(String basePath, String userName) throws IOException {
        String userDirPath = basePath + (basePath.endsWith("/") || basePath.endsWith("\\") ? "" : "/") + userName;
        Path userDir = Path.of(userDirPath);

        if (!Files.exists(userDir)) {
            Files.createDirectories(userDir);
            setDirectoryPermissions(userDir); // Grant permissions if necessary
        }

        // Verify write access
        if (!Files.isWritable(userDir)) {
            throw new IOException("Write permission denied for directory: " + userDirPath);
        }

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
        String docDirPath = userDir + "/" + fileName;
        Path docDir = Path.of(docDirPath);

        if (!Files.exists(docDir)) {
            Files.createDirectory(docDir);
            setDirectoryPermissions(docDir); // Grant permissions if necessary
        }

        // Verify write access
        if (!Files.isWritable(docDir)) {
            throw new IOException("Write permission denied for directory: " + docDirPath);
        }

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

        if (!Files.exists(pageDir)) {
            Files.createDirectories(pageDir);
            setDirectoryPermissions(pageDir); // Grant permissions if necessary
        }

        // Verify write access
        if (!Files.isWritable(pageDir)) {
            throw new IOException("Write permission denied for directory: " + pageDirPath);
        }

        return pageDirPath;
    }

    /**
     * Utility method to sanitize folder names by removing illegal characters.
     *
     * @param name The folder name to sanitize.
     * @return A sanitized folder name.
     */
    public static String sanitizeFolderName(String name) {
        return name.replaceAll("[^a-zA-Z0-9-_]", "_");
    }

    /**
     * Grants full directory permissions if the OS and file system support it.
     *
     * @param path The path to the directory.
     * @throws IOException If permissions cannot be applied.
     */
    private void setDirectoryPermissions(Path path) throws IOException {
        try {
            // Set permissions for UNIX-like systems (if applicable)
            Set<PosixFilePermission> permissions =
                    PosixFilePermissions.fromString("rwxrwxrwx"); // Full permissions
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException e) {
            // If POSIX permissions are not supported (e.g., Windows), do nothing
        }
    }
}
