package org.example;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkExtractor {

    /**
     * Extracts all links from a given text using regex.
     *
     * @param text The text to analyze.
     * @return A list of extracted links.
     */
    public static List<String> extractLinks(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> links = new ArrayList<>();
        String regex = "(https?://[\\w.-]+(?:\\.[\\w\\.-]+)+(?:/[\\w\\-._~:/?#\\[\\] @!$&'()*+,;=.]+)?)";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            links.add(matcher.group());
        }

        return links;
    }

    /**
     * Categorizes links using predefined keywords and dynamically updates categories based on document context.
     *
     * @param links            The list of extracted links.
     * @param dynamicCategories A map of category names to their identifying keywords.
     * @return A mapping of category names to their respective links.
     */
    public static HashMap<String, List<String>> categorizeLinks(List<String> links, HashMap<String, List<String>> dynamicCategories) {
        HashMap<String, List<String>> categorizedLinks = new HashMap<>();

        for (String link : links) {
            boolean categorized = false;
            for (var entry : dynamicCategories.entrySet()) {
                String category = entry.getKey();
                List<String> keywords = entry.getValue();

                for (String keyword : keywords) {
                    if (link.toLowerCase().contains(keyword.toLowerCase())) {
                        categorizedLinks.computeIfAbsent(category, k -> new ArrayList<>()).add(link);
                        categorized = true;
                        break;
                    }
                }

                if (categorized) {
                    break;
                }
            }

            // Uncategorized links go into a "General" category
            if (!categorized) {
                categorizedLinks.computeIfAbsent("General", k -> new ArrayList<>()).add(link);
            }
        }

        return categorizedLinks;
    }

    /**
     * Dynamically identifies categories in a file using contextual inference (placeholder for ML).
     *
     * @param text The raw input text.
     * @return A map of identified categories with their associated keywords.
     */
    public static HashMap<String, List<String>> generateDynamicCategories(String text) {
        if (text == null || text.isEmpty()) {
            return new HashMap<>();
        }

        // Predefined categories (can be extended with NLP/ML models like spaCy or BERT)
        HashMap<String, List<String>> categories = new HashMap<>();

        categories.put("Social Media", Arrays.asList("facebook", "twitter", "instagram", "linkedin", "youtube", "reddit"));
        categories.put("E-Commerce", Arrays.asList("amazon", "ebay", "shopify"));
        categories.put("News", Arrays.asList("cnn", "bbc", "nytimes", "reuters"));

        // Placeholder logic for ML-based dynamic category inference
        // Add Named Entity Recognition (NER) or document parsing here to extract context dynamically.

        return categories;
    }

    /**
     * Extracts and categorizes links from the input text dynamically.
     *
     * @param text The input text from which links are to be extracted.
     * @return A categorized map of extracted links.
     */
    public static HashMap<String, List<String>> extractAndCategorizeLinks(String text) {
        List<String> allLinks = extractLinks(text);
        HashMap<String, List<String>> dynamicCategories = generateDynamicCategories(text);
        return categorizeLinks(allLinks, dynamicCategories);
    }
}
