package pl.mojrytm.service;

/** Zmiana wpisu narusza regułę. UI zamienia {@link #reason()} na komunikat z messages_pl.properties. */
public final class EntryRejectedException extends RuntimeException {

    public enum Reason {
        HABIT_NOT_FOUND,
        HABIT_ARCHIVED,
        /** Np. ilość dla nawyku CHECK albo zaznaczenie dla nawyku COUNT. */
        WRONG_KIND,
        /** Nie można zaznaczyć dnia z przyszłości. */
        FUTURE_DAY,
        /** Dni przed założeniem nawyku nie liczą się nigdzie. */
        BEFORE_HABIT_CREATED,
        INVALID_AMOUNT
    }

    private final Reason reason;

    public EntryRejectedException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
