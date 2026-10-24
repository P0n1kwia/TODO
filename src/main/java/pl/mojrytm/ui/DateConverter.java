package pl.mojrytm.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import javafx.util.StringConverter;

/** Daty w polach wyboru jako 24.10.2026; wpisać można też 24.10.2026 bez zer (4.1.2027). */
final class DateConverter extends StringConverter<LocalDate> {

    /** Tryb ścisły: 31.02.2026 to błąd, a nie cicha poprawka na 28 lutego. */
    private static final DateTimeFormatter PARSER =
            DateTimeFormatter.ofPattern("d.M.uuuu").withResolverStyle(ResolverStyle.STRICT);

    @Override
    public String toString(LocalDate date) {
        return date == null ? "" : Formats.numericDate(date);
    }

    @Override
    public LocalDate fromString(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), PARSER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
