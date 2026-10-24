package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pl.mojrytm.ui.DailyNoteForm.Problem;

class DailyNoteFormTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 24);

    @Test
    void todayAndThePastAreFine() {
        assertEquals(Optional.empty(), new DailyNoteForm(TODAY, TODAY).problem());
        assertEquals(Optional.empty(), new DailyNoteForm(TODAY.minusYears(2), TODAY).problem());
    }

    @Test
    void tomorrowIsRejected() {
        assertEquals(Optional.of(Problem.DATE_IN_FUTURE), new DailyNoteForm(TODAY.plusDays(1), TODAY).problem());
    }

    @Test
    void aMissingDateIsRejected() {
        assertEquals(Optional.of(Problem.DATE_MISSING), new DailyNoteForm(null, TODAY).problem());
    }

    @ParameterizedTest
    @EnumSource(Problem.class)
    void everyProblemHasAMessage(Problem problem) {
        assertTrue(Messages.has("journal.daily.problem." + problem.name()));
    }
}
