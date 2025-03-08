package org.example;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        ProgressTracker tracker = new ProgressTracker();

        try (Scanner scanner = new Scanner(System.in)) { // Automatically close Scanner resource
            // Step 1: Welcome Message and User Inputs
            System.out.println("Welcome to the Dynamic QR Code Generator!");

            // Base Path for Directory
            String basePath = getInputWithValidation(scanner, "Enter Base Path (e.g., D:\\ or C:\\Users\\YourName\\Documents\\): ",
                    Utils::validateDirectoryPath, "Invalid base path. Please try again: ");

            // User Name
            String userName = getInputWithValidation(scanner, "Enter UserName: ",
                    input -> !input.trim().isEmpty(), "UserName cannot be empty. Please enter a valid UserName: ");

            // Document Path
            String documentPath = getInputWithValidation(scanner, "Enter Document Path (PDF file): ",
                    Utils::validateFilePath, "Invalid document path. Please provide a valid PDF file: ");

            // Output File Name
            String fileName = getInputWithValidation(scanner, "Enter File Name (Without Extension): ",
                    input -> !input.trim().isEmpty(), "File Name cannot be empty. Please enter a valid File Name: ");

            // QR Code Customization
            int qrWidth = 250; // Default size
            int qrHeight = 250; // Default size
            System.out.print("Do you want to customize the QR code size? (yes/no): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("yes")) {
                System.out.print("Enter QR Code Width (in pixels): ");
                qrWidth = getValidNumericInput(scanner);
                System.out.print("Enter QR Code Height (in pixels): ");
                qrHeight = getValidNumericInput(scanner);
            }

            // Initialize Progress Tracker
            tracker.startTracking(7); // Updated to 7 steps to include smart QR placement
            System.out.println("\nProcess started with 7 steps.\n");

            // Step 2: Directory Setup
            tracker.updateProgress("Creating project directories...");
            FolderManager folderManager = new FolderManager();
            String userDir;
            String docDir;
            try {
                userDir = folderManager.createUserDirectory(basePath, userName);
                docDir = folderManager.createDocumentDirectory(userDir, fileName);
            } catch (IOException e) {
                throw new IOException("Failed to create directories: " + e.getMessage());
            }

            // Step 3: Analyze Document (Extract Links)
            tracker.updateProgress("Analyzing document for visible and embedded links...");
            DocumentAnalyzer analyzer = new DocumentAnalyzer();
            HashMap<Integer, List<String>> pageLinks;
            try {
                pageLinks = analyzer.analyzeDocument(documentPath);
            } catch (IOException e) {
                throw new IOException("Error during document analysis: " + e.getMessage());
            }

            if (pageLinks.isEmpty()) {
                tracker.logMessage("No links found in the document. Process completed successfully.");
                tracker.completeTracking();
                return;
            }

            // Step 4: Generate QR Codes
            tracker.updateProgress("Generating QR codes...");
            QRCodeGenerator qrGenerator = new QRCodeGenerator();
            HashMap<Integer, List<QRCodeDetails>> qrData;
            try {
                qrData = qrGenerator.generateQRCodes(pageLinks, docDir, qrWidth, qrHeight);
            } catch (IOException e) {
                throw new IOException("Failed to generate QR codes: " + e.getMessage());
            }

            // Step 5: Generate Supplementary Files
            tracker.updateProgress("Creating supplementary files...");
            FileGenerator fileGenerator = new FileGenerator();
            try {
                fileGenerator.createQrCodesJson(qrData, docDir);
                fileGenerator.createQrTableXlsx(qrData, docDir);
                fileGenerator.createQrTablePdf(qrData, docDir);
            } catch (IOException e) {
                throw new IOException("Failed to generate supplementary files: " + e.getMessage());
            }

            // Step 6: Smart QR Code Placement into PDF
            tracker.updateProgress("Dynamically embedding QR codes into the PDF using smart placement...");
            SmartQRCodeGenerator smartQrGenerator = new SmartQRCodeGenerator();
            try {
                smartQrGenerator.placeSmartQRCodes(documentPath, qrData, userDir + "/" + fileName + "_Final.pdf");
            } catch (IOException e) {
                throw new IOException("Failed to embed QR codes dynamically into the PDF: " + e.getMessage());
            }

            // Final Step: Completion
            tracker.updateProgress("Finalizing and saving output files...");
            tracker.completeTracking();
            tracker.logMessage("Process completed successfully! All files are saved in: " + userDir);

        } catch (IOException e) {
            System.err.println("\nAn error occurred during the process: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("\nInvalid numeric input. Please ensure you enter valid numbers.");
        }
    }

    /**
     * Helper method to get a valid numeric input.
     *
     * @param scanner The Scanner instance for user input.
     * @return A valid integer input.
     */
    private static int getValidNumericInput(Scanner scanner) {
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.err.print("Invalid number. Please enter a valid numeric value: ");
            }
        }
    }

    /**
     * Helper method to get validated user input based on a condition.
     *
     * @param scanner        The Scanner instance for user input.
     * @param prompt         The prompt message for the user.
     * @param validation     A validation function to check the input's validity.
     * @param errorMessage   The error message to display if the input is invalid.
     * @return The validated user input.
     */
    private static String getInputWithValidation(Scanner scanner, String prompt, java.util.function.Predicate<String> validation, String errorMessage) {
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
