package org.example;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.fasterxml.jackson.databind.ObjectMapper;

public class FileGenerator {
    private static final int BATCH_SIZE = 50; // Process QR codes in batches
    private static final int MAX_IMAGE_SIZE = 1024 * 1024; // 1MB max image size

    /**
     * Creates an Excel file to store QR code data in tabular format with hyperlinks and QR code images.
     */
    public void createQrTableXlsx(HashMap<Integer, List<QRCodeDetails>> qrData, String outputPath) throws IOException {
        if (!Utils.validateDirectoryPath(outputPath)) {
            throw new IOException("[ERROR] Invalid output directory: " + outputPath);
        }

        Utils.measureExecutionTime(() -> {
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("QR Codes");
                setupSheetHeaders(sheet, workbook);
                
                int rowIndex = 1;
                int processedItems = 0;
                
                // Process entries in batches
                for (var entry : qrData.entrySet()) {
                    int pageNo = entry.getKey();
                    for (QRCodeDetails details : entry.getValue()) {
                        createRowWithQRCode(workbook, sheet, rowIndex++, pageNo, details);
                        processedItems++;
                        
                        // Perform cleanup after batch processing
                        if (processedItems % BATCH_SIZE == 0) {
                            // sheet.flushRows(); // Flush rows to disk
                            System.gc(); // Controlled GC after batch processing
                            System.out.println("[INFO] Processed " + processedItems + " QR codes");
                        }
                    }
                }

                saveWorkbook(workbook, outputPath);
            } catch (IOException e) {
                System.err.println("[ERROR] Failed to create Excel file: " + e.getMessage());
            }
        }, "Create Excel File");
    }

    private void setupSheetHeaders(Sheet sheet, Workbook workbook) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Page No", "QR Code ID", "Text", "Short Link", "QR Code"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(createHeaderCellStyle(workbook));
            sheet.setColumnWidth(i, 15 * 256); // Set optimal column width
        }
    }

    private void createRowWithQRCode(Workbook workbook, Sheet sheet, int rowIndex, 
                                   int pageNo, QRCodeDetails details) throws IOException {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(150);

        // Create basic cells
        row.createCell(0).setCellValue(pageNo);
        row.createCell(1).setCellValue(generateQrCodeName(details));
        row.createCell(2).setCellValue(details.getText());

        // Create hyperlink cell
        createHyperlinkCell(workbook, row, details);

        // Add QR code image with size validation
        addQRCodeImage(workbook, sheet, row, details, rowIndex);
    }

    private void createHyperlinkCell(Workbook workbook, Row row, QRCodeDetails details) {
        String shortLink = shortenUrl(details.getLink());
        Cell linkCell = row.createCell(3);
        linkCell.setCellValue(shortLink);

        CreationHelper creationHelper = workbook.getCreationHelper();
        Hyperlink hyperlink = creationHelper.createHyperlink(HyperlinkType.URL);
        hyperlink.setAddress(details.getLink());
        linkCell.setHyperlink(hyperlink);
        linkCell.setCellStyle(createHyperlinkStyle(workbook));
    }

    private void addQRCodeImage(Workbook workbook, Sheet sheet, Row row, 
                              QRCodeDetails details, int rowIndex) throws IOException {
        try (InputStream is = new BufferedInputStream(new FileInputStream(details.getQrFilePath()))) {
            byte[] imageBytes = IOUtils.toByteArray(is);
            
            // Validate image size
            if (imageBytes.length > MAX_IMAGE_SIZE) {
                throw new IOException("[ERROR] QR code image too large: " + details.getQrFilePath());
            }

            int pictureIndex = workbook.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
            
            // Use row parameter to set cell height for better image display
            row.setHeightInPoints(150); // Adjust row height for QR code
            
            // Set anchor points using row height
            anchor.setCol1(4);
            anchor.setRow1(rowIndex);
            anchor.setCol2(5);
            anchor.setRow2(rowIndex + 1);
            
            Picture picture = drawing.createPicture(anchor, pictureIndex);
            // Adjust picture scaling based on row height
            double scale = row.getHeightInPoints() / picture.getImageDimension().getHeight();
            picture.resize(scale);
        }
    }

    private void saveWorkbook(Workbook workbook, String outputPath) throws IOException {
        File xlsxFile = new File(outputPath, Utils.sanitizeFileName("QrTable.xlsx"));
        try (FileOutputStream outputStream = new FileOutputStream(xlsxFile)) {
            workbook.write(outputStream);
            System.out.println("[INFO] Excel file created successfully at: " + xlsxFile.getAbsolutePath());
        }
    }

    /**
     * Creates a PDF file with QR code details in a tabular matrix layout.
     */
    public void createQrTablePdf(HashMap<Integer, List<QRCodeDetails>> qrData, String outputPath) throws IOException {
        if (!Utils.validateDirectoryPath(outputPath)) {
            throw new IOException("[ERROR] Invalid output directory: " + outputPath);
        }

        try (PDDocument pdfDocument = new PDDocument()) {
            float margin = 50;
            float tableWidth = PDRectangle.A4.getWidth() - (2 * margin);
            float cellWidth = tableWidth / 3; // 3 columns per row
            float cellHeight = 120; // Height for QR code, page number, and link
            float yStart = PDRectangle.A4.getHeight() - margin;
            float xStart = margin;

            // Load the font from resources
            InputStream fontStream = getClass().getClassLoader().getResourceAsStream("fonts/times.ttf");
            if (fontStream == null) {
                throw new IOException("Font file 'times.ttf' not found in resources.");
            }
            PDType0Font font = PDType0Font.load(pdfDocument, fontStream);

            PDPage page = new PDPage(PDRectangle.A4);
            pdfDocument.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, page);

            float yPosition = yStart;
            int currentColumn = 0;

            // Sort QR code data by page number (ascending order)
            TreeMap<Integer, List<QRCodeDetails>> sortedQrData = new TreeMap<>(qrData);

            for (var entry : sortedQrData.entrySet()) {
                int pageNo = entry.getKey();
                for (QRCodeDetails details : entry.getValue()) {
                    validateQRCodeFile(details); // Ensure the QR code file exists

                    // Add a new page if the row exceeds the bottom margin
                    if (currentColumn == 0 && yPosition - cellHeight < margin) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        pdfDocument.addPage(page);
                        contentStream = new PDPageContentStream(pdfDocument, page);
                        yPosition = yStart;
                    }

                    // Calculate cell positions
                    float xPosition = xStart + (currentColumn * cellWidth);

                    // Draw border for the cell
                    contentStream.setLineWidth(0.5f);
                    contentStream.addRect(xPosition, yPosition - cellHeight, cellWidth, cellHeight);
                    contentStream.stroke();

                    // Add QR code image
                    PDImageXObject qrImage = PDImageXObject.createFromFile(details.getQrFilePath(), pdfDocument);
                    float qrCodeSize = Math.min(cellWidth - 20, cellHeight - 40); // Fit QR code within cell
                    float qrX = xPosition + (cellWidth - qrCodeSize) / 2;
                    float qrY = yPosition - cellHeight + 10;
                    contentStream.drawImage(qrImage, qrX, qrY, qrCodeSize, qrCodeSize);

                    // Add page number
                    contentStream.beginText();
                    contentStream.setFont(font, 10);
                    contentStream.newLineAtOffset(xPosition + 5, yPosition - 20);
                    contentStream.showText("Page No: " + pageNo);
                    contentStream.endText();

                    // Add final page slug as the link name
                    String slugName = extractAndTruncateSlug(details.getLink());
                    contentStream.beginText();
                    contentStream.setFont(font, 10);
                    contentStream.newLineAtOffset(xPosition + 5, yPosition - 35);
                    contentStream.showText("Link: " + slugName);
                    contentStream.endText();

                    // Move to the next column
                    currentColumn++;
                    if (currentColumn == 3) { // Reset column after 3 columns
                        currentColumn = 0;
                        yPosition -= cellHeight; // Move to the next row
                    }
                }
            }

            contentStream.close();

            // Ensure the PDF file is saved with the correct extension
            File pdfFile = new File(outputPath, Utils.sanitizeFileName("QrTable.pdf"));
            if (!pdfFile.getName().endsWith(".pdf")) {
                pdfFile = new File(pdfFile.getAbsolutePath() + ".pdf");
            }

            pdfDocument.save(pdfFile);
            System.out.println("[INFO] PDF file created successfully at: " + pdfFile.getAbsolutePath());
        }
    }

    /**
     * Extracts the final portion of the URL (slug) and truncates it to 7 characters.
     *
     * @param url The full URL link.
     * @return The extracted and truncated page slug.
     */
    private String extractAndTruncateSlug(String url) {
        if (url == null || url.isEmpty()) return "Unknown";

        String[] parts = url.split("/");
        String slug = parts[parts.length - 1];
        return slug.length() > 7 ? slug.substring(0, 7) : slug;
    }


    /**
     * Validates whether the QR code file exists and is readable.
     *
     * @param details The QRCodeDetails object containing the file path to the QR code.
     * @throws IOException If the QR code file does not exist or is not readable.
     */
    private void validateQRCodeFile(QRCodeDetails details) throws IOException {
        if (details == null) {
            throw new IOException("[ERROR] QRCodeDetails is null. Validation failed.");
        }

        String qrFilePath = details.getQrFilePath();
        if (qrFilePath == null || qrFilePath.isEmpty()) {
            throw new IOException("[ERROR] QR code file path is missing or empty in QRCodeDetails.");
        }

        File qrFile = new File(qrFilePath);
        if (!qrFile.exists()) {
            throw new IOException("[ERROR] QR code file does not exist: " + qrFilePath);
        }

        if (!qrFile.canRead()) {
            throw new IOException("[ERROR] QR code file is not readable: " + qrFilePath);
        }

        System.out.println("[INFO] QR code file validated successfully: " + qrFilePath);
    }



    /**
     * Creates a JSON file to store QR code details in ascending order by page number.
     */
    public void createQrCodesJson(HashMap<Integer, List<QRCodeDetails>> qrData, String outputPath) throws IOException {
        if (!Utils.validateDirectoryPath(outputPath)) {
            throw new IOException("[ERROR] Invalid output directory: " + outputPath);
        }

        // Sort the QR code data by page number (ascending order)
        ObjectMapper objectMapper = new ObjectMapper();
        File jsonFile = new File(outputPath, Utils.sanitizeFileName("QrCodes.json"));

        try {
            TreeMap<Integer, List<QRCodeDetails>> sortedQrData = new TreeMap<>(qrData); // Use TreeMap for sorting by key
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, sortedQrData);
            System.out.println("[INFO] JSON file created successfully at: " + jsonFile.getAbsolutePath());
        } catch (IOException e) {
            throw new IOException("[ERROR] Failed to create JSON file: " + e.getMessage(), e);
        }
    }


    // Utility methods
    private String extractFinalPartOfUrl(String url) {
        if (url == null || url.isEmpty()) return "Unknown";
        String[] parts = url.split("/");
        return parts[parts.length - 1].split("\\.")[0];
    }

    private String generateQrCodeName(QRCodeDetails details) {
        String link = details.getLink();
        if (link == null || link.isEmpty()) return "Unknown";

        String finalPart = extractFinalPartOfUrl(link);
        String prefix = finalPart.length() > 2 ? finalPart.substring(0, 2) : finalPart;
        String suffix = finalPart.length() > 3 ? finalPart.substring(finalPart.length() - 3) : finalPart;

        return prefix + suffix;
    }

    /**
     * Shortens a URL to a maximum of 5 characters.
     */
    private String shortenUrl(String url) {
        if (url == null || url.isEmpty()) return "N/A";
        return url.length() > 5 ? url.substring(0, 5) + "..." : url;
    }


    private CellStyle createHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createHyperlinkStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setUnderline(Font.U_SINGLE);
        font.setColor(IndexedColors.BLUE.getIndex());
        style.setFont(font);
        return style;
    }
}
