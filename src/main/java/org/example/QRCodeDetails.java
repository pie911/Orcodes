package org.example;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents the details of a QR code, including the page number, link, associated text,
 * and file path to the QR code image.
 */
public class QRCodeDetails implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int pageNo; // The page number where the QR code is placed
    private final String link; // The link encoded in the QR code
    private String text; // The associated text or description (mutable for dynamic updates)
    private String qrFilePath; // The file path to the saved QR code image
    private String description; // The description of the QR code

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
        if (pageNo < 1) {
            throw new IllegalArgumentException("Invalid page number: " + pageNo + ". Page number must be >= 1.");
        }
        if (link == null || link.isEmpty()) {
            throw new IllegalArgumentException("Link cannot be null or empty.");
        }
        if (qrFilePath == null || qrFilePath.isEmpty()) {
            throw new IllegalArgumentException("QR code file path cannot be null or empty.");
        }

        this.pageNo = pageNo;
        this.link = link;
        this.qrFilePath = qrFilePath;
        this.text = text != null ? text : ""; // Default to empty if null
    }

    // Getters
    public int getPageNo() {
        return pageNo;
    }

    public String getLink() {
        return link;
    }

    public String getText() {
        return text;
    }

    public String getQrFilePath() {
        return qrFilePath;
    }

    public String getDescription() {
        return description;
    }

    // Allows dynamic updates to the text description
    public void setText(String text) {
        this.text = text;
    }

    public void setQrFilePath(String qrFilePath) {
        if (qrFilePath == null || qrFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("[ERROR] QR code file path cannot be null or empty.");
        }

        // Validate that the file path ends with an image extension (.png, .jpg, etc.)
        if (!(qrFilePath.endsWith(".png") || qrFilePath.endsWith(".jpg") || qrFilePath.endsWith(".jpeg"))) {
            throw new IllegalArgumentException("[ERROR] QR code file path must point to a valid image file (.png, .jpg, .jpeg).");
        }

        this.qrFilePath = qrFilePath;
        System.out.println("[INFO] QR code file path updated successfully: " + qrFilePath);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return String.format("QRCodeDetails{pageNo=%d, link='%s', text='%s', qrFilePath='%s'}",
                pageNo, link.length() > 50 ? link.substring(0, 47) + "..." : link, text, qrFilePath);
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