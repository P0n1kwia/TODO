package pl.mojrytm.service;

import java.util.Locale;

/** Porównywanie nazw obszarów i celów: bez różnicy wielkości liter i spacji na brzegach. */
final class Names {

    private Names() {
    }

    static boolean same(String first, String second) {
        return key(first).equals(key(second));
    }

    private static String key(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
