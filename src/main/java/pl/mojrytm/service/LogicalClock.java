package pl.mojrytm.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import pl.mojrytm.model.Settings;

/**
 * Dzień logiczny: kończy się o {@code dayStartHour} (domyślnie 4:00), a nie o północy,
 * więc wpis z godziny 1:30 należy jeszcze do poprzedniego dnia. Jedyne miejsce, które
 * zamienia zegar na datę; reszta logiki dostaje gotowy dzień.
 */
public final class LogicalClock {

    private final Clock clock;

    public LogicalClock(Clock clock) {
        this.clock = clock;
    }

    /** Dzisiejszy dzień logiczny według czasu lokalnego zegara. */
    public LocalDate today(int dayStartHour) {
        return logicalDateOf(LocalDateTime.now(clock), dayStartHour);
    }

    public LocalDate today(Settings settings) {
        return today(settings.dayStartHour());
    }

    public static LocalDate logicalDateOf(LocalDateTime moment, int dayStartHour) {
        return moment.minusHours(dayStartHour).toLocalDate();
    }
}
