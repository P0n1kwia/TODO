package pl.mojrytm.service;

import java.util.Optional;
import java.util.OptionalInt;
import pl.mojrytm.model.Area;
import pl.mojrytm.model.WheelRating;

/** Obszar z jego ostatnią oceną i oceną poprzednią (z wcześniejszego dnia), jeśli są. */
public record AreaRating(Area area, Optional<WheelRating> last, Optional<WheelRating> previous) {

    /** Ocena, od której zaczyna suwak, gdy obszaru jeszcze nie oceniano. */
    public static final int DEFAULT_SCORE = 5;

    /** Zmiana ostatniej oceny względem poprzedniej; pusta, gdy brakuje którejś z nich. */
    public OptionalInt change() {
        if (last.isEmpty() || previous.isEmpty()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(last.get().score() - previous.get().score());
    }

    /** Ocena do pokazania na suwaku: ostatnia (czyli dzisiejsza, jeśli już oceniono) albo domyślna. */
    public int suggestedScore() {
        return last.map(WheelRating::score).orElse(DEFAULT_SCORE);
    }
}
