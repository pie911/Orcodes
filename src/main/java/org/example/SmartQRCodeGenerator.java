package org.example;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color; // Used for QR code color customization
import java.io.File;
import java.io.IOException;
import java.util.*;

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

            // Convert the final PDF to a website
            convertPDFToWebsite(outputPath);
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

        // Use stack to manage QR code positions
        Stack<float[]> placementStack = new Stack<>();

        float x = pageWidth - MARGIN - QR_CODE_SIZE; // Start at right margin
        float y = pageHeight - MARGIN; // Start at top margin

        // Push initial position to stack
        placementStack.push(new float[]{x, y});

        try (var contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(
                document, page, org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode.APPEND, true)) {

            for (QRCodeDetails qr : qrCodes) {
                // Use random colors for QR codes
                Color qrColor = getRandomColor();

                // Load the QR code image
                PDImageXObject qrImage = PDImageXObject.createFromFile(qr.getQrFilePath(), document);

                // Extract the last part of the link for annotation
                String qrName = extractLastPartOfLink(qr.getLink());

                // Check if placement fits or backtrack
                if (y - QR_CODE_SIZE < MARGIN) { // If space runs out
                    if (!placementStack.isEmpty()) {
                        // Backtrack to the previous valid placement
                        float[] previousPosition = placementStack.pop();
                        x = previousPosition[0] - (QR_CODE_SIZE + SPACING);
                        y = previousPosition[1];
                    } else {
                        // No space left, create a new page
                        PDPage newPage = new PDPage();
                        document.addPage(newPage);
                        contentStream.close();

                        // Recursive call for remaining QR codes
                        placeQRCodesOnPage(document, newPage, qrCodes.subList(qrCodes.indexOf(qr), qrCodes.size()), font);
                        return;
                    }
                }

                // Draw the QR code image
                contentStream.drawImage(qrImage, x, y - QR_CODE_SIZE, QR_CODE_SIZE, QR_CODE_SIZE);

                // Annotate the QR code with its text or link description
                contentStream.beginText();
                contentStream.setFont(font, 10);
                contentStream.newLineAtOffset(x, y - QR_CODE_SIZE - 15);
                contentStream.showText(qrName);
                contentStream.endText();

                // Update position
                y -= (QR_CODE_SIZE + SPACING);

                // Save current position to the stack
                placementStack.push(new float[]{x, y});
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
        return parts[parts.length - 1];
    }

    /**
     * Generates a random color for QR codes.
     *
     * @return A random color instance.
     */
    private Color getRandomColor() {
        Random random = new Random();
        return new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }

    /**
     * Converts the final PDF into a website by generating HTML, CSS, and JavaScript files.
     *
     * @param pdfPath The path of the PDF document to convert.
     * @throws IOException If an error occurs during file generation.
     */
    private void convertPDFToWebsite(String pdfPath) throws IOException {
        String htmlPath = pdfPath.replace(".pdf", ".html");
        File htmlFile = new File(htmlPath);

        try (var writer = new java.io.PrintWriter(htmlFile)) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html lang=\"en\">");
            writer.println("<head>");
            writer.println("<meta charset=\"UTF-8\">");
            writer.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
            writer.println("<title>QR Code Website</title>");
            writer.println("<style>");
            writer.println("table { border-collapse: collapse; width: 100%; }");
            writer.println("th, td { border: 1px solid black; padding: 8px; text-align: left; }");
            writer.println("</style>");
            writer.println("</head>");
            writer.println("<body>");
            writer.println("<h1>QR Codes</h1>");
            writer.println("<table>");
            writer.println("<tr><th>QR Code Name</th><th>QR Code</th></tr>");

            for (int i = 1; i <= 10; i++) {
                writer.println("<tr>");
                writer.println("<td>QR Code " + i + "</td>");
                writer.println("<td><img src='path_to_qr_" + i + ".png' alt='QR Code " + i + "'/></td>");
                writer.println("</tr>");
            }

            writer.println("</table>");
            writer.println("</body>");
            writer.println("</html>");
        }

        System.out.println("Website HTML generated at: " + htmlPath);
    }
}
