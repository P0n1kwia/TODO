package pl.mojrytm.service;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Tagi notatek: wpisywane po przecinku, zapisywane małymi literami, bez powtórzeń i bez "#". */
public final class Tags {

    private Tags() {
    }

    /** Zamienia tekst "sen, #Energia ,sen" na tagi: [sen, energia]. */
    public static List<String> parse(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return normalize(List.of(text.split(",")));
    }

    /** Przycina, usuwa początkowe "#", zamienia na małe litery, pomija puste i powtórzone (kolejność zostaje). */
    public static List<String> normalize(Collection<String> tags) {
        return tags.stream()
                .flatMap(tag -> tag == null ? Stream.empty() : Stream.of(clean(tag)))
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .toList();
    }

    private static String clean(String tag) {
        String trimmed = tag.trim();
        while (trimmed.startsWith("#")) {
            trimmed = trimmed.substring(1).trim();
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
