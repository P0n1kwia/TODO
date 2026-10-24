package pl.mojrytm.service;

import java.util.List;
import java.util.Optional;

/**
 * Podsumowanie tygodnia do przeglądu: wiersze nawyków oraz najlepszy i najsłabszy nawyk tygodnia.
 * Najsłabszy jest pusty, gdy wszystkie nawyki mają ten sam wynik albo jest tylko jeden.
 */
public record WeekSummary(List<WeekRow> rows, Optional<WeekRow> best, Optional<WeekRow> weakest) {
}
