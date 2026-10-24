package pl.mojrytm.ui;

import java.util.List;
import pl.mojrytm.model.Note;
import pl.mojrytm.service.Tags;

/**
 * Treść notatki w postaci porównywalnej: tekst bez spacji na brzegach, znormalizowane tagi i nawyk.
 * Pusty tekst oznacza "nie ma notatki" ({@link #EMPTY}), więc tagi i nawyk bez tekstu nie liczą się jako zmiana.
 * Służy do wykrywania niezapisanych zmian bez JavaFX.
 */
record NoteState(String text, List<String> tags, String habitId) {

    static final NoteState EMPTY = new NoteState("", List.of(), null);

    /** Stan zapisanej notatki; brak notatki to {@link #EMPTY}. */
    static NoteState of(Note note) {
        return note == null ? EMPTY : new NoteState(note.text().strip(), note.tags(), note.habitId());
    }

    /** Stan pól formularza (tagi jako tekst po przecinku). */
    static NoteState ofFields(String text, String tagsText, String habitId) {
        String stripped = text == null ? "" : text.strip();
        return stripped.isEmpty() ? EMPTY : new NoteState(stripped, Tags.parse(tagsText), habitId);
    }

    boolean isEmpty() {
        return text.isEmpty();
    }
}
