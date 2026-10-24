package pl.mojrytm.ui;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Zegar do testów, który można przesuwać. */
final class MutableClock extends Clock {

    private Instant instant;

    MutableClock(String isoInstant) {
        this.instant = Instant.parse(isoInstant);
    }

    void advance(Duration duration) {
        instant = instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
