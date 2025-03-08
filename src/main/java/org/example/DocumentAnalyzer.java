package org.example;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentAnalyzer {

    // Regex to identify visible links in text
    private static final Pattern LINK_PATTERN = Pattern.compile("\\bhttps?://[\\w\\-._~:/?#@!$&'()*+,;=%]+\\b");

    /**
     * Analyze the PDF document to extract both visible and embedded links.
     *
     * @param documentPath The path of the PDF document.
     * @return A HashMap with page numbers as keys and a list of extracted links as values.
     * @throws IOException If the document cannot be read or processed.
     */
    public HashMap<Integer, List<String>> analyzeDocument(String documentPath) throws IOException {
        HashMap<Integer, List<String>> pageLinks = new HashMap<>();

        // Validate the PDF file
        File pdfFile = new File(documentPath);
        if (!pdfFile.exists() || !pdfFile.canRead()) {
            throw new IOException("PDF file does not exist or cannot be read: " + documentPath);
        }

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            int totalPages = document.getNumberOfPages();

            // Process each page
            for (int pageNum = 0; pageNum < totalPages; pageNum++) {
                PDPage page = document.getPage(pageNum);
                Set<String> links = new LinkedHashSet<>(); // Use LinkedHashSet to avoid duplicates while preserving order

                // Extract visible links from text
                links.addAll(extractVisibleLinks(document, pageNum + 1));

                // Extract embedded links from annotations
                links.addAll(extractEmbeddedLinks(page));

                // Save the links if not empty
                if (!links.isEmpty()) {
                    pageLinks.put(pageNum + 1, new ArrayList<>(links)); // Convert LinkedHashSet to List
                }
            }
        }

        return pageLinks;
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
        org.apache.pdfbox.text.PDFTextStripper pdfStripper = new org.apache.pdfbox.text.PDFTextStripper();
        pdfStripper.setStartPage(pageNum);
        pdfStripper.setEndPage(pageNum);

        String pageText = pdfStripper.getText(document);
        List<String> links = new ArrayList<>();
        Matcher matcher = LINK_PATTERN.matcher(pageText);

        while (matcher.find()) {
            links.add(matcher.group());
        }

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

        // Check for link annotations on the page
        List<PDAnnotation> annotations = page.getAnnotations();
        for (PDAnnotation annotation : annotations) {
            if (annotation instanceof PDAnnotationLink linkAnnotation) {
                COSBase action = linkAnnotation.getCOSObject().getDictionaryObject("A"); // Get the action dictionary
                if (action instanceof COSDictionary actionDict) {
                    String uri = actionDict.getString("URI"); // Extract URI from the dictionary
                    if (uri != null) {
                        embeddedLinks.add(uri);
                    }
                }
            }
        }

        return embeddedLinks;
    }
}
