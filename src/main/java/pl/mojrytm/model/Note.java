package pl.mojrytm.model;

import java.time.LocalDate;
import java.util.List;

/** Notatka, opcjonalnie powiązana z nawykiem. */
public record Note(String id, LocalDate date, NoteType type, String text, List<String> tags, String habitId) {

    public Note {
        Checks.requireText(id, "id");
        Checks.requireNonNull(date, "date");
        Checks.requireNonNull(type, "type");
        Checks.requireNonNull(text, "text");
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
