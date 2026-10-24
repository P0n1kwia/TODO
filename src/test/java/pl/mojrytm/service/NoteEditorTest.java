package pl.mojrytm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pl.mojrytm.service.Fixtures.LONG_AGO;
import static pl.mojrytm.service.Fixtures.MON;
import static pl.mojrytm.service.Fixtures.NEXT_MON;
import static pl.mojrytm.service.Fixtures.PREV_MON;
import static pl.mojrytm.service.Fixtures.SAT;
import static pl.mojrytm.service.Fixtures.SUN;
import static pl.mojrytm.service.Fixtures.TODAY;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Habit;
import pl.mojrytm.model.HabitKind;
import pl.mojrytm.model.Note;
import pl.mojrytm.model.NoteType;
import pl.mojrytm.model.Schedule;

class NoteEditorTest {

    private final NoteEditor editor = new NoteEditor();

    private static AppData withHabit() {
        return AppData.empty().withHabits(List.of(new Habit("h1", null, "Spacer", HabitKind.CHECK, Schedule.DAILY,
                null, null, null, null, LONG_AGO, false)));
    }

    private static List<Note> notesOf(AppData data, NoteType type) {
        return data.notes().stream().filter(note -> note.type() == type).toList();
    }

    @Test
    void savingADailyNoteAddsItWithTheDateTrimmedTextAndNormalizedTags() {
        AppData data = editor.saveDaily(withHabit(), TODAY, SAT, "  Dobry dzień.\nMało snu.  ",
                List.of(" Sen ", "#Energia", "sen"), "h1");

        Note note = data.notes().get(0);
        assertEquals(NoteType.DAILY, note.type());
        assertEquals(SAT, note.date());
        assertEquals("Dobry dzień.\nMało snu.", note.text());
        assertEquals(List.of("sen", "energia"), note.tags());
        assertEquals("h1", note.habitId());
        assertEquals(36, note.id().length());
    }

    @Test
    void savingAgainForTheSameDayReplacesTheNoteKeepingItsIdAndPlace() {
        AppData first = editor.saveDaily(withHabit(), TODAY, SAT, "Pierwsza wersja", List.of("a"), "h1");
        AppData withOther = editor.saveDaily(first, TODAY, MON, "Inny dzień", List.of(), null);
        String id = first.notes().get(0).id();

        AppData changed = editor.saveDaily(withOther, TODAY, SAT, "Druga wersja", List.of("b"), null);

        assertEquals(2, changed.notes().size());
        Note note = changed.notes().get(0);
        assertEquals(id, note.id());
        assertEquals("Druga wersja", note.text());
        assertEquals(List.of("b"), note.tags());
        assertEquals(null, note.habitId());
        assertEquals("Inny dzień", changed.notes().get(1).text());
    }

    @Test
    void aBlankTextRemovesTheDailyNote() {
        AppData data = editor.saveDaily(AppData.empty(), TODAY, SAT, "Coś", List.of(), null);

        AppData cleared = editor.saveDaily(data, TODAY, SAT, "  \n ", List.of("x"), null);

        assertEquals(List.of(), cleared.notes());
    }

    @Test
    void aBlankTextWithoutAnExistingNoteChangesNothing() {
        AppData data = AppData.empty();

        assertSame(data, editor.saveDaily(data, TODAY, SAT, "", List.of(), null));
    }

    @Test
    void pastDaysAndTodayAreAllowedButNotTheFuture() {
        AppData data = AppData.empty();

        assertEquals(1, editor.saveDaily(data, TODAY, TODAY, "Dziś", List.of(), null).notes().size());
        assertEquals(1, editor.saveDaily(data, TODAY, LONG_AGO, "Dawno", List.of(), null).notes().size());
        assertThrows(IllegalArgumentException.class,
                () -> editor.saveDaily(data, TODAY, TODAY.plusDays(1), "Jutro", List.of(), null));
    }

    @Test
    void aDailyNoteMayOnlyPointToAnExistingHabit() {
        assertThrows(IllegalArgumentException.class,
                () -> editor.saveDaily(withHabit(), TODAY, SAT, "Tekst", List.of(), "nie-ma"));
    }

    @Test
    void aDailyNoteAndAFreeNoteOfTheSameDayCoexist() {
        AppData data = editor.addFree(AppData.empty(), TODAY, "Pomysł", List.of(), null);

        AppData both = editor.saveDaily(data, TODAY, TODAY, "Wpis", List.of(), null);

        assertEquals(2, both.notes().size());
    }

    @Test
    void aWeeklyReviewIsStoredOnTheMondayOfThatWeek() {
        AppData data = editor.saveWeeklyReview(AppData.empty(), TODAY, SAT, "Tydzień był dobry");

        Note note = data.notes().get(0);
        assertEquals(NoteType.WEEKLY_REVIEW, note.type());
        assertEquals(MON, note.date());
        assertEquals(List.of(), note.tags());
        assertEquals(null, note.habitId());
    }

    @Test
    void anyDayOfTheWeekReplacesTheSameReview() {
        AppData first = editor.saveWeeklyReview(AppData.empty(), TODAY, MON, "A");

        AppData second = editor.saveWeeklyReview(first, TODAY, SUN, "B");

        assertEquals(1, second.notes().size());
        assertEquals("B", second.notes().get(0).text());
        assertEquals(first.notes().get(0).id(), second.notes().get(0).id());
    }

    @Test
    void reviewsOfDifferentWeeksAreSeparate() {
        AppData data = editor.saveWeeklyReview(editor.saveWeeklyReview(AppData.empty(), TODAY, SAT, "Ten"),
                TODAY, PREV_MON, "Poprzedni");

        assertEquals(List.of(MON, PREV_MON), data.notes().stream().map(Note::date).toList());
    }

    @Test
    void aBlankReviewIsRemoved() {
        AppData data = editor.saveWeeklyReview(AppData.empty(), TODAY, SAT, "Tekst");

        assertEquals(List.of(), editor.saveWeeklyReview(data, TODAY, SAT, " ").notes());
    }

    @Test
    void theCurrentWeekMayBeReviewedButNotAFutureOne() {
        AppData data = AppData.empty();

        assertEquals(1, editor.saveWeeklyReview(data, MON, SUN, "Na początku tygodnia").notes().size());
        assertThrows(IllegalArgumentException.class, () -> editor.saveWeeklyReview(data, TODAY, NEXT_MON, "Za wcześnie"));
    }

    @Test
    void aWeeklyReviewDoesNotTouchDailyNotes() {
        AppData data = editor.saveDaily(AppData.empty(), TODAY, MON, "Wpis z poniedziałku", List.of(), null);

        AppData both = editor.saveWeeklyReview(data, TODAY, MON, "Przegląd");

        assertEquals(1, notesOf(both, NoteType.DAILY).size());
        assertEquals(1, notesOf(both, NoteType.WEEKLY_REVIEW).size());
    }

    @Test
    void freeNotesAreAddedWithTodaysDateAndCanRepeat() {
        AppData data = editor.addFree(editor.addFree(withHabit(), TODAY, "  Pierwsza ", List.of("Pomysł"), "h1"),
                TODAY, "Druga", List.of(), null);

        assertEquals(2, data.notes().size());
        Note first = data.notes().get(0);
        assertEquals(NoteType.FREE, first.type());
        assertEquals(TODAY, first.date());
        assertEquals("Pierwsza", first.text());
        assertEquals(List.of("pomysł"), first.tags());
        assertEquals("h1", first.habitId());
    }

    @Test
    void aBlankFreeNoteOrOneForAnUnknownHabitIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> editor.addFree(AppData.empty(), TODAY, "  ", List.of(), null));
        assertThrows(IllegalArgumentException.class,
                () -> editor.addFree(withHabit(), TODAY, "Tekst", List.of(), "nie-ma"));
    }

    @Test
    void removeDeletesOnlyThatNote() {
        AppData data = editor.addFree(editor.addFree(AppData.empty(), TODAY, "A", List.of(), null),
                TODAY, "B", List.of(), null);

        AppData removed = editor.remove(data, data.notes().get(0).id());

        assertEquals(List.of("B"), removed.notes().stream().map(Note::text).toList());
    }

    @Test
    void removingAnUnknownNoteIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> editor.remove(AppData.empty(), "x"));
    }
}
