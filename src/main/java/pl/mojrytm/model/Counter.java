package pl.mojrytm.model;

import java.time.LocalDate;

/** Licznik „dni od" lub „dni bez". */
public record Counter(String id, String name, LocalDate startDate) {

    public Counter {
        Checks.requireText(id, "id");
        Checks.requireText(name, "name");
        Checks.requireNonNull(startDate, "startDate");
    }
}
