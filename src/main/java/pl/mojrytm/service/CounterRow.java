package pl.mojrytm.service;

import pl.mojrytm.model.Counter;

/** Licznik i liczba pełnych dni od jego startu (do dziś logicznego). */
public record CounterRow(Counter counter, int days) {
}
