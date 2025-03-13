package org.example;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PDFEditor {

    private PDDocument document; // The in-memory representation of the PDF document.

    /**
     * Loads a PDF into memory from the specified file path.
     *
     * @param filePath The path of the PDF file to load.
     * @throws IOException If the file cannot be loaded.
     */
    public void loadPDF(String filePath) throws IOException {
        this.document = Loader.loadPDF(new File(filePath));
        System.out.println("[INFO] PDF loaded successfully from: " + filePath);
    }

    /**
     * Saves the current document to the specified output file path.
     *
     * @param outputFilePath The path where the updated PDF should be saved.
     * @throws IOException If the file cannot be saved.
     */
    public void savePDF(String outputFilePath) throws IOException {
        if (this.document == null) {
            throw new IllegalStateException("[ERROR] No PDF document is loaded.");
        }
        this.document.save(new File(outputFilePath));
        System.out.println("[INFO] PDF saved successfully to: " + outputFilePath);
    }

    /**
     * Closes the current PDF document and releases associated resources.
     *
     * @throws IOException If an error occurs while closing the document.
     */
    public void closePDF() throws IOException {
        if (this.document != null) {
            this.document.close();
            System.out.println("[INFO] PDF document closed.");
        }
    }

    /**
     * Embeds QR codes into specific pages of the PDF and appends QR Code Table pages at the end.
     *
     * @param qrData        A mapping of page numbers to lists of QR code details.
     * @param outputPdfPath The path where the updated PDF should be saved.
     * @param tracker       A progress tracker to monitor and log the process.
     * @throws IOException If any error occurs during the embedding process.
     */
    public void embedQRCodes(TreeMap<Integer, List<QRCodeDetails>> qrData, String outputPdfPath, ProgressTracker tracker) throws IOException {
        if (this.document == null) {
            throw new IllegalStateException("[ERROR] No PDF document is loaded.");
        }

        tracker.logMessage("[INFO] Embedding QR codes into the PDF...");

        for (Map.Entry<Integer, List<QRCodeDetails>> entry : qrData.entrySet()) {
            int pageNo = entry.getKey();
            List<QRCodeDetails> qrDetailsList = entry.getValue();

            if (pageNo < 1 || pageNo > document.getNumberOfPages()) {
                tracker.logError("[WARNING] Page number " + pageNo + " is invalid. Skipping...");
                continue;
            }

            PDPage page = document.getPage(pageNo - 1); // 0-indexed
            embedQRDetailsOnPage(page, qrDetailsList, tracker);
        }

        tracker.logMessage("[INFO] Appending QR Code Table pages...");
        appendQrTablePages(qrData, tracker);

        savePDF(outputPdfPath);
        tracker.logMessage("[INFO] PDF processing completed successfully.");
    }

    /**
     * Embeds QR codes on a specific page of the PDF.
     *
     * @param page           The page where QR codes will be embedded.
     * @param qrDetailsList  The list of QR code details for the page.
     * @param tracker        The progress tracker for logging progress.
     * @throws IOException   If an error occurs during QR code embedding.
     */
    private void embedQRDetailsOnPage(PDPage page, List<QRCodeDetails> qrDetailsList, ProgressTracker tracker) throws IOException {
        float margin = 50;
        float qrCodeSize = 100;
        float horizontalSpacing = 20;
        float verticalSpacing = 30;
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();

        float x = margin; // Start placing from the left margin
        float y = pageHeight - margin; // Start placing from the top margin

        PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);

        for (QRCodeDetails qrDetails : qrDetailsList) {
            // Adjust position for QR codes dynamically
            if (x + qrCodeSize > pageWidth - margin) {
                x = margin; // Reset X to the left margin
                y -= qrCodeSize + verticalSpacing; // Move down to the next row
            }

            if (y - qrCodeSize < margin) { // Not enough space
                tracker.logWarning("[WARNING] Insufficient space on the page for more QR codes.");
                break;
            }

            // Embed QR code
            String qrFilePath = qrDetails.getQrFilePath();
            PDImageXObject qrImage = PDImageXObject.createFromFile(qrFilePath, document);
            contentStream.drawImage(qrImage, x, y - qrCodeSize, qrCodeSize, qrCodeSize);

            // Add text label below the QR code
            String pageName = extractAndTruncatePageName(qrDetails.getLink());
            PDType0Font font = loadFont();
            contentStream.beginText();
            contentStream.setFont(font, 10);
            contentStream.newLineAtOffset(x + 5, y - qrCodeSize - 15);
            contentStream.showText("Page: " + pageName);
            contentStream.endText();

            x += qrCodeSize + horizontalSpacing; // Move to the next position horizontally
        }

        contentStream.close();
        tracker.logMessage("[INFO] QR codes embedded successfully on the page.");
    }


    /**
     * Appends QR Code Table pages to the end of the PDF document.
     *
     * @param qrData   A sorted mapping of page numbers to QR code details.
     * @param tracker  A progress tracker for logging progress.
     * @throws IOException If an error occurs during table appending.
     */
    private void appendQrTablePages(TreeMap<Integer, List<QRCodeDetails>> qrData, ProgressTracker tracker) throws IOException {
        float margin = 50;
        float tableWidth = PDRectangle.A4.getWidth() - 2 * margin;
        float cellWidth = tableWidth / 3; // 3 columns
        float cellHeight = 120; // Each row height
        float yStart = PDRectangle.A4.getHeight() - margin;

        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDPageContentStream contentStream = new PDPageContentStream(document, page);

        float x = margin; // Start at left margin
        float y = yStart; // Start at top margin
        int columnCount = 0;

        for (var entry : qrData.entrySet()) {
            int pageNo = entry.getKey();
            for (QRCodeDetails qrDetails : entry.getValue()) {
                if (columnCount == 3) { // Move to the next row after 3 columns
                    columnCount = 0;
                    x = margin;
                    y -= cellHeight;
                }

                if (y - cellHeight < margin) { // Add new page if out of space
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    y = yStart;
                }

                // Draw cell borders
                contentStream.setLineWidth(0.5f);
                contentStream.addRect(x, y - cellHeight, cellWidth, cellHeight);
                contentStream.stroke();

                // Add page number and QR code label
                PDType0Font font = loadFont();
                contentStream.beginText();
                contentStream.setFont(font, 10);
                contentStream.newLineAtOffset(x + 5, y - 20);
                contentStream.showText("Page: " + pageNo);
                contentStream.endText();

                x += cellWidth;
                columnCount++;
            }
        }

        contentStream.close();
        tracker.logMessage("[INFO] QR Code Table pages appended successfully.");
    }


    /**
     * Extracts the last portion of a URL and truncates it to a maximum of 7 characters.
     *
     * @param url The full URL to extract from.
     * @return The extracted and truncated page name.
     */
    private String extractAndTruncatePageName(String url) {
        if (url == null || url.isEmpty()) return "Unknown";

        String[] parts = url.split("/");
        String pageName = parts[parts.length - 1];
        return pageName.length() > 7 ? pageName.substring(0, 7) : pageName;
    }

    /**
     * Loads the font from the resources folder.
     *
     * @return The loaded PDType0Font.
     * @throws IOException If the font cannot be loaded.
     */
    private PDType0Font loadFont() throws IOException {
        // Load the font from the resources folder
        InputStream fontStream = getClass().getClassLoader().getResourceAsStream("fonts/times.ttf");
        if (fontStream == null) {
            throw new IOException("[ERROR] Font file 'times.ttf' not found in resources.");
        }

        // Load the font into the PDF document
        PDType0Font font;
        try {
            font = PDType0Font.load(document, fontStream);
        } catch (IOException e) {
            throw new IOException("[ERROR] Failed to load font from 'times.ttf'. Ensure the font is valid and accessible.", e);
        }

        System.out.println("[INFO] Font 'times.ttf' loaded successfully.");
        return font;
    }

    /**
     * Embeds QR codes into the original PDF and appends the QR Code Table pages at the end.
     *
     * @param documentPath The path to the original PDF file.
     * @param qrData       A mapping of page numbers to QR code details.
     * @param outputPath   The path to save the final PDF with QR codes embedded.
     * @param tracker      The progress tracker for logging progress.
     * @throws IOException If any error occurs during the process.
     */
    public void embedQRCodes(String documentPath, HashMap<Integer, List<QRCodeDetails>> qrData, String outputPath, ProgressTracker tracker) throws IOException {
        tracker.logMessage("[INFO] Starting QR code embedding process...");

        // Load the PDF document
        loadPDF(documentPath);

        try {
            // Step 1: Embed QR codes on specified pages
            for (var entry : qrData.entrySet()) {
                int pageNo = entry.getKey();
                List<QRCodeDetails> qrCodeDetailsList = entry.getValue();

                if (pageNo < 1 || pageNo > document.getNumberOfPages()) {
                    tracker.logWarning("[WARNING] Page number " + pageNo + " is out of range. Skipping...");
                    continue;
                }

                PDPage page = document.getPage(pageNo - 1); // Pages are 0-indexed
                tracker.logMessage("[INFO] Embedding QR codes on page " + pageNo + "...");
                embedQRDetailsOnPage(page, qrCodeDetailsList, tracker);
            }

            // Step 2: Append QR Code Table pages at the end
            tracker.logMessage("[INFO] Appending QR Code Table pages...");
            appendQrTablePages(new TreeMap<>(qrData), tracker);

            // Step 3: Save the updated document
            savePDF(outputPath);
            tracker.logMessage("[INFO] QR code embedding process completed successfully. File saved at: " + outputPath);

        } catch (IOException e) {
            tracker.logError("[ERROR] Failed to embed QR codes: " + e.getMessage());
            throw e;

        } finally {
            // Ensure the document is closed to release resources
            closePDF();
        }
    }
}