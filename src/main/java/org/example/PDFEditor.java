package org.example;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class PDFEditor {

    /**
     * Embeds QR codes into the original PDF and saves the updated file.
     * Handles QR code placement dynamically by adding new pages as required.
     *
     * @param originalPdfPath The path to the original PDF file.
     * @param qrData          A mapping of page numbers to QR code details.
     * @param outputPdfPath   The path to save the final PDF with QR codes embedded.
     * @param tracker         The progress tracker for logging progress.
     * @throws IOException If there are errors reading or writing the PDF file.
     */
    public void embedQRCodes(String originalPdfPath, HashMap<Integer, List<QRCodeDetails>> qrData, String outputPdfPath, ProgressTracker tracker) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(originalPdfPath))) {
            tracker.logMessage("[INFO] Starting QR code embedding process...");

            for (var entry : qrData.entrySet()) {
                int pageNo = entry.getKey();
                List<QRCodeDetails> qrCodeDetailsList = entry.getValue();

                if (pageNo > document.getNumberOfPages() || pageNo < 1) {
                    tracker.logError("[WARNING] Page number " + pageNo + " is out of range. Skipping...");
                    continue;
                }

                PDPage page = document.getPage(pageNo - 1); // Pages are 0-indexed in PDFBox
                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();

                // Set margins and dimensions
                float margin = 50;
                float qrCodeSize = 100; // QR code width and height
                float horizontalSpacing = 20; // Space between QR codes
                float verticalSpacing = 30;

                float x = margin; // Start placing from the left margin
                float y = pageHeight - margin; // Start placing from the top margin

                PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);

                for (QRCodeDetails qrDetails : qrCodeDetailsList) {
                    String qrFilePath = qrDetails.getQrFilePath();

                    if (qrFilePath == null || qrFilePath.isEmpty()) {
                        tracker.logError("[ERROR] QR code file path is missing for page " + pageNo + ". Skipping...");
                        continue;
                    }

                    PDImageXObject qrImage = PDImageXObject.createFromFile(qrFilePath, document);

                    // Check if QR code fits horizontally; if not, move to the next row
                    if (x + qrCodeSize > pageWidth - margin) {
                        x = margin; // Reset X to the left margin
                        y -= qrCodeSize + verticalSpacing; // Move down to the next row
                    }

                    // Check if there's enough space vertically; if not, create a new page
                    if (y - qrCodeSize < margin) {
                        tracker.logMessage("[INFO] Adding a new page due to insufficient space.");
                        contentStream.close(); // Close the current content stream
                        page = new PDPage(document.getPage(0).getMediaBox()); // Use the same page size as the original
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true); // Open a new content stream
                        x = margin;
                        y = pageHeight - margin; // Reset starting position for the new page
                    }

                    // Embed the QR code
                    contentStream.drawImage(qrImage, x, y - qrCodeSize, qrCodeSize, qrCodeSize);
                    tracker.logMessage(String.format("[INFO] QR code embedded at (%.2f, %.2f) on page %d.", x, y - qrCodeSize, pageNo));

                    x += qrCodeSize + horizontalSpacing; // Move to the next position horizontally
                }

                contentStream.close(); // Close the content stream for the current page
            }

            // Save the updated document to the specified output path
            File outputFile = new File(outputPdfPath.endsWith(".pdf") ? outputPdfPath : outputPdfPath + ".pdf");
            document.save(outputFile);
            tracker.logMessage("[INFO] QR code embedding process completed successfully. File saved at: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            tracker.logError("[ERROR] An error occurred while embedding QR codes: " + e.getMessage());
            throw e;
        }
    }
}
