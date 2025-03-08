package org.example;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

public class QRCodeGenerator {

    /**
     * Generates QR codes for each link extracted from the document and saves them in appropriate folders.
     * Dynamically handles QR code resizing based on user inputs or machine learning algorithms.
     *
     * @param pageLinks  A mapping of page numbers to a list of links.
     * @param baseDir    The base directory where QR codes should be saved.
     * @param width      The width of the QR code in pixels.
     * @param height     The height of the QR code in pixels.
     * @return A mapping of page numbers to a list of QRCodeDetails for further processing.
     * @throws IOException If there is an error creating directories or writing files.
     */
    public HashMap<Integer, List<QRCodeDetails>> generateQRCodes(
            HashMap<Integer, List<String>> pageLinks, String baseDir, int width, int height) throws IOException {

        HashMap<Integer, List<QRCodeDetails>> qrCodeDetailsMap = new HashMap<>();

        for (var entry : pageLinks.entrySet()) {
            int pageNo = entry.getKey();
            List<String> links = entry.getValue();

            // Ensure each page has its own directory for QR codes
            String pageDir = baseDir + "/Page_" + pageNo;
            if (!Files.exists(Path.of(pageDir))) {
                Files.createDirectories(Path.of(pageDir));
            }

            System.out.println("Generating QR codes for page " + pageNo + "...");
            for (String link : links) {
                // Sanitize the link to generate a valid QR code file name
                String qrFileName = Utils.sanitizeFileName(link) + ".png";
                String qrFilePath = pageDir + "/" + qrFileName;

                // Generate the QR code and save it to the specified path
                try {
                    generateQRCodeImage(link, width, height, qrFilePath);
                    System.out.println("QR code generated and saved at: " + qrFilePath);
                } catch (WriterException e) {
                    System.err.println("Error generating QR code for link: " + link + " - " + e.getMessage());
                    continue; // Skip this link and proceed with the next one
                } catch (IOException e) {
                    System.err.println("Error saving QR code for link: " + link + " - " + e.getMessage());
                    continue; // Skip this link and proceed with the next one
                }

                // Save details of the generated QR code
                QRCodeDetails qrCodeDetails = new QRCodeDetails(pageNo, link, qrFilePath, "");
                qrCodeDetails.setText(link); // Optionally set text for display in other files
                qrCodeDetailsMap.computeIfAbsent(pageNo, k -> new java.util.ArrayList<>()).add(qrCodeDetails);
            }
        }

        System.out.println("QR code generation completed.");
        return qrCodeDetailsMap;
    }

    /**
     * Generates a QR code image for the given text and saves it to the specified path.
     *
     * @param text     The text or link to encode in the QR code.
     * @param width    The width of the QR code in pixels.
     * @param height   The height of the QR code in pixels.
     * @param filePath The path where the QR code image should be saved.
     * @throws WriterException If an error occurs during QR code generation.
     * @throws IOException     If an error occurs while saving the QR code image.
     */
    private void generateQRCodeImage(String text, int width, int height, String filePath) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        // Create parent directories if they don't exist
        Path parentPath = FileSystems.getDefault().getPath(filePath).getParent();
        if (!Files.exists(parentPath)) {
            Files.createDirectories(parentPath);
        }

        // Generate the QR code as a BitMatrix
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        // Save the QR code image as a PNG file
        Path outputPath = FileSystems.getDefault().getPath(filePath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", outputPath);
    }
}
