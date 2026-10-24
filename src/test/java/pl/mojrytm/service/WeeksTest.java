package pl.mojrytm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WeeksTest {

    @ParameterizedTest
    @CsvSource({
            // dzień,     poniedziałek tygodnia
            "2026-10-19, 2026-10-19", // poniedziałek
            "2026-10-22, 2026-10-19", // czwartek
            "2026-10-24, 2026-10-19", // sobota
            "2026-10-25, 2026-10-19", // niedziela należy do tygodnia kończącego się nią
            "2026-10-26, 2026-10-26", // następny poniedziałek
            "2026-12-31, 2026-12-28", // tydzień przechodzi przez Nowy Rok
            "2027-01-01, 2026-12-28",
            "2026-03-01, 2026-02-23", // przejście przez koniec miesiąca
    })
    void weekStartsOnMonday(String day, String monday) {
        assertEquals(LocalDate.parse(monday), Weeks.startOf(LocalDate.parse(day)));
    }

    @Test
    void weekEndsOnSunday() {
        assertEquals(LocalDate.of(2026, 10, 25), Weeks.endOf(LocalDate.of(2026, 10, 19)));
        assertEquals(LocalDate.of(2026, 10, 25), Weeks.endOf(LocalDate.of(2026, 10, 22)));
        assertEquals(LocalDate.of(2027, 1, 3), Weeks.endOf(LocalDate.of(2026, 12, 31)));
    }
}
