package pl.mojrytm.model;

import java.util.UUID;

/** Identyfikatory encji: UUID zapisany jako tekst. */
public final class Ids {

    private Ids() {
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }
}
