package org.example;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public class PDFEditor implements AutoCloseable {

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
        tracker.logMessage("[INFO] Starting QR code embedding process...");

        // Step 1: Ensure the PDF is loaded
        if (this.document == null) {
            throw new IllegalStateException("[ERROR] No PDF document is loaded. Please load a document before embedding.");
        }

        try {
            // Step 2: Embed QR codes on specified pages
            for (Map.Entry<Integer, List<QRCodeDetails>> entry : qrData.entrySet()) {
                int pageNo = entry.getKey();
                List<QRCodeDetails> qrDetailsList = entry.getValue();

                if (pageNo < 1 || pageNo > document.getNumberOfPages()) {
                    tracker.logWarning("[WARNING] Page number " + pageNo + " is out of range. Skipping page...");
                    continue;
                }

                PDPage page = document.getPage(pageNo - 1); // Pages are 0-indexed
                tracker.logMessage("[INFO] Embedding QR codes on page " + pageNo + "...");
                embedQRDetailsOnPage(page, qrDetailsList, tracker);
            }

            // Step 3: Append QR Code Table pages at the end
            tracker.logMessage("[INFO] Appending QR Code Table pages...");
            appendQrTablePages(qrData, tracker);

            // Step 4: Save the updated PDF to the specified output path
            if (!outputPdfPath.endsWith(".pdf")) {
                outputPdfPath += ".pdf"; // Ensure output file has `.pdf` extension
            }
            savePDF(outputPdfPath);
            tracker.logMessage("[INFO] QR code embedding process completed successfully. File saved at: " + outputPdfPath);

        } catch (IOException e) {
            tracker.logError("[ERROR] Failed during QR code embedding process: " + e.getMessage());
            throw e;

        } finally {
            // Step 5: Clean up resources by closing the document
            closePDF();
            tracker.logMessage("[INFO] PDF document closed and resources released.");
        }
    }

    /**
     * Embeds QR codes on a specific page of the PDF.
     *
     * @param page           The page where QR codes will be embedded.
     * @param qrDetailsList  The list of QR code details for the page.
     * @param tracker        The progress tracker for logging progress.
     * @throws IOException   If an error occurs during QR code embedding.
     */
    /**
     * Embeds QR codes on a specific page of the PDF with improved placement and alignment.
     *
     * @param page           The page where QR codes will be embedded.
     * @param qrDetailsList  The list of QR code details for the page.
     * @param tracker        The progress tracker for logging progress.
     * @throws IOException   If an error occurs during QR code embedding.
     */
    /**
     * Embeds QR codes on a specific page of the PDF with improved placement, dynamic page handling, and optimized layout.
     *
     * @param page           The page where QR codes will be embedded.
     * @param qrDetailsList  The list of QR code details for the page.
     * @param tracker        The progress tracker for logging progress.
     * @throws IOException   If an error occurs during QR code embedding.
     */
    private void embedQRDetailsOnPage(PDPage page, List<QRCodeDetails> qrDetailsList, ProgressTracker tracker) throws IOException {
        // Margins and dimensions
        float margin = 50; // Margin from the page edges
        float qrCodeSize = 100; // Standard QR code size
        float horizontalSpacing = 20; // Horizontal spacing between QR codes
        float verticalSpacing = 30; // Vertical spacing between QR codes
        float pageWidth = page.getMediaBox().getWidth(); // Width of the PDF page
        float pageHeight = page.getMediaBox().getHeight(); // Height of the PDF page

        // Starting positions for QR code placement
        float x = margin;
        float y = pageHeight - margin;

        // Initialize content stream for the current page
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)) {

            for (QRCodeDetails qrDetails : qrDetailsList) {
                // Check horizontal space; move to the next row if exceeded
                if (x + qrCodeSize > pageWidth - margin) {
                    x = margin; // Reset x position to the left margin
                    y -= qrCodeSize + verticalSpacing; // Move down to the next row
                }

                // Check vertical space; add a new page if exceeded
                if (y - qrCodeSize < margin) {
                    tracker.logMessage("[INFO] Adding a new page due to insufficient space.");
                    contentStream.close(); // Close the current content stream

                    // Add a new page and reset starting positions
                    page = new PDPage(document.getPage(0).getMediaBox());
                    document.addPage(page);
                    contentStream.close();
                    try (PDPageContentStream newContentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)) {
                        x = margin;
                        y = pageHeight - margin;

                        // Continue processing the QR codes with a new content stream
                        continue;
                    }
                }

                // Draw a border (box) around the QR code
                contentStream.setLineWidth(0.5f); // Thickness of the border
                contentStream.addRect(x, y - qrCodeSize, qrCodeSize, qrCodeSize); // Define the rectangle dimensions
                contentStream.stroke(); // Render the border

                // Embed the QR code image within the defined box
                PDImageXObject qrImage = PDImageXObject.createFromFile(qrDetails.getQrFilePath(), document);
                contentStream.drawImage(qrImage, x, y - qrCodeSize, qrCodeSize, qrCodeSize);

                // Add a label below the QR code (e.g., the URL slug)
                String slugName = extractAndTruncateSlug(qrDetails.getLink());
                PDType0Font font = loadFont(); // Load font only once (cached)
                contentStream.beginText();
                contentStream.setFont(font, 10);
                contentStream.newLineAtOffset(x + 5, y - qrCodeSize - 15); // Position the text
                contentStream.showText("Page: " + slugName);
                contentStream.endText();


                // Move horizontally for the next QR code
                x += qrCodeSize + horizontalSpacing;
            }

            // Log success for QR code embedding
            tracker.logMessage("[INFO] QR codes successfully embedded on the page.");
        }
    }




    private String extractAndTruncateSlug(String url) {
        if (url == null || url.isEmpty()) return "Unknown";

        String[] parts = url.split("/");
        String slug = parts[parts.length - 1];
        return slug.length() > 7 ? slug.substring(0, 7) : slug;
    }


    /**
     * Appends QR Code Table pages to the end of the PDF document with better layout and spacing.
     *
     * @param qrData   A sorted mapping of page numbers to QR code details.
     * @param tracker  A progress tracker for logging progress.
     * @throws IOException If an error occurs during table appending.
     */
    /**
     * Appends QR Code Table pages to the end of the PDF document with improved layout, proper alignment, and optimized spacing.
     *
     * @param qrData   A sorted mapping of page numbers to QR code details.
     * @param tracker  A progress tracker for logging progress.
     * @throws IOException If an error occurs during table appending.
     */
    private void appendQrTablePages(TreeMap<Integer, List<QRCodeDetails>> qrData, ProgressTracker tracker) throws IOException {
        // Margins and dimensions remain the same
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
        int processedItems = 0;
        int totalItems = qrData.values().stream().mapToInt(List::size).sum();

        try {
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

                    // Draw cell and content
                    contentStream.setLineWidth(0.5f);
                    contentStream.addRect(x, y - cellHeight, cellWidth, cellHeight);
                    contentStream.stroke();

                    PDImageXObject qrImage = PDImageXObject.createFromFile(qrDetails.getQrFilePath(), document);
                    float qrCodeSize = Math.min(cellWidth - 20, cellHeight - 40);
                    float qrX = x + (cellWidth - qrCodeSize) / 2;
                    float qrY = y - cellHeight + 10;
                    contentStream.drawImage(qrImage, qrX, qrY, qrCodeSize, qrCodeSize);

                    String slugName = extractAndTruncateSlug(qrDetails.getLink());
                    PDType0Font font = loadFont();
                    contentStream.beginText();
                    contentStream.setFont(font, 10);
                    contentStream.newLineAtOffset(x + 5, y - cellHeight + 5);
                    contentStream.showText("Page: " + slugName);
                    contentStream.endText();

                    x += cellWidth;
                    columnCount++;
                    processedItems++;

                    // Trigger GC only after processing a significant number of items
                    if (processedItems % 50 == 0) {
                        System.gc();
                        tracker.logMessage("[INFO] Memory cleanup performed after processing " + processedItems + " of " + totalItems + " items");
                    }
                }
            }
        } finally {
            contentStream.close();
        }

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

    private PDType0Font cachedFont; // Caches the font instance

    /**
     * Loads the font from the resources folder or returns the cached font instance.
     *
     * @return The loaded PDType0Font.
     * @throws IOException If the font cannot be loaded.
     */
    private PDType0Font loadFont() throws IOException {
        // Check if the font is already loaded
        if (cachedFont == null) {
            System.out.println("[INFO] Loading font 'times.ttf' for the first time...");
            InputStream fontStream = getClass().getClassLoader().getResourceAsStream("fonts/times.ttf");
            if (fontStream == null) {
                throw new IOException("[ERROR] Font file 'times.ttf' not found in resources.");
            }

            // Load the font into the document
            try {
                cachedFont = PDType0Font.load(document, fontStream);
            } catch (IOException e) {
                throw new IOException("[ERROR] Failed to load font from 'times.ttf'. Ensure the font is valid and accessible.", e);
            }
        }
        return cachedFont; // Return the cached font
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
        
        try {
            loadPDF(documentPath);
            
            // Step 2: Embed QR codes on specified pages
            for (var entry : qrData.entrySet()) {
                int pageNo = entry.getKey();
                List<QRCodeDetails> qrCodeDetailsList = entry.getValue();

                if (pageNo < 1 || pageNo > document.getNumberOfPages()) {
                    tracker.logWarning("[WARNING] Page number " + pageNo + " is out of range. Skipping...");
                    continue;
                }

                PDPage page = document.getPage(pageNo - 1);
                tracker.logMessage("[INFO] Embedding QR codes on page " + pageNo + "...");
                embedQRDetailsOnPage(page, qrCodeDetailsList, tracker);
            }

            // Step 3: Append QR Code Table pages
            tracker.logMessage("[INFO] Appending QR Code Table pages...");
            appendQrTablePages(new TreeMap<>(qrData), tracker);

            // Step 4: Save the final PDF
            if (!outputPath.endsWith(".pdf")) {
                outputPath += ".pdf";
            }

            savePDF(outputPath);
            tracker.logMessage("[INFO] QR code embedding process completed successfully. File saved at: " + outputPath);

        } catch (IOException e) {
            tracker.logError("[ERROR] Failed to embed QR codes: " + e.getMessage());
            throw e;
        } finally {
            // Cleanup resources
            closePDF();
            System.gc(); // Single garbage collection at the end
            tracker.logMessage("[INFO] Resources cleaned up successfully.");
        }
    }

    @Override
    public void close() throws IOException {
        closePDF();
    }
}