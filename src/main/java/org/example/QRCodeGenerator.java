package org.example;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QRCodeGenerator {
    private static final String PNG_FORMAT = "PNG";
    private static final String PAGE_PREFIX = "Page_";
    private static final QRCodeWriter qrCodeWriter = new QRCodeWriter();

    /**
     * Generates QR codes for each unique link extracted from the document and saves them in appropriate folders.
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
        Set<String> processedLinks = new HashSet<>(); // To avoid duplicate QR code generation

        for (var entry : pageLinks.entrySet()) {
            int pageNo = entry.getKey();
            List<String> links = entry.getValue();

            // Ensure each page has its own directory for QR codes
            String pageDir = baseDir + "/" + PAGE_PREFIX + pageNo;
            createDirectory(pageDir);

            System.out.println("Generating QR codes for page " + pageNo + "...");
            
            List<QRCodeDetails> pageDetails = links.stream()
                .filter(processedLinks::add) // Returns true if link was added (not duplicate)
                .map(link -> generateQRCodeForLink(link, pageDir, pageNo, width, height))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

            if (!pageDetails.isEmpty()) {
                qrCodeDetailsMap.put(pageNo, pageDetails);
            }
        }

        System.out.println("[INFO] QR code generation completed.");
        return qrCodeDetailsMap;
    }

    private Optional<QRCodeDetails> generateQRCodeForLink(String link, String pageDir, int pageNo, int width, int height) {
        try {
            String qrFileName = Utils.sanitizeFileName(link) + ".png";
            String qrFilePath = pageDir + "/" + qrFileName;

            generateQRCodeImage(link, width, height, qrFilePath);
            System.out.println("[INFO] QR code generated and saved at: " + qrFilePath);

            QRCodeDetails details = new QRCodeDetails(pageNo, link, qrFilePath, link);
            return Optional.of(details);

        } catch (WriterException e) {
            System.err.println("[ERROR] Error generating QR code for link: " + link + " - " + e.getMessage());
        } catch (IOException e) {
            System.err.println("[ERROR] Error saving QR code for link: " + link + " - " + e.getMessage());
        }
        return Optional.empty();
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
        // Create parent directories if they don't exist
        Path parentPath = FileSystems.getDefault().getPath(filePath).getParent();
        createDirectory(parentPath.toString());

        // Generate the QR code as a BitMatrix
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        // Save the QR code image as a PNG file
        Path outputPath = FileSystems.getDefault().getPath(filePath);
        MatrixToImageWriter.writeToPath(bitMatrix, PNG_FORMAT, outputPath);
    }

    /**
     * Creates a directory if it does not exist.
     *
     * @param dirPath The directory path to create.
     * @throws IOException If there is an error creating the directory.
     */
    private void createDirectory(String dirPath) throws IOException {
        Path dir = Path.of(dirPath);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
            System.out.println("[INFO] Directory created: " + dirPath);
        }
    }
}
