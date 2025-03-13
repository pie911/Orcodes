package org.example;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class SmartQRCodeGenerator {

    private final FolderManager folderManager = new FolderManager();
    private final List<String> colors = Arrays.asList("bg-primary", "bg-success", "bg-info", "bg-warning", "bg-danger");

    /**
     * Converts the updated PDF content into a responsive website with QR codes, extracted text, and headings.
     */
    public void convertPDFToWebsite(String pdfPath, HashMap<Integer, List<QRCodeDetails>> qrData,
                                    String websiteOutputDir) throws IOException {
        // Create website directory
        String userWebsiteDir = folderManager.createUserDirectory(websiteOutputDir, "Website");

        // Create subdirectories for QR codes and extracted images
        String qrCodesDir = folderManager.createDocumentDirectory(userWebsiteDir, "QrCodes");
        String imagesDir = folderManager.createDocumentDirectory(userWebsiteDir, "ExtractedImages");

        // Create the main index.html file
        File indexFile = new File(userWebsiteDir, "index.html");
        try (PrintWriter writer = new PrintWriter(indexFile);
             PDDocument document = Loader.loadPDF(new File(pdfPath))) {

            writer.println(generateHTMLHeader("QR Code Website"));
            writer.println("<body>");
            writer.println(generateNavbar("QR Code Website"));
            writer.println("<div class='container mt-4'>");
            writer.println("<h1 class='display-4 text-center mb-4'>QR Code Index</h1>");

            // Create a table layout for page navigation
            writer.println("<table class='table table-bordered table-striped text-center'>");
            writer.println("<thead class='table-dark'><tr>");
            writer.println("<th>Page No</th><th>Page No</th><th>Page No</th><th>Page No</th><th>Page No</th>");
            writer.println("</tr></thead>");
            writer.println("<tbody>");

            // Sort page numbers in ascending order
            List<Integer> sortedPageNumbers = new ArrayList<>(qrData.keySet());
            Collections.sort(sortedPageNumbers);

            int columnCounter = 0;
            for (int i = 0; i < sortedPageNumbers.size(); i++) {
                // Start a new row every 5 columns
                if (columnCounter == 0) {
                    writer.println("<tr>");
                }

                // Assign a unique color from the list in a repeating pattern
                String colorClass = colors.get(i % colors.size());
                int pageNo = sortedPageNumbers.get(i);
                writer.println("<td class='" + colorClass + "'><a class='text-white' href='Page_" + pageNo + ".html'>Page " + pageNo + "</a></td>");

                columnCounter++;
                if (columnCounter == 5) { // End the row after 5 columns
                    writer.println("</tr>");
                    columnCounter = 0;
                }
            }

            // Close the last row if it's incomplete
            if (columnCounter != 0) {
                writer.println("</tr>");
            }

            writer.println("</tbody>");
            writer.println("</table>");
            writer.println("</div>");
            writer.println("</body>");
            writer.println("</html>");

            // Generate individual page files
            PDFTextStripper textStripper = new PDFTextStripper();
            for (var entry : qrData.entrySet()) {
                createPageHTML(userWebsiteDir, document, textStripper, entry, entry.getKey());
            }
        }
    }

    /**
     * Helper method to create an individual page HTML file.
     */
    private void createPageHTML(String userWebsiteDir, PDDocument document, PDFTextStripper textStripper,
                                Map.Entry<Integer, List<QRCodeDetails>> entry, int pageNo) throws IOException {
        File pageFile = new File(userWebsiteDir, "Page_" + pageNo + ".html");
        try (PrintWriter pageWriter = new PrintWriter(pageFile)) {
            pageWriter.println(generateHTMLHeader("Page " + pageNo));
            pageWriter.println("<body>");
            pageWriter.println(generateNavbar("Page " + pageNo));
            pageWriter.println("<div class='container mt-4'>");

            // Extract and display page text
            textStripper.setStartPage(pageNo);
            textStripper.setEndPage(pageNo);
            String pageText = textStripper.getText(document);

            pageWriter.println("<h1 class='mb-4 text-primary text-center'>Page " + pageNo + "</h1>");
            pageWriter.println("<p class='lead text-justify'>" + pageText.replace("\n", "<br>") + "</p>");

            // Display extracted images if present
            String pageImagePath = "./ExtractedImages/Page_" + pageNo + ".png";
            File imageFile = new File(pageImagePath);
            if (imageFile.exists()) {
                pageWriter.println("<div class='text-center my-4'>");
                pageWriter.println("<img src='" + pageImagePath + "' class='img-fluid rounded' alt='Page Image'>");
                pageWriter.println("</div>");
            }

            // Display QR codes
            pageWriter.println("<div class='row mt-4'>");
            for (QRCodeDetails details : entry.getValue()) {
                writeQRCard(pageWriter, details);
            }
            pageWriter.println("</div>");

            pageWriter.println("</div>");
            pageWriter.println("<a href='index.html' class='btn btn-secondary mt-3'>Back to Index</a>");
            pageWriter.println("</body>");
            pageWriter.println("</html>");
        }
    }

    /**
     * Extracts all images from the PDF and saves them to the output directory.
     */
    public void extractImagesFromPDF(String pdfPath, String extractedImagesDir) throws IOException {
        String imagesDir = folderManager.createDocumentDirectory(extractedImagesDir, "ExtractedImages");

        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, 300, ImageType.RGB);
                String imageName = "Page_" + (pageIndex + 1) + ".png";
                File outputImageFile = new File(imagesDir, imageName);
                ImageIO.write(image, "PNG", outputImageFile);
                System.out.println("[INFO] Extracted image saved at: " + outputImageFile.getAbsolutePath());
            }
        }
    }

    /**
     * Helper method to generate an HTML header.
     */
    private String generateHTMLHeader(String title) {
        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.0-alpha3/dist/css/bootstrap.min.css'>" +
                "<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap-icons/font/bootstrap-icons.css'>" +
                "<title>" + title + "</title></head>";
    }

    /**
     * Helper method to generate a navigation bar.
     */
    private String generateNavbar(String brandName) {
        return "<nav class='navbar navbar-expand-lg navbar-dark bg-dark'>" +
                "<div class='container-fluid'>" +
                "<a class='navbar-brand' href='index.html'>" + brandName + "</a>" +
                "<button class='navbar-toggler' type='button' data-bs-toggle='collapse' data-bs-target='#navbarNav' aria-controls='navbarNav' aria-expanded='false' aria-label='Toggle navigation'>" +
                "<span class='navbar-toggler-icon'></span></button>" +
                "<div class='collapse navbar-collapse' id='navbarNav'>" +
                "<ul class='navbar-nav'>" +
                "<li class='nav-item'><a class='nav-link active' href='index.html'>Home</a></li>" +
                "</ul></div></div></nav>";
    }

    /**
     * Helper method to write a QR code card HTML snippet.
     */
    private void writeQRCard(PrintWriter writer, QRCodeDetails details) {
        writer.println("<div class='col-md-4 mb-4'>");
        writer.println("<div class='card shadow-lg'>");
        writer.println("<img src='./QrCodes/" + details.getQrFilePath() + "' class='card-img-top' alt='QR Code'>");
        writer.println("<div class='card-body'>");
        writer.println("<h5 class='card-title'>" + details.getText() + "</h5>");
        writer.println("<a href='" + details.getLink() + "' class='btn btn-primary'>");
        writer.println("<i class='bi bi-arrow-right-circle'></i> Visit Link</a>");
        writer.println("</div>"); // Close card-body
        writer.println("</div>"); // Close card
        writer.println("</div>"); // Close column
    }
}