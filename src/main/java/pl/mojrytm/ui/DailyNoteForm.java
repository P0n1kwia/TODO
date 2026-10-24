package pl.mojrytm.ui;

import java.time.LocalDate;
import java.util.Optional;

/** Dzień wpisu dziennego z walidacją (bez JavaFX, żeby dało się testować). {@code today} to dzień logiczny. */
record DailyNoteForm(LocalDate date, LocalDate today) {

    enum Problem {
        DATE_MISSING,
        DATE_IN_FUTURE
    }

    /** Pierwszy znaleziony problem albo pusty, gdy dzień jest poprawny. */
    Optional<Problem> problem() {
        if (date == null) {
            return Optional.of(Problem.DATE_MISSING);
        }
        if (date.isAfter(today)) {
            return Optional.of(Problem.DATE_IN_FUTURE);
        }
        return Optional.empty();
    }
}
