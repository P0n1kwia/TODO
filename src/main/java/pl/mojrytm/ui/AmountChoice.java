package pl.mojrytm.ui;

import java.util.Optional;
import java.util.OptionalDouble;

/** Ilość wybrana w oknie ilości (wpisany tekst i znacznik wersji minimalnej). */
record AmountChoice(double amount, boolean minimal) {

    /** Pusty, gdy tekst nie jest liczbą nieujemną. */
    static Optional<AmountChoice> parse(String text, boolean minimal) {
        OptionalDouble value = Formats.parseAmount(text);
        if (value.isEmpty() || value.getAsDouble() < 0) {
            return Optional.empty();
        }
        return Optional.of(new AmountChoice(value.getAsDouble(), minimal));
    }
}
