package org.example;

// Required imports for JSON handling
import com.fasterxml.jackson.databind.ObjectMapper;

// Required imports for Excel handling
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.common.usermodel.HyperlinkType;

// Required imports for PDF creation
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

// Required imports for Java utilities
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class FileGenerator {

    /**
     * Creates a JSON file to store QR code data with updated text (final part of the URL).
     *
     * @param qrData     The mapping of page numbers to QR code details.
     * @param outputPath The directory where the JSON file will be saved.
     * @throws IOException If there is an error while writing the JSON file.
     */
    public void createQrCodesJson(HashMap<Integer, List<QRCodeDetails>> qrData, String outputPath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        // Update the text field with the final part of the URL
        for (var entry : qrData.entrySet()) {
            List<QRCodeDetails> detailsList = entry.getValue();
            for (QRCodeDetails details : detailsList) {
                String finalPart = extractFinalPartOfUrl(details.getLink());
                details.setText(finalPart); // Update the text field
            }
        }

        File jsonFile = new File(outputPath + "/QrCodes.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, qrData);
        System.out.println("JSON file created successfully at: " + jsonFile.getAbsolutePath());
    }

    /**
     * Creates an Excel file to store QR code data in tabular format with hyperlinks and QR code images.
     *
     * @param qrData     The mapping of page numbers to QR code details.
     * @param outputPath The directory where the Excel file will be saved.
     * @throws IOException If there is an error while creating the Excel file.
     */
    public void createQrTableXlsx(HashMap<Integer, List<QRCodeDetails>> qrData, String outputPath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("QR Codes");

            // Create the header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"QR Code ID (P1.Q1)", "Text (Final URL Part)", "Link (Hyperlink)", "QR Code"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderCellStyle(workbook));
            }

            // Populate rows with data
            int rowIndex = 1;
            for (var entry : qrData.entrySet()) {
                int pageNo = entry.getKey();
                List<QRCodeDetails> detailsList = entry.getValue();

                for (int i = 0; i < detailsList.size(); i++) {
                    QRCodeDetails details = detailsList.get(i);
                    Row row = sheet.createRow(rowIndex++);

                    // QR Code ID
                    row.createCell(0).setCellValue("P" + pageNo + ".Q" + (i + 1));

                    // Final URL text
                    String finalPart = extractFinalPartOfUrl(details.getLink());
                    row.createCell(1).setCellValue(finalPart);

                    // Hyperlink
                    Cell linkCell = row.createCell(2);
                    linkCell.setCellValue(details.getLink());
                    CreationHelper creationHelper = workbook.getCreationHelper();
                    Hyperlink hyperlink = creationHelper.createHyperlink(HyperlinkType.URL);
                    hyperlink.setAddress(details.getLink());
                    linkCell.setHyperlink(hyperlink);
                    linkCell.setCellStyle(createHyperlinkStyle(workbook));

                    // QR Code Image
                    try (InputStream is = new FileInputStream(details.getQrFilePath())) {
                        byte[] imageBytes = IOUtils.toByteArray(is);
                        int pictureIndex = workbook.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);
                        Drawing<?> drawing = sheet.createDrawingPatriarch();
                        ClientAnchor anchor = creationHelper.createClientAnchor();
                        anchor.setCol1(3);
                        anchor.setRow1(rowIndex - 1);
                        Picture picture = drawing.createPicture(anchor, pictureIndex);
                        picture.resize(1.0);
                    }
                }
            }

            // Write the Excel file
            File xlsxFile = new File(outputPath + "/QrTable.xlsx");
            try (FileOutputStream outputStream = new FileOutputStream(xlsxFile)) {
                workbook.write(outputStream);
                System.out.println("Excel file created successfully at: " + xlsxFile.getAbsolutePath());
            }
        }
    }

    /**
     * Creates a PDF file with QR code details in tabular format, including text and pagination.
     *
     * @param qrData     The mapping of page numbers to QR code details.
     * @param outputPath The directory where the PDF file will be saved.
     * @throws IOException If there is an error while creating the PDF file.
     */
    public void createQrTablePdf(HashMap<Integer, List<QRCodeDetails>> qrData, String outputPath) throws IOException {
        try (PDDocument pdfDocument = new PDDocument()) {
            float margin = 50;
            float yStart = PDRectangle.A4.getHeight() - margin;
            float rowHeight = 140;

            InputStream fontStream = getClass().getResourceAsStream("/fonts/times.ttf");
            if (fontStream == null) throw new IOException("Font file 'times.ttf' not found.");
            PDType0Font font = PDType0Font.load(pdfDocument, fontStream);

            PDPage page = new PDPage(PDRectangle.A4);
            pdfDocument.addPage(page);

            // Initialize content stream without declaring it as final
            PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, page);
            float yPosition = yStart;

            for (var entry : qrData.entrySet()) {
                int pageNo = entry.getKey();
                for (QRCodeDetails details : entry.getValue()) {
                    if (yPosition < margin) {
                        contentStream.close(); // Close the current stream
                        page = new PDPage(PDRectangle.A4);
                        pdfDocument.addPage(page);
                        contentStream = new PDPageContentStream(pdfDocument, page);
                        yPosition = yStart;
                    }

                    contentStream.beginText();
                    contentStream.setFont(font, 12);
                    contentStream.newLineAtOffset(margin, yPosition);
                    contentStream.showText("P" + pageNo + ".Q" + extractFinalPartOfUrl(details.getLink()));
                    contentStream.endText();

                    // Embed QR Code image
                    PDImageXObject qrImage = PDImageXObject.createFromFile(details.getQrFilePath(), pdfDocument);
                    contentStream.drawImage(qrImage, margin + 200, yPosition - 100, 100, 100);

                    yPosition -= rowHeight;
                }
            }

            contentStream.close(); // Close the final content stream
            File pdfFile = new File(outputPath + "/QrTable.pdf");
            pdfDocument.save(pdfFile);
            System.out.println("PDF file created successfully at: " + pdfFile.getAbsolutePath());
        }
    }

    // Utility methods
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

    private String extractFinalPartOfUrl(String url) {
        if (url == null || url.isEmpty()) return "Unknown";
        String[] parts = url.split("/");
        return parts[parts.length - 1].split("\\.")[0]; // Handle extensions like .html
    }
}
