package pl.mojrytm.model;

import java.time.LocalDate;

/** Pauza (choroba, wyjazd) obejmująca wszystkie nawyki; oba końce włącznie. */
public record Pause(LocalDate from, LocalDate to, String reason) {

    public Pause {
        Checks.requireNonNull(from, "from");
        Checks.requireNonNull(to, "to");
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("Koniec pauzy (" + to + ") jest przed początkiem (" + from + ")");
        }
    }
}
