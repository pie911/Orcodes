package org.example;

import java.io.Serializable;
import java.util.Objects;

public class QRCodeDetails implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int pageNo; // The page number where the QR code is placed
    private final String link; // The link encoded in the QR code
    private String text; // The associated text or description (mutable for dynamic updates)
    private final String qrFilePath; // The file path to the saved QR code image

    // Constructor
    public QRCodeDetails(int pageNo, String link, String qrFilePath, String s) {
        if (pageNo < 1) {
            throw new IllegalArgumentException("Page number must be greater than or equal to 1.");
        }
        if (link == null || link.isEmpty()) {
            throw new IllegalArgumentException("Link cannot be null or empty.");
        }
        if (qrFilePath == null || qrFilePath.isEmpty()) {
            throw new IllegalArgumentException("QR code file path cannot be null or empty.");
        }

        this.pageNo = pageNo;
        this.link = link;
        this.qrFilePath = qrFilePath; // Assign the file path
        this.text = ""; // Initialize as empty; can be dynamically updated later
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

    public String getQrCodePath() {
        return qrFilePath;
    }

    // Setter for text (allows dynamic updates)
    public void setText(String text) {
        this.text = text;
    }

    // ToString for Debugging
    @Override
    public String toString() {
        return "QRCodeDetails{" +
                "pageNo=" + pageNo +
                ", link='" + link + '\'' +
                ", text='" + text + '\'' +
                ", qrFilePath='" + qrFilePath + '\'' +
                '}';
    }

    // Equals and HashCode for comparison
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
