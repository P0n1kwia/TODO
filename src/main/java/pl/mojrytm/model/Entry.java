package pl.mojrytm.model;

import java.time.LocalDate;

/**
 * Wpis wykonania nawyku w danym dniu logicznym.
 *
 * @param value   ilość (dla CHECK umownie 1)
 * @param minimal czy zaliczone wersją minimalną
 */
public record Entry(String id, String habitId, LocalDate date, double value, boolean minimal) {

    public Entry {
        Checks.requireText(id, "id");
        Checks.requireText(habitId, "habitId");
        Checks.requireNonNull(date, "date");
        if (!(value >= 0) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Pole 'value' musi być liczbą nieujemną, jest: " + value);
        }
    }
}
