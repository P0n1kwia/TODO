package pl.mojrytm.storage;

import java.io.IOException;

/** Zawartość pliku danych jest nieprawidłowa (uszkodzony JSON, błędne wartości, brak migracji). */
public class DataFormatException extends IOException {

    public DataFormatException(String message) {
        super(message);
    }

    public DataFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
