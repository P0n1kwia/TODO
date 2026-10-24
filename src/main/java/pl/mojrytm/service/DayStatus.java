package pl.mojrytm.service;

/** Stan dnia nawyku wynikający z wpisów (bez uwzględniania pauzy). */
public enum DayStatus {
    NOT_DONE,
    /** Zaliczone wersją minimalną: liczy się jako zaliczone, ale jest oznaczone. */
    DONE_MINIMAL,
    DONE;

    public boolean isDone() {
        return this != NOT_DONE;
    }
}
