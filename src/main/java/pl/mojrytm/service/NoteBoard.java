package pl.mojrytm.service;

import java.text.Collator;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Note;
import pl.mojrytm.model.NoteType;

/** Odczyt notatek dla widoków: wpis i przegląd z danego dnia, lista najnowszych (z filtrem tagu), użyte tagi. */
public final class NoteBoard {

    private static final Locale POLISH = Locale.forLanguageTag("pl-PL");

    public Optional<Note> dailyNote(AppData data, LocalDate date) {
        return find(data, NoteType.DAILY, date);
    }

    /** Przegląd tygodnia zawierającego {@code anyDayOfWeek}. */
    public Optional<Note> weeklyReview(AppData data, LocalDate anyDayOfWeek) {
        return find(data, NoteType.WEEKLY_REVIEW, Weeks.startOf(anyDayOfWeek));
    }

    /**
     * Najnowsze notatki: od najnowszej daty, a przy tej samej dacie później dodane pierwsze.
     *
     * @param tag   tylko notatki z tym tagiem (wielkość liter bez znaczenia); null albo pusty to wszystkie
     * @param limit największa liczba wyników
     */
    public List<Note> recent(AppData data, String tag, int limit) {
        List<String> wanted = tag == null ? List.of() : Tags.normalize(List.of(tag));
        List<Note> newestFirst = data.notes().reversed();
        return newestFirst.stream()
                .filter(note -> wanted.isEmpty() || note.tags().contains(wanted.get(0)))
                .sorted(Comparator.comparing(Note::date).reversed())
                .limit(limit)
                .toList();
    }

    /** Wszystkie użyte tagi, bez powtórzeń, alfabetycznie po polsku. */
    public List<String> tags(AppData data) {
        Collator collator = Collator.getInstance(POLISH);
        return data.notes().stream()
                .flatMap(note -> note.tags().stream())
                .distinct()
                .sorted(collator)
                .toList();
    }

    private static Optional<Note> find(AppData data, NoteType type, LocalDate date) {
        return data.notes().stream()
                .filter(note -> note.type() == type && note.date().equals(date))
                .findFirst();
    }
}
