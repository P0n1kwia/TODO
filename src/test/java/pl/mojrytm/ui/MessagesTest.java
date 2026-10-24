package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pl.mojrytm.model.NoteType;
import pl.mojrytm.service.EntryRejectedException;

class MessagesTest {

    /** Pełny literał jako klucz; klucze składane z członów (np. error.entry.<powód>) sprawdzają osobne testy. */
    private static final Pattern KEY_USE = Pattern.compile("Messages\\.(?:get|format)\\(\\s*\"([^\"]+)\"\\s*[,)]");

    @Test
    void readsPolishTextsWithDiacritics() {
        assertEquals("Mój Rytm", Messages.get("app.title"));
    }

    @Test
    void formatsArgumentsIntoTheText() {
        assertEquals("Seria: 5 dni", Messages.format("streak.label", "5 dni"));
    }

    @Test
    void everyKeyUsedInTheSourcesExistsInTheBundle() throws IOException {
        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                Matcher matcher = KEY_USE.matcher(Files.readString(file));
                while (matcher.find()) {
                    if (!Messages.has(matcher.group(1))) {
                        missing.add(file.getFileName() + ": " + matcher.group(1));
                    }
                }
            }
        }

        assertTrue(missing.isEmpty(), "brakujące klucze w messages_pl.properties: " + missing);
    }

    @ParameterizedTest
    @EnumSource(EntryRejectedException.Reason.class)
    void everyEntryRejectionReasonHasAMessage(EntryRejectedException.Reason reason) {
        assertTrue(Messages.has("error.entry." + reason.name()), "brak komunikatu dla " + reason);
    }

    @ParameterizedTest
    @EnumSource(NoteType.class)
    void everyNoteTypeHasALabel(NoteType type) {
        assertTrue(Messages.has("journal.list.type." + type.name()), "brak nazwy rodzaju " + type);
    }
}
