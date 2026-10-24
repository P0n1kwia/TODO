package pl.mojrytm.ui;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/** Jedyne miejsce dostępu do tekstów widocznych w UI (plik messages_pl.properties). */
public final class Messages {

    static final Locale POLISH = Locale.of("pl");

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("pl.mojrytm.messages", POLISH);

    private Messages() {
    }

    public static String get(String key) {
        return BUNDLE.getString(key);
    }

    /** Tekst z podstawionymi argumentami ({0}, {1}). Liczby przekazuj już sformatowane jako tekst. */
    public static String format(String key, Object... args) {
        return new MessageFormat(get(key), POLISH).format(args);
    }

    static boolean has(String key) {
        return BUNDLE.containsKey(key);
    }
}
