package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pl.mojrytm.model.Pause;
import pl.mojrytm.ui.PauseForm.Problem;

class PauseFormTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 24);

    @Test
    void aOneDayPauseIsValid() {
        PauseForm form = new PauseForm(DAY, DAY, "choroba");

        assertEquals(Optional.empty(), form.problem());
        assertEquals(new Pause(DAY, DAY, "choroba"), form.toPause());
    }

    @Test
    void missingDatesAreReportedStartFirst() {
        assertEquals(Optional.of(Problem.FROM_MISSING), new PauseForm(null, null, "").problem());
        assertEquals(Optional.of(Problem.TO_MISSING), new PauseForm(DAY, null, "").problem());
    }

    @Test
    void theEndMustNotBeBeforeTheStart() {
        assertEquals(Optional.of(Problem.END_BEFORE_START), new PauseForm(DAY, DAY.minusDays(1), "").problem());
    }

    @Test
    void pastAndFuturePausesAreBothAllowed() {
        assertEquals(Optional.empty(), new PauseForm(DAY.minusDays(30), DAY.minusDays(25), "").problem());
        assertEquals(Optional.empty(), new PauseForm(DAY.plusDays(30), DAY.plusDays(40), "").problem());
    }

    @Test
    void theReasonIsTrimmedAndBlankMeansNoReason() {
        assertEquals("wyjazd", new PauseForm(DAY, DAY, "  wyjazd ").toPause().reason());
        assertEquals(null, new PauseForm(DAY, DAY, "   ").toPause().reason());
        assertEquals(null, new PauseForm(DAY, DAY, null).toPause().reason());
    }

    @Test
    void anInvalidFormCannotBeTurnedIntoAPause() {
        assertThrows(IllegalStateException.class, () -> new PauseForm(null, DAY, "").toPause());
    }

    @ParameterizedTest
    @EnumSource(Problem.class)
    void everyProblemHasAMessage(Problem problem) {
        assertEquals(true, Messages.has("pauses.form.problem." + problem.name()));
    }
}
