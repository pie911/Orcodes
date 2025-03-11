package org.example;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class SmartQRCodeGenerator {

    private static final int QR_CODE_SIZE = 100; // QR Code size in pixels
    private static final int MARGIN = 50; // Margin from page edges
    private static final int SPACING = 20; // Spacing between QR codes

    /**
     * Dynamically places QR codes into the PDF using intelligent logic.
     *
     * @param pdfPath    Path to the original PDF document.
     * @param qrData     Mapping of page numbers to QR codes.
     * @param outputPath Path to save the updated PDF with QR codes embedded.
     * @throws IOException If an error occurs while processing the PDF.
     */
    public void placeSmartQRCodes(String pdfPath, HashMap<Integer, List<QRCodeDetails>> qrData, String outputPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {

            // Load italic font
            PDType0Font italicFont = PDType0Font.load(document, new File("src/main/resources/fonts/timesi.ttf"));

            for (var entry : qrData.entrySet()) {
                int pageNo = entry.getKey();
                List<QRCodeDetails> detailsList = entry.getValue();

                PDPage page = (pageNo - 1 < document.getNumberOfPages()) ?
                        document.getPage(pageNo - 1) : new PDPage();

                // Add new page if needed
                if (pageNo > document.getNumberOfPages()) {
                    document.addPage(page);
                }

                // Analyze and place QR codes on the page
                placeQRCodesOnPage(document, page, detailsList, italicFont);
            }

            // Save the updated PDF
            document.save(outputPath);
            System.out.println("Smart QR Code placement completed. File saved at: " + outputPath);
        }
    }

    /**
     * Places QR codes on the specified page with layout-aware positioning.
     *
     * @param document   The PDF document.
     * @param page       The current PDF page.
     * @param qrCodes    The list of QR code details for placement.
     * @param font       The font to use for annotations.
     * @throws IOException If an error occurs during QR code placement.
     */
    private void placeQRCodesOnPage(PDDocument document, PDPage page, List<QRCodeDetails> qrCodes, PDType0Font font) throws IOException {
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();

        float x = pageWidth - MARGIN - QR_CODE_SIZE; // Start at right margin
        float y = pageHeight - MARGIN; // Start at top margin

        try (var contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(
                document, page, org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode.APPEND, true)) {

            for (QRCodeDetails qr : qrCodes) {
                PDImageXObject qrImage = PDImageXObject.createFromFile(qr.getQrFilePath(), document);

                // Extract the last part of the link for annotation
                String qrName = extractLastPartOfLink(qr.getLink());

                // Check if QR code fits vertically; otherwise, move to the next page
                if (y - QR_CODE_SIZE < MARGIN) {
                    y = pageHeight - MARGIN; // Reset Y position
                    x -= (QR_CODE_SIZE + SPACING); // Shift left if the right side is full
                    if (x < MARGIN) { // If no space left, add a new page
                        PDPage newPage = new PDPage();
                        document.addPage(newPage);
                        contentStream.close();

                        // Recursive call to place remaining QR codes on the new page
                        placeQRCodesOnPage(document, newPage, qrCodes.subList(qrCodes.indexOf(qr), qrCodes.size()), font);
                        return;
                    }
                }

                // Draw the QR code image
                contentStream.drawImage(qrImage, x, y - QR_CODE_SIZE, QR_CODE_SIZE, QR_CODE_SIZE);

                // Annotate the QR code with its text or link description
                contentStream.beginText();
                contentStream.setFont(font, 10); // Use italic font for annotations
                contentStream.newLineAtOffset(x, y - QR_CODE_SIZE - 15); // Position text below the QR code
                contentStream.showText(qrName);
                contentStream.endText();

                y -= (QR_CODE_SIZE + SPACING); // Move down for the next QR code
            }
        }
    }

    /**
     * Extracts the last part of a URL or string (e.g., after the last "/" or "#").
     *
     * @param link The full URL or string.
     * @return The last part of the link.
     */
    private String extractLastPartOfLink(String link) {
        if (link == null || link.isEmpty()) return "Unknown";
        String[] parts = link.split("[/#]");
        return parts[parts.length - 1]; // Return the last part
    }
}
