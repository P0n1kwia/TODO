package pl.mojrytm.model;

import java.time.LocalDate;

/** Check-in nastroju i energii; jeden na dzień. */
public record CheckIn(LocalDate date, int mood, int energy) {

    public CheckIn {
        Checks.requireNonNull(date, "date");
        Checks.requireRange(mood, 1, 5, "mood");
        Checks.requireRange(energy, 1, 5, "energy");
    }
}
