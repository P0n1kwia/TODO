package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import pl.mojrytm.model.Counter;
import pl.mojrytm.ui.CounterForm.Problem;

class CounterFormTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 24);

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void blankNameIsRejected(String name) {
        assertEquals(Optional.of(Problem.NAME_EMPTY), new CounterForm(name, TODAY, TODAY).problem());
    }

    @Test
    void missingStartIsRejected() {
        assertEquals(Optional.of(Problem.START_MISSING), new CounterForm("Bez słodyczy", null, TODAY).problem());
    }

    @Test
    void startInTheFutureIsRejectedButTodayAndThePastAreFine() {
        assertEquals(Optional.of(Problem.START_IN_FUTURE),
                new CounterForm("Bez słodyczy", TODAY.plusDays(1), TODAY).problem());
        assertEquals(Optional.empty(), new CounterForm("Bez słodyczy", TODAY, TODAY).problem());
        assertEquals(Optional.empty(), new CounterForm("Bez słodyczy", TODAY.minusYears(3), TODAY).problem());
    }

    @Test
    void theNameIsReportedBeforeTheStart() {
        assertEquals(Optional.of(Problem.NAME_EMPTY), new CounterForm("", null, TODAY).problem());
    }

    @Test
    void buildsACounterWithATrimmedName() {
        Counter counter = new CounterForm("  Bez słodyczy ", TODAY.minusDays(5), TODAY).toCounter("id");

        assertEquals(new Counter("id", "Bez słodyczy", TODAY.minusDays(5)), counter);
    }

    @Test
    void anInvalidFormCannotBeTurnedIntoACounter() {
        assertThrows(IllegalStateException.class, () -> new CounterForm("", TODAY, TODAY).toCounter("id"));
    }

    @ParameterizedTest
    @EnumSource(Problem.class)
    void everyProblemHasAMessage(Problem problem) {
        assertTrue(Messages.has("counters.form.problem." + problem.name()));
    }
}
