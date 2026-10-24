package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DateConverterTest {

    private final DateConverter converter = new DateConverter();

    @Test
    void formatsAsDayMonthYear() {
        assertEquals("04.01.2027", converter.toString(LocalDate.of(2027, 1, 4)));
        assertEquals("", converter.toString(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"24.10.2026", "24.10.2026 ", "4.1.2027"})
    void readsWithOrWithoutLeadingZeros(String text) {
        LocalDate expected = text.startsWith("4.") ? LocalDate.of(2027, 1, 4) : LocalDate.of(2026, 10, 24);

        assertEquals(expected, converter.fromString(text));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "jutro", "31.02.2026", "2026-10-24", "24.10"})
    void unreadableTextGivesNull(String text) {
        assertNull(converter.fromString(text));
    }

    @Test
    void nullTextGivesNull() {
        assertNull(converter.fromString(null));
    }
}
