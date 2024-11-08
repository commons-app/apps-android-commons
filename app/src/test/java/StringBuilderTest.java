import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StringBuilderTest {
    @Test
    void testFallbackDescriptionBuilder() {
        String secondaryLanguages = "en,fr,es";  // Example secondary languages
        String[] secondaryLanguageArray = secondaryLanguages.split(",\\s*");

        StringBuilder fallBackDescription = new StringBuilder();
        for (int i = 0; i < secondaryLanguageArray.length; i++) {
            fallBackDescription.append("OPTIONAL {?item schema:description ?itemDescriptionPreferredLanguage_")
                .append(i + 1)  // Unique identifier for each fallback
                .append(". FILTER (lang(?itemDescriptionPreferredLanguage_")
                .append(i + 1)
                .append(") = \"")
                .append(secondaryLanguageArray[i])  // Use the secondary language code
                .append("\")}\n");
        }

        String expected = "OPTIONAL {?item schema:description ?itemDescriptionPreferredLanguage_1. FILTER (lang(?itemDescriptionPreferredLanguage_1) = \"en\")}\n" +
            "OPTIONAL {?item schema:description ?itemDescriptionPreferredLanguage_2. FILTER (lang(?itemDescriptionPreferredLanguage_2) = \"fr\")}\n" +
            "OPTIONAL {?item schema:description ?itemDescriptionPreferredLanguage_3. FILTER (lang(?itemDescriptionPreferredLanguage_3) = \"es\")}\n";

        assertEquals(expected, fallBackDescription.toString());
    }

    @Test
    void testFallbackLabelBuilder() {
        String secondaryLanguages = "en,fr,es";  // Example secondary languages
        String[] secondaryLanguageArray = secondaryLanguages.split(",\\s*");

        StringBuilder fallbackLabel = new StringBuilder();
        for (int i = 0; i < secondaryLanguageArray.length; i++) {
            fallbackLabel.append("OPTIONAL {?item rdfs:label ?itemLabelPreferredLanguage_")
                .append(i + 1)
                .append(". FILTER (lang(?itemLabelPreferredLanguage_")
                .append(i + 1)
                .append(") = \"")
                .append(secondaryLanguageArray[i])
                .append("\")}\n");
        }

        String expected = "OPTIONAL {?item rdfs:label ?itemLabelPreferredLanguage_1. FILTER (lang(?itemLabelPreferredLanguage_1) = \"en\")}\n" +
            "OPTIONAL {?item rdfs:label ?itemLabelPreferredLanguage_2. FILTER (lang(?itemLabelPreferredLanguage_2) = \"fr\")}\n" +
            "OPTIONAL {?item rdfs:label ?itemLabelPreferredLanguage_3. FILTER (lang(?itemLabelPreferredLanguage_3) = \"es\")}\n";

        assertEquals(expected, fallbackLabel.toString());
    }

    @Test
    void testFallbackClassLabelBuilder() {
        String secondaryLanguages = "en,fr,es";  // Example secondary languages
        String[] secondaryLanguageArray = secondaryLanguages.split(",\\s*");

        StringBuilder fallbackClassLabel = new StringBuilder();
        for (int i = 0; i < secondaryLanguageArray.length; i++) {
            fallbackClassLabel.append("OPTIONAL {?class rdfs:label ?classLabelPreferredLanguage_")
                .append(i + 1)
                .append(". FILTER (lang(?classLabelPreferredLanguage_")
                .append(i + 1)
                .append(") = \"")
                .append(secondaryLanguageArray[i])
                .append("\")}\n");
        }

        String expected = "OPTIONAL {?class rdfs:label ?classLabelPreferredLanguage_1. FILTER (lang(?classLabelPreferredLanguage_1) = \"en\")}\n" +
            "OPTIONAL {?class rdfs:label ?classLabelPreferredLanguage_2. FILTER (lang(?classLabelPreferredLanguage_2) = \"fr\")}\n" +
            "OPTIONAL {?class rdfs:label ?classLabelPreferredLanguage_3. FILTER (lang(?classLabelPreferredLanguage_3) = \"es\")}\n";

        assertEquals(expected, fallbackClassLabel.toString());
    }
}

