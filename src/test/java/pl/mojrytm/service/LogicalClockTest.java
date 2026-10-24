package pl.mojrytm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pl.mojrytm.model.Settings;

class LogicalClockTest {

    @ParameterizedTest
    @CsvSource({
            // chwila,            początek dnia, dzień logiczny
            "2026-10-24T03:59:59, 4, 2026-10-23",
            "2026-10-24T04:00:00, 4, 2026-10-24",
            "2026-10-24T01:30:00, 4, 2026-10-23",
            "2026-10-24T00:00:00, 4, 2026-10-23",
            "2026-10-24T23:59:59, 4, 2026-10-24",
            "2026-10-24T00:00:00, 0, 2026-10-24",
            "2026-10-24T05:59:59, 6, 2026-10-23",
            "2026-10-24T11:59:59, 12, 2026-10-23",
            "2026-11-01T02:00:00, 4, 2026-10-31",
            "2027-01-01T01:00:00, 4, 2026-12-31",
            "2028-03-01T03:00:00, 4, 2028-02-29",
    })
    void dayEndsAtTheStartHourNotAtMidnight(String moment, int startHour, String expectedDay) {
        assertEquals(LocalDate.parse(expectedDay),
                LogicalClock.logicalDateOf(LocalDateTime.parse(moment), startHour));
    }

    @Test
    void todayComesFromTheInjectedClock() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-24T01:30:00Z"), ZoneOffset.UTC);

        assertEquals(LocalDate.of(2026, 10, 23), new LogicalClock(clock).today(4));
        assertEquals(LocalDate.of(2026, 10, 24), new LogicalClock(clock).today(0));
    }

    @Test
    void usesLocalWallTimeOfTheClockZone() {
        // 02:30 UTC to 04:30 w Warszawie (czas letni): tam dzień logiczny już się zaczął
        Instant instant = Instant.parse("2026-10-24T02:30:00Z");

        assertEquals(LocalDate.of(2026, 10, 24),
                new LogicalClock(Clock.fixed(instant, ZoneId.of("Europe/Warsaw"))).today(4));
        assertEquals(LocalDate.of(2026, 10, 23),
                new LogicalClock(Clock.fixed(instant, ZoneOffset.UTC)).today(4));
    }

    @Test
    void readsTheStartHourFromSettings() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-24T05:00:00Z"), ZoneOffset.UTC);

        assertEquals(LocalDate.of(2026, 10, 23), new LogicalClock(clock).today(new Settings(6, null)));
        assertEquals(LocalDate.of(2026, 10, 24), new LogicalClock(clock).today(new Settings(4, null)));
    }
}
