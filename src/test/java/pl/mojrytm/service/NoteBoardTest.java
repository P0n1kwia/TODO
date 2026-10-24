package pl.mojrytm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.mojrytm.service.Fixtures.MON;
import static pl.mojrytm.service.Fixtures.SAT;
import static pl.mojrytm.service.Fixtures.SUN;
import static pl.mojrytm.service.Fixtures.TUE;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Note;
import pl.mojrytm.model.NoteType;

class NoteBoardTest {

    private final NoteBoard board = new NoteBoard();

    private static Note note(String id, LocalDate date, NoteType type, String... tags) {
        return new Note(id, date, type, "tekst " + id, List.of(tags), null);
    }

    private static AppData with(Note... notes) {
        return AppData.empty().withNotes(List.of(notes));
    }

    private static List<String> ids(List<Note> notes) {
        return notes.stream().map(Note::id).toList();
    }

    @Test
    void dailyNoteIsFoundByItsDateAndType() {
        AppData data = with(note("free", SAT, NoteType.FREE), note("daily", SAT, NoteType.DAILY),
                note("review", MON, NoteType.WEEKLY_REVIEW));

        assertEquals("daily", board.dailyNote(data, SAT).orElseThrow().id());
        assertEquals(Optional.empty(), board.dailyNote(data, MON));
    }

    @Test
    void weeklyReviewIsFoundFromAnyDayOfItsWeek() {
        AppData data = with(note("review", MON, NoteType.WEEKLY_REVIEW), note("daily", SUN, NoteType.DAILY));

        assertEquals("review", board.weeklyReview(data, SUN).orElseThrow().id());
        assertEquals("review", board.weeklyReview(data, MON).orElseThrow().id());
        assertEquals(Optional.empty(), board.weeklyReview(data, MON.minusDays(1)));
    }

    @Test
    void recentGoesFromTheNewestDateAndLaterAddedFirstWithinADay() {
        AppData data = with(note("old", MON, NoteType.DAILY), note("a", SAT, NoteType.FREE),
                note("b", SAT, NoteType.FREE), note("mid", TUE, NoteType.DAILY));

        assertEquals(List.of("b", "a", "mid", "old"), ids(board.recent(data, null, 10)));
    }

    @Test
    void recentIsLimited() {
        AppData data = with(note("a", MON, NoteType.DAILY), note("b", TUE, NoteType.DAILY),
                note("c", SAT, NoteType.DAILY));

        assertEquals(List.of("c", "b"), ids(board.recent(data, null, 2)));
        assertEquals(List.of(), board.recent(data, null, 0));
    }

    @Test
    void recentCanBeFilteredByTagIgnoringCaseAndHash() {
        AppData data = with(note("a", MON, NoteType.DAILY, "sen"), note("b", TUE, NoteType.FREE, "sen", "praca"),
                note("c", SAT, NoteType.DAILY, "praca"));

        assertEquals(List.of("b", "a"), ids(board.recent(data, "SEN", 10)));
        assertEquals(List.of("c", "b"), ids(board.recent(data, "#Praca", 10)));
        assertEquals(List.of(), board.recent(data, "nic", 10));
    }

    @Test
    void aBlankFilterMeansNoFilter() {
        AppData data = with(note("a", MON, NoteType.DAILY, "sen"), note("b", TUE, NoteType.DAILY));

        assertEquals(2, board.recent(data, "  ", 10).size());
        assertEquals(2, board.recent(data, "", 10).size());
    }

    @Test
    void tagsAreUniqueAndSortedAlphabeticallyWithPolishLetters() {
        AppData data = with(note("a", MON, NoteType.DAILY, "zdrowie", "łóżko", "sen"),
                note("b", TUE, NoteType.FREE, "sen", "ćwiczenia", "żółw"));

        assertEquals(List.of("ćwiczenia", "łóżko", "sen", "zdrowie", "żółw"), board.tags(data));
    }

    @Test
    void noNotesGiveNothing() {
        assertEquals(List.of(), board.tags(AppData.empty()));
        assertEquals(List.of(), board.recent(AppData.empty(), null, 10));
        assertTrue(board.dailyNote(AppData.empty(), SAT).isEmpty());
    }
}
