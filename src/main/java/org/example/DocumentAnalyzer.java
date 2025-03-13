package org.example;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;

public class DocumentAnalyzer {

    private final FolderManager folderManager = new FolderManager(); // For folder operations
    private static final Pattern LINK_PATTERN = Pattern.compile("\\bhttps?://[\\w\\-._~:/?#@!$&'()*+,;=%]+\\b");
    private static final int BATCH_SIZE = 50; // Process pages in batches
    private PDType0Font cachedFont; // Cache font for reuse
    private PDType0Font cachedBoldFont;
    private static final String FONT_PATH = "/fonts/times.ttf";
    private static final String FONT_BOLD_PATH = "/fonts/timesbd.ttf";

    // Add font loading method
    private PDType0Font getFont(PDDocument document, boolean bold) throws IOException {
        if (bold) {
            if (cachedBoldFont == null) {
                try (InputStream fontStream = getClass().getResourceAsStream(FONT_BOLD_PATH)) {
                    if (fontStream == null) {
                        throw new IOException("[ERROR] Bold font file not found: " + FONT_BOLD_PATH);
                    }
                    cachedBoldFont = PDType0Font.load(document, fontStream);
                    System.out.println("[INFO] Times Bold font loaded successfully");
                }
            }
            return cachedBoldFont;
        } else {
            if (cachedFont == null) {
                try (InputStream fontStream = getClass().getResourceAsStream(FONT_PATH)) {
                    if (fontStream == null) {
                        throw new IOException("[ERROR] Font file not found: " + FONT_PATH);
                    }
                    cachedFont = PDType0Font.load(document, fontStream);
                    System.out.println("[INFO] Times Regular font loaded successfully");
                }
            }
            return cachedFont;
        }
    }

    /**
     * Analyze the PDF document to extract both visible and embedded links.
     *
     * @param documentPath The path of the PDF document.
     * @param outputDir    The output directory for analysis results.
     * @return A HashMap with page numbers as keys and a list of extracted links as values.
     * @throws IOException If the document cannot be read or processed.
     */
    public HashMap<Integer, List<String>> analyzeDocument(String documentPath, String outputDir) throws IOException {
        HashMap<Integer, List<String>> pageLinks = new HashMap<>();
        int processedPages = 0;

        // Validate inputs
        File pdfFile = validateInputs(documentPath);
        String analysisDir = folderManager.createDocumentDirectory(outputDir, "Analysis");

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            int totalPages = document.getNumberOfPages();
            System.out.println("[INFO] Starting analysis of " + totalPages + " pages...");

            // Process each page with batch handling
            for (int pageNum = 0; pageNum < totalPages; pageNum++) {
                PDPage page = document.getPage(pageNum);
                Set<String> links = new LinkedHashSet<>(); // Avoid duplicates while preserving order

                System.out.println("[INFO] Processing page " + (pageNum + 1) + " of " + totalPages);

                // Extract links
                links.addAll(extractVisibleLinks(document, pageNum + 1));
                links.addAll(extractEmbeddedLinks(page));

                // Save links if found
                if (!links.isEmpty()) {
                    pageLinks.put(pageNum + 1, new ArrayList<>(links));
                    saveLinksToFile(pageLinks, analysisDir, pageNum + 1);
                }

                // Batch processing cleanup
                processedPages++;
                if (processedPages % BATCH_SIZE == 0) {
                    System.gc();
                    System.out.println("[INFO] Memory cleaned after processing " + processedPages + " pages");
                }
            }

            System.out.println("[INFO] Document analysis completed. Found links in " + pageLinks.size() + " pages.");
        }

        return pageLinks;
    }

    private File validateInputs(String documentPath) throws IOException {
        if (documentPath == null || documentPath.trim().isEmpty()) {
            throw new IOException("[ERROR] Document path cannot be null or empty");
        }

        File pdfFile = new File(documentPath);
        if (!pdfFile.exists() || !pdfFile.canRead()) {
            throw new IOException("[ERROR] PDF file does not exist or cannot be read: " + documentPath);
        }

        return pdfFile;
    }

    /**
     * Extracts visible links from the text content of a page using PDFTextStripper.
     *
     * @param document The PDF document.
     * @param pageNum  The page number to analyze (1-based index).
     * @return A list of visible links.
     * @throws IOException If an error occurs while extracting text.
     */
    private List<String> extractVisibleLinks(PDDocument document, int pageNum) throws IOException {
        PDFTextStripper pdfStripper = new PDFTextStripper();
        pdfStripper.setStartPage(pageNum);
        pdfStripper.setEndPage(pageNum);

        // Use StringBuilder for better memory efficiency
        StringBuilder textBuilder = new StringBuilder();
        textBuilder.append(pdfStripper.getText(document));

        List<String> links = new ArrayList<>();
        Matcher matcher = LINK_PATTERN.matcher(textBuilder);

        while (matcher.find()) {
            links.add(matcher.group());
        }

        // Help GC by clearing the StringBuilder
        textBuilder.setLength(0);
        return links;
    }

    /**
     * Extracts embedded links (e.g., hover links) from annotations on a page.
     *
     * @param page The PDF page to analyze.
     * @return A list of embedded links.
     * @throws IOException If an error occurs while processing annotations.
     */
    private List<String> extractEmbeddedLinks(PDPage page) throws IOException {
        List<String> embeddedLinks = new ArrayList<>();
        List<PDAnnotation> annotations = page.getAnnotations();

        for (PDAnnotation annotation : annotations) {
            if (annotation instanceof PDAnnotationLink linkAnnotation) {
                processLinkAnnotation(linkAnnotation, embeddedLinks);
            }
        }

        return embeddedLinks;
    }

    private void processLinkAnnotation(PDAnnotationLink linkAnnotation, List<String> embeddedLinks) {
        COSBase action = linkAnnotation.getCOSObject().getDictionaryObject("A");
        if (action instanceof COSDictionary actionDict) {
            String uri = actionDict.getString("URI");
            if (uri != null && !uri.isBlank()) {
                embeddedLinks.add(uri);
            }
        }
    }

    /**
     * Saves the extracted links for a specific page to a text file.
     *
     * @param pageLinks A HashMap containing page numbers and their links.
     * @param outputDir The directory where the file will be saved.
     * @param pageNum   The page number for which links are being saved.
     * @throws IOException If the file cannot be written.
     */
    private void saveLinksToFile(HashMap<Integer, List<String>> pageLinks, String outputDir, int pageNum) throws IOException {
        String sanitizedPageName = FolderManager.sanitizeFolderName("Page_" + pageNum);
        File outputFile = new File(outputDir, sanitizedPageName + "_links.txt");

        List<String> links = pageLinks.get(pageNum);
        if (links != null && !links.isEmpty()) {
            // Create a PDF summary with formatted links
            File pdfSummary = new File(outputDir, sanitizedPageName + "_links.pdf");
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    float margin = 50;
                    float yStart = page.getMediaBox().getHeight() - margin;
                    float titleFontSize = 14;
                    float textFontSize = 12;
                    
                    // Use bold font for title
                    PDType0Font boldFont = getFont(document, true);
                    PDType0Font regularFont = getFont(document, false);
                    
                    // Add title with bold font
                    contentStream.beginText();
                    contentStream.setFont(boldFont, titleFontSize);
                    contentStream.newLineAtOffset(margin, yStart);
                    contentStream.showText("Links found on page " + pageNum);
                    contentStream.endText();
                    
                    // Add links with regular font
                    float y = yStart - 25;
                    contentStream.setFont(regularFont, textFontSize);
                    
                    for (String link : links) {
                        if (y < margin) {
                            // Create new page if needed
                            contentStream.close();
                            page = new PDPage();
                            document.addPage(page);
                            contentStream.setFont(regularFont, textFontSize);
                            y = yStart;
                        }
                        
                        contentStream.beginText();
                        contentStream.newLineAtOffset(margin, y);
                        contentStream.showText(link);
                        contentStream.endText();
                        y -= textFontSize + 5;
                    }
                }
                document.save(pdfSummary);
            }

            // Also save as text file for backup
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                for (String link : links) {
                    writer.write(link);
                    writer.newLine();
                }
            }
            System.out.println("[INFO] Saved " + links.size() + " links for page " + pageNum + " in both PDF and TXT format");
        }
    }
}
