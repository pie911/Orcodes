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
     *
     * @param originalPdfPath The path to the original PDF file.
     * @param qrData          A mapping of page numbers to QR code details.
     * @param outputPdfPath   The path to save the final PDF with QR codes embedded.
     * @throws IOException If there are errors reading or writing the PDF file.
     */
    public void embedQRCodes(String originalPdfPath, HashMap<Integer, List<QRCodeDetails>> qrData, String outputPdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(originalPdfPath))) {
            System.out.println("Starting QR code embedding process...");

            for (var entry : qrData.entrySet()) {
                int pageNo = entry.getKey();
                List<QRCodeDetails> qrCodeDetailsList = entry.getValue();

                if (pageNo > document.getNumberOfPages() || pageNo < 1) {
                    System.err.println("Warning: Page number " + pageNo + " is out of range. Skipping...");
                    continue;
                }

                // Load the page where QR codes should be embedded
                PDPage page = document.getPage(pageNo - 1); // Pages are 0-indexed in PDFBox
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)) {
                    float pageWidth = page.getMediaBox().getWidth();
                    float pageHeight = page.getMediaBox().getHeight();

                    float x = 50; // Initial X-coordinate
                    float y = pageHeight - 50; // Initial Y-coordinate (starting from the top)

                    for (QRCodeDetails qrDetails : qrCodeDetailsList) {
                        // Load the QR code image
                        PDImageXObject qrImage = PDImageXObject.createFromFile(qrDetails.getQrCodePath(), document);

                        // Check if QR code fits the page dimensions; adjust placement if necessary
                        if (y - 120 < 0) { // If QR code would go below the bottom margin
                            x += 150; // Move to a new column
                            y = pageHeight - 50; // Reset Y-coordinate
                        }
                        if (x + 100 > pageWidth) { // If QR codes exceed page width
                            System.err.println("Error: Not enough space to embed all QR codes on page " + pageNo);
                            break;
                        }

                        // Embed the QR code at the calculated position
                        contentStream.drawImage(qrImage, x, y, 100, 100); // Default size: 100x100
                        System.out.println("QR code embedded at (" + x + ", " + y + ") on page " + pageNo);

                        y -= 120; // Adjust Y-coordinate for spacing between QR codes
                    }
                }
            }

            // Save the updated document to the specified output path
            document.save(outputPdfPath);
            System.out.println("QR code embedding process completed. File saved at: " + outputPdfPath);
        } catch (IOException e) {
            System.err.println("An error occurred while embedding QR codes: " + e.getMessage());
            throw e;
        }
    }
}
