package org.example;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Represents the details of a QR code, including the page number, link, associated text,
 * and file path to the QR code image.
 */
public class QRCodeDetails implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int MAX_LINK_LENGTH = 50;
    private static final int TRUNCATED_LINK_LENGTH = 47;
    private static final Pattern IMAGE_PATTERN = Pattern.compile(".*\\.(png|jpg|jpeg)$", Pattern.CASE_INSENSITIVE);
    private static final AtomicInteger instanceCounter = new AtomicInteger(0);

    private final int pageNo; // The page number where the QR code is placed
    private final String link; // The link encoded in the QR code
    private volatile String text; // The associated text or description (mutable for dynamic updates)
    private volatile String qrFilePath; // The file path to the saved QR code image
    private final int instanceId;

    /**
     * Constructor for QRCodeDetails.
     *
     * @param pageNo     The page number where the QR code is placed (must be >= 1).
     * @param link       The URL or link encoded in the QR code (non-null and non-empty).
     * @param qrFilePath The file path to the saved QR code image (non-null and non-empty).
     * @param text       The associated text or description (optional, can be null).
     * @throws IllegalArgumentException if any parameter is invalid.
     */
    public QRCodeDetails(int pageNo, String link, String qrFilePath, String text) {
        validateInputs(pageNo, link, qrFilePath);
        
        this.pageNo = pageNo;
        this.link = link;
        this.qrFilePath = qrFilePath;
        this.text = text != null ? text : ""; // Default to empty if null
        this.instanceId = instanceCounter.incrementAndGet();
    }

    private void validateInputs(int pageNo, String link, String qrFilePath) {
        StringBuilder errors = new StringBuilder();
        
        if (pageNo < 1) {
            errors.append("Invalid page number: ").append(pageNo).append(". Must be >= 1. ");
        }
        if (link == null || link.trim().isEmpty()) {
            errors.append("Link cannot be null or empty. ");
        }
        if (qrFilePath == null || qrFilePath.trim().isEmpty()) {
            errors.append("QR code file path cannot be null or empty.");
        }
        
        if (errors.length() > 0) {
            throw new IllegalArgumentException(errors.toString().trim());
        }
    }

    // Getters with null safety
    public int getPageNo() {
        return pageNo;
    }

    public String getLink() {
        return link;
    }

    public String getText() {
        return text != null ? text : "";
    }

    public String getQrFilePath() {
        return qrFilePath;
    }

    // Allows dynamic updates to the text description
    public void setText(String text) {
        this.text = text != null ? text : "";
    }

    /**
     * Sets the file path for the QR code image.
     *
     * @param filePath The new file path to set for the QR code image.
     * @throws IllegalArgumentException If the file path is null, empty, or invalid.
     */
    public void setQrFilePath(String filePath) {
        validateFilePath(filePath);
        this.qrFilePath = filePath;
        System.out.println("[INFO] QR code file path updated successfully: " + filePath);
    }

    private void validateFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("[ERROR] QR code file path cannot be null or empty.");
        }
        if (!IMAGE_PATTERN.matcher(filePath).matches()) {
            throw new IllegalArgumentException("[ERROR] Invalid image file format. Supported formats: PNG, JPG, JPEG");
        }
    }

    @Override
    public String toString() {
        return String.format("QRCodeDetails{id=%d, pageNo=%d, link='%s', text='%s', qrFilePath='%s'}",
                instanceId,
                pageNo,
                truncateLink(link),
                text,
                qrFilePath);
    }

    private String truncateLink(String link) {
        return link != null && link.length() > MAX_LINK_LENGTH ? 
               link.substring(0, TRUNCATED_LINK_LENGTH) + "..." : 
               link;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QRCodeDetails that = (QRCodeDetails) o;
        return pageNo == that.pageNo && 
               Objects.equals(link, that.link) && 
               Objects.equals(qrFilePath, that.qrFilePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageNo, link, qrFilePath);
    }
}