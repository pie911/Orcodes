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
        float margin = 50; // Margins for the page
        float qrCodeSize = 100; // Standard QR code size
        float horizontalSpacing = 20; // Space between QR codes horizontally
        float verticalSpacing = 30; // Space between QR codes vertically
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();

        float x = margin; // Initial x position for QR code placement
        float y = pageHeight - margin; // Initial y position for QR code placement

        // Create content stream for writing content to the page
        PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);

        for (QRCodeDetails qrDetails : qrDetailsList) {
            // Move to the next row if QR code exceeds the horizontal boundary
            if (x + qrCodeSize > pageWidth - margin) {
                x = margin; // Reset x to the left margin
                y -= qrCodeSize + verticalSpacing; // Move down to the next row
            }

            // If y-position is below the bottom margin, create a new page
            if (y - qrCodeSize < margin) {
                tracker.logMessage("[INFO] Adding a new page due to insufficient space.");
                contentStream.close(); // Close the current content stream

                // Create a new page and reset positions
                page = new PDPage(document.getPage(0).getMediaBox());
                document.addPage(page);
                contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);
                x = margin;
                y = pageHeight - margin;
            }

            // Draw a border (box) around the QR code
            contentStream.setLineWidth(0.5f); // Border thickness
            contentStream.addRect(x, y - qrCodeSize, qrCodeSize, qrCodeSize); // Define the box dimensions
            contentStream.stroke(); // Render the box

            // Embed the QR code image inside the box
            PDImageXObject qrImage = PDImageXObject.createFromFile(qrDetails.getQrFilePath(), document);
            contentStream.drawImage(qrImage, x, y - qrCodeSize, qrCodeSize, qrCodeSize);

            // Add a label below the QR code with the slug or page name
            String slugName = extractAndTruncateSlug(qrDetails.getLink());
            PDType0Font font = loadFont(); // Load font once for text display
            contentStream.beginText();
            contentStream.setFont(font, 10);
            contentStream.newLineAtOffset(x + 5, y - qrCodeSize - 15);
            contentStream.showText("Page: " + slugName);
            contentStream.endText();

            // Move to the next QR code position horizontally
            x += qrCodeSize + horizontalSpacing;
        }

        contentStream.close(); // Close the content stream when done
    }


    private String extractAndTruncateSlug(String url) {
        if (url == null || url.isEmpty()) return "Unknown";

        String[] parts = url.split("/");
        String slug = parts[parts.length - 1];
        return slug.length() > 7 ? slug.substring(0, 7) : slug;
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
        float cellWidth = tableWidth / 3;
        float cellHeight = 120;
        float yStart = PDRectangle.A4.getHeight() - margin;

        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDPageContentStream contentStream = new PDPageContentStream(document, page);

        float x = margin;
        float y = yStart;
        int columnCount = 0;

        for (var entry : qrData.entrySet()) {
            for (QRCodeDetails qrDetails : entry.getValue()) {
                if (columnCount == 3) {
                    columnCount = 0;
                    x = margin;
                    y -= cellHeight;
                }

                if (y - cellHeight < margin) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    y = yStart;
                }

                // Draw cell and QR code details
                contentStream.setLineWidth(0.5f);
                contentStream.addRect(x, y - cellHeight, cellWidth, cellHeight);
                contentStream.stroke();

                String slugName = extractAndTruncateSlug(qrDetails.getLink());
                PDType0Font font = loadFont();
                contentStream.beginText();
                contentStream.setFont(font, 10);
                contentStream.newLineAtOffset(x + 5, y - 20);
                contentStream.showText("Page: " + slugName);
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
    /**
     * Embeds QR codes into the original PDF and appends QR Code Table pages at the end.
     *
     * @param documentPath  The path to the original PDF file.
     * @param qrData        A mapping of page numbers to QR code details.
     * @param outputPath    The path to save the final PDF with QR codes embedded.
     * @param tracker       The progress tracker for logging progress.
     * @throws IOException  If any error occurs during the process.
     */
    public void embedQRCodes(String documentPath, HashMap<Integer, List<QRCodeDetails>> qrData, String outputPath, ProgressTracker tracker) throws IOException {
        tracker.logMessage("[INFO] Starting QR code embedding process...");

        // Step 1: Load the PDF document
        loadPDF(documentPath);

        try {
            // Step 2: Embed QR codes on specified pages
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

            // Step 3: Append QR Code Table pages to the end
            tracker.logMessage("[INFO] Appending QR Code Table pages...");
            appendQrTablePages(new TreeMap<>(qrData), tracker);

            // Step 4: Ensure the final PDF is saved with the `.pdf` extension
            if (!outputPath.endsWith(".pdf")) {
                outputPath += ".pdf";
            }
            savePDF(outputPath);
            tracker.logMessage("[INFO] QR code embedding process completed successfully. File saved at: " + outputPath);

        } catch (IOException e) {
            tracker.logError("[ERROR] Failed to embed QR codes: " + e.getMessage());
            throw e;

        } finally {
            // Step 5: Ensure the document is closed to release resources
            closePDF();
        }
    }

}