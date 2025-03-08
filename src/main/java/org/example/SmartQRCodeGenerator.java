package org.example;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class SmartQRCodeGenerator {

    private final int qrCodeSize = 100; // QR Code size in pixels (width and height)
    private final int margin = 50; // Margin from edges
    private final int spacing = 20; // Spacing between QR codes

    /**
     * Dynamically places QR codes into the PDF using grid-based allocation with reinforcement-inspired logic.
     *
     * @param pdfPath     The original PDF file.
     * @param qrData      A mapping of page numbers to QR codes.
     * @param outputPath  The updated PDF file path.
     * @throws IOException If an error occurs while processing the PDF.
     */
    public void placeSmartQRCodes(String pdfPath, HashMap<Integer, List<QRCodeDetails>> qrData, String outputPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            for (var entry : qrData.entrySet()) {
                int pageNo = entry.getKey();
                List<QRCodeDetails> detailsList = entry.getValue();

                PDPage page;
                if (pageNo - 1 < document.getNumberOfPages()) {
                    page = document.getPage(pageNo - 1);
                } else {
                    page = new PDPage();
                    document.addPage(page);
                }

                // Start placing QR codes on the page
                placeQRCodesOnPage(document, page, detailsList);
            }

            // Save the updated PDF
            document.save(outputPath);
            System.out.println("Smart QR Code placement completed. File saved to: " + outputPath);
        }
    }

    /**
     * Places QR codes on the page using a grid-based allocation strategy.
     *
     * @param document    The PDF document.
     * @param page        The current PDF page.
     * @param qrCodes     The list of QR code details for this page.
     * @throws IOException If an error occurs during placement.
     */
    private void placeQRCodesOnPage(PDDocument document, PDPage page, List<QRCodeDetails> qrCodes) throws IOException {
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float x = margin, y = pageHeight - margin; // Initial position for QR code placement

        try (var contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page, org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode.APPEND, true)) {
            for (QRCodeDetails qr : qrCodes) {
                // Check if we need to move to the next row
                if (x + qrCodeSize > pageWidth - margin) {
                    x = margin; // Reset to left margin
                    y -= (qrCodeSize + spacing); // Move down one row
                }

                // If insufficient space on the page, append a new page
                if (y - qrCodeSize < margin) {
                    y = pageHeight - margin; // Reset Y position for new page
                    PDPage newPage = new PDPage();
                    document.addPage(newPage);
                    contentStream.close();
                    placeQRCodesOnPage(document, newPage, qrCodes.subList(qrCodes.indexOf(qr), qrCodes.size()));
                    return;
                }

                // Draw the QR code
                PDImageXObject qrImage = PDImageXObject.createFromFile(qr.getQrCodePath(), document);
                contentStream.drawImage(qrImage, x, y - qrCodeSize, qrCodeSize, qrCodeSize);

                // Move to the next column
                x += (qrCodeSize + spacing);
            }
        }
    }

    /**
     * Simulated reward mechanism for evaluating QR code placement.
     * Higher rewards mean better placement (e.g., minimal interference, alignment).
     *
     * @param x           The X-coordinate of the placement.
     * @param y           The Y-coordinate of the placement.
     * @param pageWidth   The width of the PDF page.
     * @param pageHeight  The height of the PDF page.
     * @return A reward value for the placement (higher is better).
     */
    private double evaluatePlacementReward(float x, float y, float pageWidth, float pageHeight) {
        // Reward placements that are within bounds and have sufficient spacing
        boolean withinBounds = (x + qrCodeSize <= pageWidth - margin) && (y - qrCodeSize >= margin);
        double reward = withinBounds ? 1.0 : -1.0; // Penalize out-of-bounds placements

        // Add bonus rewards for centering or avoiding edges
        reward += (x > margin && x < pageWidth / 2) ? 0.5 : 0.0; // Prefer left-center
        reward += (y < pageHeight - margin && y > margin) ? 0.5 : 0.0; // Prefer vertical centering

        return reward;
    }

    /**
     * Simulated learning loop for reinforcement-inspired QR code placement.
     */
    public void simulateReinforcementLearning() {
        // Placeholder for future training logic
        System.out.println("Reinforcement learning simulation for QR code placement is not yet implemented.");
    }
}
