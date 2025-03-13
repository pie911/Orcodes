package org.example;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        ProgressTracker tracker = new ProgressTracker();

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Welcome to the Dynamic QR Code Generator!");

            // Get validated inputs from the user
            String basePath = getInputWithValidation(scanner,
                    "Enter Base Path (e.g., D:\\ or C:\\Users\\YourName\\Documents\\): ",
                    Utils::validateDirectoryPath, "Invalid base path. Please try again: ");

            String userName = getInputWithValidation(scanner,
                    "Enter UserName: ",
                    input -> !input.trim().isEmpty(), "UserName cannot be empty. Please enter a valid UserName: ");

            String documentPath = getInputWithValidation(scanner,
                    "Enter Document Path (PDF file): ",
                    Utils::validateFilePath, "Invalid document path. Please provide a valid PDF file: ");

            String fileName = getInputWithValidation(scanner,
                    "Enter File Name (Without Extension): ",
                    input -> !input.trim().isEmpty(), "File Name cannot be empty. Please enter a valid File Name: ");

            // Optionally customize QR code size
            int qrWidth = 250;
            int qrHeight = 250;
            System.out.print("Do you want to customize the QR code size? (yes/no): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("yes")) {
                qrWidth = getValidNumericInput(scanner, "Enter QR Code Width (in pixels): ");
                qrHeight = getValidNumericInput(scanner, "Enter QR Code Height (in pixels): ");
            }

            // Initialize progress tracking
            tracker.startTracking(7); // Define 7 steps
            System.out.println("\nProcess started with 7 steps.\n");

            // Step 1: Create directories
            tracker.updateProgress("Creating project directories...");
            FolderManager folderManager = new FolderManager();
            String userDir;
            String docDir;
            try {
                userDir = folderManager.createUserDirectory(basePath, userName);
                docDir = folderManager.createDocumentDirectory(userDir, fileName);
            } catch (IOException e) {
                tracker.logError("Failed to create directories: " + e.getMessage());
                return;
            }

            // Step 2: Analyze the document for links
            tracker.updateProgress("Analyzing document for links...");
            DocumentAnalyzer analyzer = new DocumentAnalyzer();
            HashMap<Integer, List<String>> pageLinks;
            try {
                pageLinks = analyzer.analyzeDocument(documentPath, docDir); // Updated for compatibility
            } catch (IOException e) {
                tracker.logError("Error during document analysis: " + e.getMessage());
                return;
            }

            if (pageLinks.isEmpty()) {
                tracker.logMessage("No links found. Process completed.");
                tracker.completeTracking();
                return;
            }

            // Step 3: Categorize links
            tracker.updateProgress("Categorizing links...");
            HashMap<String, List<String>> categorizedLinks = LinkExtractor.extractAndCategorizeLinks(
                    String.join(" ", pageLinks.values().stream().flatMap(List::stream).toList())
            );
            System.out.println("Categorized Links: " + categorizedLinks);

            // Step 4: Generate QR codes
            tracker.updateProgress("Generating QR codes...");
            QRCodeGenerator qrGenerator = new QRCodeGenerator();
            HashMap<Integer, List<QRCodeDetails>> qrData;
            try {
                qrData = qrGenerator.generateQRCodes(pageLinks, docDir, qrWidth, qrHeight);
            } catch (IOException e) {
                tracker.logError("Failed to generate QR codes: " + e.getMessage());
                return;
            }

            // Step 5: Generate supplementary files
            tracker.updateProgress("Creating supplementary files...");
            FileGenerator fileGenerator = new FileGenerator();
            try {
                fileGenerator.createQrCodesJson(qrData, docDir); // Ensured compatibility
                fileGenerator.createQrTableXlsx(qrData, docDir);
                fileGenerator.createQrTablePdf(qrData, docDir);
            } catch (IOException e) {
                tracker.logError("Failed to generate supplementary files: " + e.getMessage());
                return;
            }

            // Step 6: Embed QR codes into PDF
            tracker.updateProgress("Embedding QR codes into PDF...");
            PDFEditor pdfEditor = new PDFEditor(); // Ensure PDFEditor implementation exists
            try {
                pdfEditor.embedQRCodes(documentPath, qrData, userDir + "/" + fileName + "_Final", tracker);
                tracker.logMessage("QR codes successfully embedded into the PDF.");
            } catch (IOException e) {
                tracker.logError("Failed to embed QR codes: " + e.getMessage());
                return;
            }

            // Step 7: Generate responsive website
            tracker.updateProgress("Generating responsive website...");
            SmartQRCodeGenerator smartQrGenerator = new SmartQRCodeGenerator();
            try {
                smartQrGenerator.convertPDFToWebsite(documentPath, qrData, userDir + "/Website");
                tracker.logMessage("Responsive website generated successfully.");
            } catch (IOException e) {
                tracker.logError("Failed to generate website: " + e.getMessage());
            }

            // Finalize process
            tracker.updateProgress("Finalizing output...");
            tracker.completeTracking();
            tracker.logMessage("Process completed successfully! Files saved in: " + userDir);

        } catch (NumberFormatException e) {
            System.err.println("Invalid numeric input. Ensure you enter valid numbers.");
        }
    }

    private static int getValidNumericInput(Scanner scanner, String prompt) {
        System.out.print(prompt);
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.err.print("Invalid number. Try again: ");
            }
        }
    }

    private static String getInputWithValidation(Scanner scanner, String prompt,
                                                 java.util.function.Predicate<String> validation, String errorMessage) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        while (!validation.test(input)) {
            System.err.print(errorMessage);
            System.out.print(prompt);
            input = scanner.nextLine().trim();
        }
        return input;
    }
}
