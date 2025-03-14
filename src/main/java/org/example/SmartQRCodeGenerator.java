package org.example;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

public class SmartQRCodeGenerator {

    private final FolderManager folderManager = new FolderManager();
    private final List<String> colors = Arrays.asList("bg-primary", "bg-success", "bg-info", "bg-warning", "bg-danger");
    private static final int DPI = 300;

    public void convertPDFToWebsite(String pdfPath, HashMap<Integer, List<QRCodeDetails>> qrData, String websiteOutputDir) throws IOException {
        File pdfFile = new File(pdfPath);
        String fileNameWithoutExtension = pdfFile.getName().replaceFirst("[.][^.]+$", "");
        String userWebsiteDir = folderManager.createUserDirectory(websiteOutputDir, fileNameWithoutExtension);

        String qrCodesDir = folderManager.createDocumentDirectory(userWebsiteDir, "QrCodes");
        String imagesDir = folderManager.createDocumentDirectory(userWebsiteDir, "ExtractedImages");

        copyQrCodesToWebsite(qrData, qrCodesDir);
        triggerGarbageCollection("[INFO] Garbage collection triggered after copying QR codes.");

        extractImagesFromPDF(pdfPath, imagesDir);
        triggerGarbageCollection("[INFO] Garbage collection triggered after extracting images.");

        int totalPages = getTotalPages(pdfFile);

        generateIndexHTML(userWebsiteDir, totalPages);

        for (int pageNo = 1; pageNo <= totalPages; pageNo++) {
            List<QRCodeDetails> qrCodesForPage = qrData.getOrDefault(pageNo, new ArrayList<>());
            createPageHTML(userWebsiteDir, pageNo, totalPages, qrCodesForPage);
            triggerGarbageCollection("[INFO] Garbage collection triggered after creating Page_" + pageNo + ".html");
        }
    }

    private void triggerGarbageCollection(String message) {
        System.gc();
        System.out.println(message);
    }

    private int getTotalPages(File pdfFile) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            throw new IOException("[ERROR] Failed to load PDF document: " + e.getMessage());
        }
    }

    private void generateIndexHTML(String userWebsiteDir, int totalPages) throws IOException {
        File indexFile = new File(userWebsiteDir, "index.html");

        try (PrintWriter writer = new PrintWriter(indexFile)) {
            writer.println(generateHTMLHeader("PDF Page Index"));
            writer.println("<body>");
            writer.println(generateNavbar("PDF Page Index"));
            writer.println("<div class='container mt-4'>");
            writer.println("<h1 class='display-4 text-center mb-4'>PDF Page Index</h1>");
            writer.println("<div class='row'>");

            for (int pageNo = 1; pageNo <= totalPages; pageNo++) {
                writer.println("<div class='col-md-4 mb-4'>");
                writer.println("<div class='card shadow-sm'>");
                writer.println("<div class='card-body text-center'>");
                writer.println("<h5 class='card-title'>Page " + pageNo + "</h5>");
                writer.println("<a href='Page_" + pageNo + ".html' class='btn btn-primary'>View Page</a>");
                writer.println("</div>");
                writer.println("</div>");
                writer.println("</div>");
            }

            writer.println("</div>");
            writer.println("</div>");
            writer.println("</body>");
            writer.println("</html>");
        }
    }

    private void createPageHTML(String userWebsiteDir, int pageNo, int totalPages, List<QRCodeDetails> qrCodes) throws IOException {
        File pageFile = new File(userWebsiteDir, "Page_" + pageNo + ".html");
        try (PrintWriter pageWriter = new PrintWriter(pageFile)) {
            pageWriter.println(generateHTMLHeader("Page " + pageNo));
            pageWriter.println("<body>");
            pageWriter.println(generateNavbar("Page " + pageNo));
            pageWriter.println("<div class='container mt-4'>");

            String pageImagePath = "./ExtractedImages/Page_" + pageNo + ".png";
            pageWriter.println("<div class='text-center my-4'>");
            pageWriter.println("<img src='" + pageImagePath + "' class='img-fluid rounded border' alt='Page Image'>");
            pageWriter.println("</div>");

            if (!qrCodes.isEmpty()) {
                pageWriter.println("<h3 class='mt-4'>QR Codes for this page:</h3>");
                pageWriter.println("<div class='row mt-4'>");
                for (QRCodeDetails details : qrCodes) {
                    writeQRCard(pageWriter, details);
                }
                pageWriter.println("</div>");
            } else {
                pageWriter.println("<h5 class='text-muted text-center'>No QR codes available for this page.</h5>");
            }

            pageWriter.println("<div class='d-flex justify-content-between mt-4'>");
            if (pageNo > 1) {
                pageWriter.println("<a href='Page_" + (pageNo - 1) + ".html' class='btn btn-secondary'>Previous Page</a>");
            } else {
                pageWriter.println("<button class='btn btn-secondary' disabled>Previous Page</button>");
            }
            if (pageNo < totalPages) {
                pageWriter.println("<a href='Page_" + (pageNo + 1) + ".html' class='btn btn-secondary'>Next Page</a>");
            } else {
                pageWriter.println("<button class='btn btn-secondary' disabled>Next Page</button>");
            }
            pageWriter.println("</div>");

            pageWriter.println("<div class='text-center mt-3'>");
            pageWriter.println("<a href='index.html' class='btn btn-primary'>Back to Index</a>");
            pageWriter.println("</div>");

            pageWriter.println("</div>");
            pageWriter.println("</body>");
            pageWriter.println("</html>");
        }
    }

    private void copyQrCodesToWebsite(HashMap<Integer, List<QRCodeDetails>> qrData, String qrCodesDir) throws IOException {
        for (var entry : qrData.entrySet()) {
            for (QRCodeDetails details : entry.getValue()) {
                File sourceFile = new File(details.getQrFilePath());
                File destFile = new File(qrCodesDir, sourceFile.getName());

                Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[INFO] Copied QR code: " + sourceFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());

                details.setQrFilePath("./QrCodes/" + sourceFile.getName());
            }
        }
    }

    private void writeQRCard(PrintWriter writer, QRCodeDetails details) {
        writer.println("<div class='col-md-4 mb-4'>");
        writer.println("<div class='card shadow-sm'>");
        writer.println("<div class='card-body text-center'>");
        writer.println("<img src='" + details.getQrFilePath() + "' class='img-fluid' alt='QR Code'>");
        writer.println("<h5 class='card-title mt-2'>" + details.getDescription() + "</h5>");
        writer.println("</div>");
        writer.println("</div>");
        writer.println("</div>");
    }

    public void extractImagesFromPDF(String pdfPath, String extractedImagesDir) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, 300, ImageType.RGB);
                String imageName = "Page_" + (pageIndex + 1) + ".png";
                File outputImageFile = new File(extractedImagesDir, imageName);
                ImageIO.write(image, "PNG", outputImageFile);
                System.out.println("[INFO] Extracted image saved: " + outputImageFile.getAbsolutePath());
            }
        }
    }

    private String generateNavbar(String title) {
        return "<nav class='navbar navbar-expand-lg navbar-light bg-light'>\n" +
               "  <a class='navbar-brand' href='#'>" + title + "</a>\n" +
               "  <button class='navbar-toggler' type='button' data-toggle='collapse' data-target='#navbarNav' aria-controls='navbarNav' aria-expanded='false' aria-label='Toggle navigation'>\n" +
               "    <span class='navbar-toggler-icon'></span>\n" +
               "  </button>\n" +
               "  <div class='collapse navbar-collapse' id='navbarNav'>\n" +
               "    <ul class='navbar-nav'>\n" +
               "      <li class='nav-item active'>\n" +
               "        <a class='nav-link' href='index.html'>Home <span class='sr-only'>(current)</span></a>\n" +
               "      </li>\n" +
               "    </ul>\n" +
               "  </div>\n" +
               "</nav>";
    }

    private String generateHTMLHeader(String title) {
        return "<!DOCTYPE html>\n" +
               "<html lang='en'>\n" +
               "<head>\n" +
               "<meta charset='UTF-8'>\n" +
               "<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
               "<title>" + title + "</title>\n" +
               "<link rel='stylesheet' href='https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css'>\n" +
               "</head>";
    }
}
