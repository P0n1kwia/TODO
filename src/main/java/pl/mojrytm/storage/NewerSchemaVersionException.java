package pl.mojrytm.storage;

/** Plik pochodzi z nowszej wersji aplikacji, więc nie wolno go otwierać ani zmieniać. */
public final class NewerSchemaVersionException extends DataFormatException {

    private final int fileVersion;
    private final int appVersion;

    public NewerSchemaVersionException(int fileVersion, int appVersion) {
        super("Plik ma wersję schematu " + fileVersion + ", a aplikacja obsługuje najwyżej " + appVersion);
        this.fileVersion = fileVersion;
        this.appVersion = appVersion;
    }

    public int fileVersion() {
        return fileVersion;
    }

    public int appVersion() {
        return appVersion;
    }
}
