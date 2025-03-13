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
        String regex = "((https?|ftp)://[\\w.-]+(?:/[\\w\\-._~:/?#@!$&'()*+,;=]*)?)|www\\.[\\w.-]+";

        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            links.add(matcher.group());
        }

        return links;
    }

    /**
     * Dynamically categorizes links based on patterns or keywords in the links themselves.
     *
     * @param links The list of extracted links.
     * @return A mapping of dynamically generated categories to their respective links.
     */
    public static HashMap<String, List<String>> categorizeLinksDynamically(List<String> links) {
        HashMap<String, List<String>> categorizedLinks = new HashMap<>();

        for (String link : links) {
            String category = inferCategoryFromLink(link);
            categorizedLinks.computeIfAbsent(category, _ignored -> new ArrayList<>()).add(link);
        }

        return categorizedLinks;
    }

    /**
     * Infers a category from a link based on its domain, subdomain, or query parameters.
     *
     * @param link The link to analyze.
     * @return The inferred category.
     */
    private static String inferCategoryFromLink(String link) {
        if (link.contains("github") || link.contains("gitlab") || link.contains("bitbucket")) {
            return "Version Control";
        } else if (link.contains("aws") || link.contains("azure") || link.contains("google-cloud")) {
            return "Cloud Platforms";
        } else if (link.contains("kubernetes") || link.contains("docker") || link.contains("helm")) {
            return "Containerization";
        } else if (link.contains("owasp") || link.contains("security") || link.contains("snyk")) {
            return "DevSecOps";
        } else if (link.contains("tensorflow") || link.contains("pytorch") || link.contains("ml")) {
            return "Machine Learning";
        } else if (link.contains("news") || link.contains("blog") || link.contains("articles")) {
            return "Media/News";
        } else {
            return "Dynamic"; // New dynamic category creation
        }
    }

    /**
     * Combines extraction and dynamic categorization of links in one method.
     *
     * @param text The text to analyze.
     * @return A mapping of dynamically generated categories to their respective links.
     */
    public static HashMap<String, List<String>> extractAndCategorizeLinks(String text) {
        List<String> links = extractLinks(text);
        return categorizeLinksDynamically(links);
    }
}
