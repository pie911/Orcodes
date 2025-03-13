package org.example;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

public class SmartQRCodeGenerator {

    private final FolderManager folderManager = new FolderManager();
    private final List<String> colors = Arrays.asList("bg-primary", "bg-success", "bg-info", "bg-warning", "bg-danger");
    private static final int DPI = 300;

    /**
     * Converts the updated PDF content into a responsive website with QR codes, extracted text, and headings.
     * Converts the updated PDF content into a responsive website with QR codes, extracted text, and headings.
     */
    public void convertPDFToWebsite(String pdfPath, HashMap<Integer, List<QRCodeDetails>> qrData, String websiteOutputDir) throws IOException {
        File pdfFile = new File(pdfPath);
        String fileNameWithoutExtension = pdfFile.getName().replaceFirst("[.][^.]+$", "");
        String userWebsiteDir = folderManager.createUserDirectory(websiteOutputDir, fileNameWithoutExtension);
        
        String qrCodesDir = folderManager.createDocumentDirectory(userWebsiteDir, "QrCodes");
        String imagesDir = folderManager.createDocumentDirectory(userWebsiteDir, "ExtractedImages");

        // Extract all images first
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper textStripper = new PDFTextStripper();
            extractImagesFromPDF(document, imagesDir);
            int totalPages = document.getNumberOfPages();
            
            // Copy QR codes if they exist
            if (!qrData.isEmpty()) {
                copyQrCodesToWebsite(qrData, qrCodesDir);
            }

            // Create index.html with grid layout
            createIndexHTML(userWebsiteDir, totalPages, fileNameWithoutExtension, qrData);

            // Create individual pages
            createAllPages(userWebsiteDir, document, textStripper, qrData, totalPages);
        }
    }

    private void createIndexHTML(String userWebsiteDir, int totalPages, String title, HashMap<Integer, List<QRCodeDetails>> qrData) throws IOException {
        File indexFile = new File(userWebsiteDir, "index.html");
        try (PrintWriter writer = new PrintWriter(indexFile)) {
            writer.println(generateHTMLHeader(title));
            writer.println("<body>");
            writer.println(generateNavbar(title));
            writer.println("<div class='container mt-4'>");
            writer.println("<h1 class='display-4 text-center mb-4'>" + title + "</h1>");
            
            // Create grid layout
            writer.println("<div class='row row-cols-1 row-cols-md-3 g-4 mb-4'>");
            
            for (int pageNo = 1; pageNo <= totalPages; pageNo++) {
                String cardClass = qrData.containsKey(pageNo) ? "border-primary" : "border-secondary";
                writer.println("<div class='col'>");
                writer.println("  <div class='card h-100 " + cardClass + "'>");
                writer.println("    <div class='card-body text-center'>");
                writer.println("      <h5 class='card-title'>Page " + pageNo + "</h5>");
                writer.println("      <a href='Page_" + pageNo + ".html' class='btn btn-primary'>View Page</a>");
                writer.println("    </div>");
                writer.println("  </div>");
                writer.println("</div>");
            }
            
            writer.println("</div>");
            writer.println("</div>");
            writer.println("</body></html>");
        }
    }

    private void createAllPages(String userWebsiteDir, PDDocument document, PDFTextStripper textStripper,
                              HashMap<Integer, List<QRCodeDetails>> qrData, int totalPages) throws IOException {
        for (int pageNo = 1; pageNo <= totalPages; pageNo++) {
            // Extract page text
            textStripper.setStartPage(pageNo);
            textStripper.setEndPage(pageNo);
            String pageText = textStripper.getText(document);
            
            createPageHTML(userWebsiteDir, pageNo, totalPages, qrData.get(pageNo), pageText);
        }
    }

    private void createPageHTML(String userWebsiteDir, int pageNo, int totalPages, 
                              List<QRCodeDetails> pageQRCodes, String pageText) throws IOException {
        File pageFile = new File(userWebsiteDir, "Page_" + pageNo + ".html");
        try (PrintWriter writer = new PrintWriter(pageFile)) {
            writer.println(generateHTMLHeader("Page " + pageNo));
            writer.println("<body>");
            writer.println(generateNavbar("Page " + pageNo));
            writer.println("<div class='container mt-4'>");
            
            // Page heading
            writer.println("<h1 class='text-center mb-4'>Page " + pageNo + "</h1>");
            
            // Page content
            writer.println("<div class='row'>");
            
            // Left column: Page image
            writer.println("<div class='col-md-8'>");
            writer.println("  <div class='card mb-4 shadow'>");
            writer.println("    <div class='card-body p-0'>");
            writer.println("      <img src='./ExtractedImages/Page_" + pageNo + ".png' " +
                         "class='img-fluid rounded' alt='Page " + pageNo + "'>");
            writer.println("    </div>");
            writer.println("  </div>");
            
            // Page text
            writer.println("  <div class='card mb-4'>");
            writer.println("    <div class='card-body'>");
            writer.println("      <p class='text-justify'>" + pageText.replace("\n", "<br>") + "</p>");
            writer.println("    </div>");
            writer.println("  </div>");
            writer.println("</div>");
            
            // Right column: QR Codes
            writer.println("<div class='col-md-4'>");
            if (pageQRCodes != null && !pageQRCodes.isEmpty()) {
                writer.println("  <h3 class='text-center mb-3'>QR Codes</h3>");
                for (QRCodeDetails details : pageQRCodes) {
                    writeQRCard(writer, details);
                }
            }
            writer.println("</div>");
            writer.println("</div>"); // Close row
            
            // Navigation
            writeNavigation(writer, pageNo, totalPages);
            
            writer.println("</div>"); // Close container
            writer.println("<script src='https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js'></script>");
            writer.println("</body></html>");
        }
    }

    private void extractImagesFromPDF(PDDocument document, String extractedImagesDir) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);
        for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, DPI, ImageType.RGB); // Using DPI constant
            String imageName = "Page_" + (pageIndex + 1) + ".png";
            File outputImageFile = new File(extractedImagesDir, imageName);
            ImageIO.write(image, "PNG", outputImageFile);
            System.out.println("[INFO] Extracted page " + (pageIndex + 1) + " as image");
        }
    }

    private void copyQrCodesToWebsite(HashMap<Integer, List<QRCodeDetails>> qrData, String qrCodesDir) throws IOException {
        for (var entry : qrData.entrySet()) {
            for (QRCodeDetails details : entry.getValue()) {
                File sourceFile = new File(details.getQrFilePath());
                File destFile = new File(qrCodesDir, sourceFile.getName());

                // Copy the file
                Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[INFO] Copied QR code: " + sourceFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());

                // Update QRCodeDetails path to the relative one in the website directory
                details.setQrFilePath("./QrCodes/" + sourceFile.getName());
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
        writer.println("<div class='card shadow-sm' style='height: 100%;'>");
        writer.println("<div class='card-body text-center'>");

        // Add QR code image with responsive styling
        writer.println("<img src='" + details.getQrFilePath() + "' class='img-fluid rounded' alt='QR Code' style='max-height: 150px; max-width: 150px;'>");

        // Add QR code details
        writer.println("<h5 class='card-title mt-2'>" + details.getText() + "</h5>");
        writer.println("<a href='" + details.getLink() + "' class='btn btn-primary mt-2'>Visit Link</a>");

        writer.println("</div>"); // Close card-body
        writer.println("</div>"); // Close card
        writer.println("</div>"); // Close column
    }

    private void writeNavigation(PrintWriter writer, int pageNo, int totalPages) {
        writer.println("<div class='d-flex justify-content-between mt-4'>");
        if (pageNo > 1) {
            writer.println("<a href='Page_" + (pageNo - 1) + ".html' class='btn btn-primary'>" +
                         "<i class='bi bi-arrow-left'></i> Previous</a>");
        } else {
            writer.println("<button class='btn btn-primary' disabled>" +
                         "<i class='bi bi-arrow-left'></i> Previous</button>");
        }
        
        writer.println("<a href='index.html' class='btn btn-secondary'>Index</a>");
        
        if (pageNo < totalPages) {
            writer.println("<a href='Page_" + (pageNo + 1) + ".html' class='btn btn-primary'>" +
                         "Next <i class='bi bi-arrow-right'></i></a>");
        } else {
            writer.println("<button class='btn btn-primary' disabled>" +
                         "Next <i class='bi bi-arrow-right'></i></button>");
        }
        writer.println("</div>");
    }

    public List<String> getColors() {
        return colors;
    }
}