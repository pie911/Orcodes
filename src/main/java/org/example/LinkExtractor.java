package org.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkExtractor {
    private static final Pattern URL_PATTERN = Pattern.compile(
        "((https?|ftp)://[\\w.-]+(?:/[\\w\\-._~:/?#@!$&'()*+,;=]*)?)|www\\.[\\w.-]+",
        Pattern.CASE_INSENSITIVE
    );

    private static final Map<String, String> CATEGORY_PATTERNS = Map.of(
        "Version Control", ".*(github|gitlab|bitbucket).*",
        "Cloud Platforms", ".*(aws|azure|google-cloud).*",
        "Containerization", ".*(kubernetes|docker|helm).*",
        "DevSecOps", ".*(owasp|security|snyk).*",
        "Machine Learning", ".*(tensorflow|pytorch|ml).*",
        "Media/News", ".*(news|blog|articles).*"
    );

    private static final Map<String, Pattern> COMPILED_PATTERNS = new HashMap<>();

    static {
        CATEGORY_PATTERNS.forEach((category, pattern) -> 
            COMPILED_PATTERNS.put(category, Pattern.compile(pattern, Pattern.CASE_INSENSITIVE))
        );
    }

    public static List<String> extractLinks(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> links = new ArrayList<>();
        Matcher matcher = URL_PATTERN.matcher(text);

        while (matcher.find()) {
            links.add(matcher.group());
        }

        return links;
    }

    public static HashMap<String, List<String>> categorizeLinksDynamically(List<String> links) {
        HashMap<String, List<String>> categorizedLinks = new HashMap<>();

        for (String link : links) {
            String category = inferCategoryFromLink(link);
            // Replace computeIfAbsent with putIfAbsent and get to avoid unused lambda parameter
            if (categorizedLinks.putIfAbsent(category, new ArrayList<>(List.of(link))) != null) {
                categorizedLinks.get(category).add(link);
            }
        }

        return categorizedLinks;
    }

    private static String inferCategoryFromLink(String link) {
        return COMPILED_PATTERNS.entrySet().stream()
            .filter(entry -> entry.getValue().matcher(link).matches())
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse("Other");
    }

    public static HashMap<String, List<String>> extractAndCategorizeLinks(String text) {
        return categorizeLinksDynamically(extractLinks(text));
    }

    /**
     * Gets the current category patterns for testing or modification.
     * @return An unmodifiable map of category patterns
     */
    public static Map<String, String> getCategoryPatterns() {
        return Collections.unmodifiableMap(CATEGORY_PATTERNS);
    }
}
