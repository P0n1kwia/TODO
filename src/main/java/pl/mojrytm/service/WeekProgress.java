package pl.mojrytm.service;

import java.util.OptionalInt;

/**
 * Wykonanie tygodnia: zaliczone / oczekiwane.
 *
 * @param done     zaliczone dni
 * @param expected ile oczekujemy; 0 oznacza tydzień pomijany (pauza, przed założeniem, przyszłość)
 */
public record WeekProgress(int done, int expected) {

    public boolean completed() {
        return expected > 0 && done >= expected;
    }

    /** Procent wykonania (0–100, nie więcej niż 100); pusty, gdy nic nie oczekujemy. */
    public OptionalInt percent() {
        if (expected == 0) {
            return OptionalInt.empty();
        }
        double ratio = Math.min(1.0, (double) done / expected);
        return OptionalInt.of((int) Math.round(ratio * 100));
    }
}
