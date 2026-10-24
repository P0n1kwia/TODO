package pl.mojrytm.model;

/**
 * Ustawienia zapisane w pliku danych.
 *
 * @param dayStartHour godzina, o której zaczyna się dzień logiczny (0–23)
 * @param dataDir      folder danych wskazany przez użytkownika (null = domyślny)
 */
public record Settings(int dayStartHour, String dataDir) {

    public static final int DEFAULT_DAY_START_HOUR = 4;

    public Settings {
        Checks.requireRange(dayStartHour, 0, 23, "dayStartHour");
    }

    public static Settings defaults() {
        return new Settings(DEFAULT_DAY_START_HOUR, null);
    }
}
