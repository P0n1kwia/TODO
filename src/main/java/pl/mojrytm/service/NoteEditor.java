package pl.mojrytm.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Ids;
import pl.mojrytm.model.Note;
import pl.mojrytm.model.NoteType;

/**
 * Zmiany na notatkach. Wpis dzienny jest jeden na dzień, przegląd tygodnia jeden na tydzień (data to
 * poniedziałek); zapis zastępuje poprzedni, a pusty tekst go usuwa. Wolnych notatek może być dowolnie
 * wiele, mają dzień dodania i można je tylko usunąć. Wpisu ani przeglądu z przyszłości nie ma sensu pisać.
 * Naruszenia to błąd programisty ({@link IllegalArgumentException}): UI sprawdza je wcześniej.
 */
public final class NoteEditor {

    /**
     * Zapisuje wpis dzienny z dnia {@code date}.
     *
     * @param today   dzień logiczny
     * @param habitId nawyk, którego wpis dotyczy; null albo istniejący nawyk
     */
    public AppData saveDaily(AppData data, LocalDate today, LocalDate date, String text, List<String> tags,
            String habitId) {
        if (date.isAfter(today)) {
            throw new IllegalArgumentException("Wpisu dziennego nie można napisać na dzień z przyszłości: " + date);
        }
        requireHabitExists(data, habitId);
        return upsert(data, NoteType.DAILY, date, text, Tags.normalize(tags), habitId);
    }

    /** Zapisuje przegląd tygodnia zawierającego {@code anyDayOfWeek} (bieżący tydzień jest dozwolony). */
    public AppData saveWeeklyReview(AppData data, LocalDate today, LocalDate anyDayOfWeek, String text) {
        LocalDate monday = Weeks.startOf(anyDayOfWeek);
        if (monday.isAfter(today)) {
            throw new IllegalArgumentException("Przeglądu przyszłego tygodnia nie można napisać: " + monday);
        }
        return upsert(data, NoteType.WEEKLY_REVIEW, monday, text, List.of(), null);
    }

    /** Dodaje wolną notatkę z dzisiejszą datą. */
    public AppData addFree(AppData data, LocalDate today, String text, List<String> tags, String habitId) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Notatka nie może być pusta");
        }
        requireHabitExists(data, habitId);
        List<Note> notes = new ArrayList<>(data.notes());
        notes.add(new Note(Ids.newId(), today, NoteType.FREE, text.strip(), Tags.normalize(tags), habitId));
        return data.withNotes(notes);
    }

    public AppData remove(AppData data, String noteId) {
        List<Note> notes = new ArrayList<>(data.notes());
        if (!notes.removeIf(note -> note.id().equals(noteId))) {
            throw new IllegalArgumentException("Nie ma notatki o id: " + noteId);
        }
        return data.withNotes(notes);
    }

    private static AppData upsert(AppData data, NoteType type, LocalDate date, String text, List<String> tags,
            String habitId) {
        List<Note> notes = new ArrayList<>(data.notes());
        int index = indexOf(notes, type, date);
        if (text == null || text.isBlank()) {
            if (index < 0) {
                return data;
            }
            notes.remove(index);
            return data.withNotes(notes);
        }
        if (index < 0) {
            notes.add(new Note(Ids.newId(), date, type, text.strip(), tags, habitId));
        } else {
            notes.set(index, new Note(notes.get(index).id(), date, type, text.strip(), tags, habitId));
        }
        return data.withNotes(notes);
    }

    private static int indexOf(List<Note> notes, NoteType type, LocalDate date) {
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).type() == type && notes.get(i).date().equals(date)) {
                return i;
            }
        }
        return -1;
    }

    private static void requireHabitExists(AppData data, String habitId) {
        if (habitId != null && data.habits().stream().noneMatch(habit -> habit.id().equals(habitId))) {
            throw new IllegalArgumentException("Nie ma nawyku o id: " + habitId);
        }
    }
}
