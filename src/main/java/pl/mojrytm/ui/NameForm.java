package pl.mojrytm.ui;

import java.util.Optional;

/**
 * Nazwa obszaru lub celu wpisana w oknie, jeszcze jako tekst, z walidacją (bez JavaFX, żeby dało się
 * testować). Czy nazwa jest zajęta, ustala wołający (zależy to od danych), i podaje w {@code taken}.
 */
record NameForm(String name, boolean taken) {

    enum Problem {
        NAME_EMPTY,
        NAME_TAKEN
    }

    /** Pierwszy znaleziony problem albo pusty, gdy nazwa jest poprawna. */
    Optional<Problem> problem() {
        if (name == null || name.isBlank()) {
            return Optional.of(Problem.NAME_EMPTY);
        }
        if (taken) {
            return Optional.of(Problem.NAME_TAKEN);
        }
        return Optional.empty();
    }

    /** Nazwa bez spacji na brzegach; formularz musi być poprawny. */
    String cleanName() {
        if (problem().isPresent()) {
            throw new IllegalStateException("Formularz nazwy jest niepoprawny: " + problem().get());
        }
        return name.trim();
    }
}
