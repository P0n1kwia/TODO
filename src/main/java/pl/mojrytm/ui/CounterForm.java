package pl.mojrytm.ui;

import java.time.LocalDate;
import java.util.Optional;
import pl.mojrytm.model.Counter;

/**
 * Wartości z formularza licznika wraz z walidacją i zamianą na {@link Counter} (bez JavaFX, żeby dało się
 * testować). {@code today} to dzień logiczny, bo start w przyszłości nie ma sensu.
 */
record CounterForm(String name, LocalDate start, LocalDate today) {

    enum Problem {
        NAME_EMPTY,
        START_MISSING,
        START_IN_FUTURE
    }

    /** Pierwszy znaleziony problem albo pusty, gdy formularz jest poprawny. */
    Optional<Problem> problem() {
        if (name == null || name.isBlank()) {
            return Optional.of(Problem.NAME_EMPTY);
        }
        if (start == null) {
            return Optional.of(Problem.START_MISSING);
        }
        if (start.isAfter(today)) {
            return Optional.of(Problem.START_IN_FUTURE);
        }
        return Optional.empty();
    }

    /** Buduje licznik; formularz musi być poprawny. */
    Counter toCounter(String id) {
        if (problem().isPresent()) {
            throw new IllegalStateException("Formularz licznika jest niepoprawny: " + problem().get());
        }
        return new Counter(id, name.trim(), start);
    }
}
