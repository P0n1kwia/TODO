package pl.mojrytm.storage;

/** Nie udało się przenieść danych na bieżącą wersję schematu. */
public final class MigrationException extends DataFormatException {

    public MigrationException(String message) {
        super(message);
    }

    public MigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
