package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MessagesTest {

    @Test
    void readsPolishTextsWithDiacritics() {
        assertEquals("Mój Rytm", Messages.get("app.title"));
    }
}
