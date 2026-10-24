package pl.mojrytm.ui;

import java.time.LocalDate;
import java.util.Optional;
import pl.mojrytm.model.Pause;

/** Wartości z formularza pauzy wraz z walidacją i zamianą na {@link Pause} (bez JavaFX, żeby dało się testować). */
record PauseForm(LocalDate from, LocalDate to, String reason) {

    enum Problem {
        FROM_MISSING,
        TO_MISSING,
        END_BEFORE_START
    }

    /** Pierwszy znaleziony problem albo pusty, gdy formularz jest poprawny. */
    Optional<Problem> problem() {
        if (from == null) {
            return Optional.of(Problem.FROM_MISSING);
        }
        if (to == null) {
            return Optional.of(Problem.TO_MISSING);
        }
        if (to.isBefore(from)) {
            return Optional.of(Problem.END_BEFORE_START);
        }
        return Optional.empty();
    }

    /** Buduje pauzę; formularz musi być poprawny. Pusty powód zapisuje się jako brak powodu. */
    Pause toPause() {
        if (problem().isPresent()) {
            throw new IllegalStateException("Formularz pauzy jest niepoprawny: " + problem().get());
        }
        String trimmed = reason == null ? "" : reason.trim();
        return new Pause(from, to, trimmed.isEmpty() ? null : trimmed);
    }
}
