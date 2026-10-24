package pl.mojrytm.ui;

import java.util.Locale;
import java.util.ResourceBundle;

/** Jedyne miejsce dostępu do tekstów widocznych w UI (plik messages_pl.properties). */
public final class Messages {

    private static final ResourceBundle BUNDLE =
            ResourceBundle.getBundle("pl.mojrytm.messages", Locale.of("pl"));

    private Messages() {
    }

    public static String get(String key) {
        return BUNDLE.getString(key);
    }
}
