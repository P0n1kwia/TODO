package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.mojrytm.model.Note;
import pl.mojrytm.model.NoteType;

class NoteStateTest {

    @Test
    void noNoteIsTheEmptyState() {
        assertEquals(NoteState.EMPTY, NoteState.of(null));
        assertTrue(NoteState.EMPTY.isEmpty());
    }

    @Test
    void aBlankTextIsTheEmptyStateWhateverTagsAndHabitAre() {
        assertEquals(NoteState.EMPTY, NoteState.ofFields("  \n ", "sen, praca", "h1"));
        assertEquals(NoteState.EMPTY, NoteState.ofFields(null, null, null));
    }

    @Test
    void fieldsAreStrippedAndTheirTagsNormalized() {
        NoteState state = NoteState.ofFields("  Dobry dzień \n", " Sen, #Energia ,sen", "h1");

        assertEquals(new NoteState("Dobry dzień", List.of("sen", "energia"), "h1"), state);
        assertFalse(state.isEmpty());
    }

    @Test
    void aSavedNoteEqualsTheFieldsItWasSavedFrom() {
        Note saved = new Note("id", LocalDate.of(2026, 10, 24), NoteType.DAILY, "Dobry dzień",
                List.of("sen", "energia"), "h1");

        assertEquals(NoteState.of(saved), NoteState.ofFields("Dobry dzień ", "SEN, energia", "h1"));
    }

    @Test
    void anyDifferenceInTextTagsOrHabitCountsAsAChange() {
        NoteState base = NoteState.ofFields("Tekst", "a", "h1");

        assertFalse(base.equals(NoteState.ofFields("Tekst 2", "a", "h1")));
        assertFalse(base.equals(NoteState.ofFields("Tekst", "a, b", "h1")));
        assertFalse(base.equals(NoteState.ofFields("Tekst", "a", null)));
    }
}
