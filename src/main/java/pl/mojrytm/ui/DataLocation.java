package pl.mojrytm.ui;

import java.nio.file.Path;

/**
 * Folder danych: domyślnie ~/.moj-rytm. Właściwość systemowa {@code mojrytm.dataDir} pozwala
 * go podmienić (testy, uruchomienia próbne bez ruszania prawdziwych danych).
 */
final class DataLocation {

    static final String OVERRIDE_PROPERTY = "mojrytm.dataDir";

    private DataLocation() {
    }

    static Path resolve() {
        String override = System.getProperty(OVERRIDE_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        return Path.of(System.getProperty("user.home"), ".moj-rytm");
    }
}
