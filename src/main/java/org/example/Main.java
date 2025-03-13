package org.example;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    private static final int DEFAULT_QR_SIZE = 250;
    private static final int MIN_QR_SIZE = 100;
    private static final int MAX_QR_SIZE = 1000;
    private static final AtomicInteger processedItems = new AtomicInteger(0);
    private static final int GC_THRESHOLD = 100;

    // Define QRSize record at class level
    private record QRSize(int width, int height) {
        public QRSize {
            if (width < MIN_QR_SIZE || width > MAX_QR_SIZE)
                throw new IllegalArgumentException("Width must be between " + MIN_QR_SIZE + " and " + MAX_QR_SIZE);
            if (height < MIN_QR_SIZE || height > MAX_QR_SIZE)
                throw new IllegalArgumentException("Height must be between " + MIN_QR_SIZE + " and " + MAX_QR_SIZE);
        }
    }

    // Define UserInputs record at class level
    private record UserInputs(String basePath, String userName, String documentPath, String fileName, QRSize qrSize) {
        public UserInputs {
            if (basePath == null || basePath.trim().isEmpty()) 
                throw new IllegalArgumentException("Base path cannot be empty");
            if (userName == null || userName.trim().isEmpty()) 
                throw new IllegalArgumentException("Username cannot be empty");
            if (documentPath == null || documentPath.trim().isEmpty()) 
                throw new IllegalArgumentException("Document path cannot be empty");
            if (fileName == null || fileName.trim().isEmpty()) 
                throw new IllegalArgumentException("File name cannot be empty");
            if (qrSize == null) 
                throw new IllegalArgumentException("QR size cannot be null");
        }
    }

    public static void main(String[] args) {
        ProgressTracker tracker = new ProgressTracker();

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Welcome to the Dynamic QR Code Generator!");

            // Collect all user inputs first
            UserInputs inputs = collectUserInputs(scanner);
            
            // Initialize progress tracking
            tracker.startTracking(7);
            System.out.println("\nProcess started with 7 steps.\n");

            // Process the PDF and generate outputs
            processDocument(inputs, tracker);

        } catch (IOException e) {
            tracker.logError("Process failed: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Invalid numeric input. Ensure you enter valid numbers.");
        } finally {
            cleanupResources(tracker);
        }
    }

    private static void cleanupResources(ProgressTracker tracker) {
        Utils.cleanup();
        if (processedItems.get() > 0) {
            System.gc();
            processedItems.set(0);
            tracker.logMessage("[INFO] Final cleanup completed.");
        }
    }

    private static UserInputs collectUserInputs(Scanner scanner) {
        String basePath = getInputWithValidation(scanner,
                "Enter Base Path (e.g., D:\\ or C:\\Users\\YourName\\Documents\\): ",
                Utils::validateDirectoryPath, 
                "Invalid base path. Please try again: ");

        String userName = getInputWithValidation(scanner,
                "Enter UserName: ",
                input -> !input.trim().isEmpty(), 
                "UserName cannot be empty. Please enter a valid UserName: ");

        String documentPath = getInputWithValidation(scanner,
                "Enter Document Path (PDF file): ",
                Utils::validateFilePath, 
                "Invalid document path. Please provide a valid PDF file: ");

        String fileName = getInputWithValidation(scanner,
                "Enter File Name (Without Extension): ",
                input -> !input.trim().isEmpty(), 
                "File Name cannot be empty. Please enter a valid File Name: ");

        // QR code size customization
        QRSize qrSize = getCustomQRSize(scanner);

        return new UserInputs(basePath, userName, documentPath, fileName, qrSize);
    }

    private static void processDocument(UserInputs inputs, ProgressTracker tracker) throws IOException {
        // Step 1: Create directories
        tracker.updateProgress("Creating project directories...");
        FolderManager folderManager = new FolderManager();
        String userDir = folderManager.createUserDirectory(inputs.basePath(), inputs.userName());
        String docDir = folderManager.createDocumentDirectory(userDir, inputs.fileName());

        // Step 2: Analyze document for links
        tracker.updateProgress("Analyzing document for links...");
        DocumentAnalyzer analyzer = new DocumentAnalyzer();
        HashMap<Integer, List<String>> pageLinks = analyzer.analyzeDocument(inputs.documentPath(), docDir);

        if (pageLinks.isEmpty()) {
            tracker.logMessage("No links found. Process completed.");
            tracker.completeTracking();
            return;
        }

        // Process remaining steps
        processQRCodesAndOutputs(pageLinks, inputs, userDir, docDir, tracker);
    }

    private static void processQRCodesAndOutputs(
            HashMap<Integer, List<String>> pageLinks, 
            UserInputs inputs,
            String userDir, 
            String docDir, 
            ProgressTracker tracker) throws IOException {
        
        tracker.updateProgress("Categorizing links...");
        HashMap<String, List<String>> categorizedLinks = LinkExtractor.extractAndCategorizeLinks(
                String.join(" ", pageLinks.values().stream().flatMap(List::stream).toList())
        );

        // Use categorizedLinks for reference (removing unused variable warning)
        tracker.logMessage("Found " + categorizedLinks.size() + " different link categories");

        tracker.updateProgress("Generating QR codes...");
        QRCodeGenerator qrGenerator = new QRCodeGenerator();
        HashMap<Integer, List<QRCodeDetails>> qrData = qrGenerator.generateQRCodes(
                pageLinks, docDir, inputs.qrSize().width(), inputs.qrSize().height());

        processedItems.addAndGet(qrData.values().stream().mapToInt(List::size).sum());

        if (processedItems.get() >= GC_THRESHOLD) {
            System.gc();
            processedItems.set(0);
            tracker.logMessage("[INFO] Memory cleaned after processing QR codes");
        }

        generateOutputFiles(qrData, inputs, userDir, docDir, tracker);
    }

    private static void generateOutputFiles(
            HashMap<Integer, List<QRCodeDetails>> qrData,
            UserInputs inputs,
            String userDir,
            String docDir,
            ProgressTracker tracker) throws IOException {
        
        // Step 5: Generate supplementary files
        tracker.updateProgress("Creating supplementary files...");
        FileGenerator fileGenerator = new FileGenerator();
        fileGenerator.createQrCodesJson(qrData, docDir);
        fileGenerator.createQrTableXlsx(qrData, docDir);
        fileGenerator.createQrTablePdf(qrData, docDir);

        // Step 6: Embed QR codes into PDF
        tracker.updateProgress("Embedding QR codes into PDF...");
        try (PDFEditor pdfEditor = new PDFEditor()) {
            pdfEditor.embedQRCodes(inputs.documentPath(), qrData, 
                    userDir + "/" + inputs.fileName() + "_Final", tracker);
        }

        // Step 7: Generate responsive website
        tracker.updateProgress("Generating responsive website...");
        SmartQRCodeGenerator smartQrGenerator = new SmartQRCodeGenerator();
        smartQrGenerator.convertPDFToWebsite(inputs.documentPath(), qrData, userDir + "/Website");

        // Finalize process
        tracker.updateProgress("Finalizing output...");
        tracker.completeTracking();
        tracker.logMessage("Process completed successfully! Files saved in: " + userDir);
    }

    private static QRSize getCustomQRSize(Scanner scanner) {
        System.out.print("Do you want to customize the QR code size? (yes/no): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("yes")) {
            int width = getValidNumericInput(scanner, "Enter QR Code Width (in pixels): ");
            int height = getValidNumericInput(scanner, "Enter QR Code Height (in pixels): ");
            return new QRSize(
                Math.min(Math.max(width, MIN_QR_SIZE), MAX_QR_SIZE),
                Math.min(Math.max(height, MIN_QR_SIZE), MAX_QR_SIZE)
            );
        }
        return new QRSize(DEFAULT_QR_SIZE, DEFAULT_QR_SIZE);
    }

    private static int getValidNumericInput(Scanner scanner, String prompt) {
        System.out.print(prompt);
        while (true) {
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= MIN_QR_SIZE && value <= MAX_QR_SIZE) {
                    return value;
                }
                System.err.printf("Please enter a value between %d and %d: ", MIN_QR_SIZE, MAX_QR_SIZE);
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
