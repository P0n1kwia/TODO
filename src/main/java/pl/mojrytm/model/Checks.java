package pl.mojrytm.model;

/** Wspólne sprawdzenia niezmienników w konstruktorach rekordów modelu. */
final class Checks {

    private Checks() {
    }

    static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Pole '" + field + "' nie może być puste");
        }
        return value;
    }

    static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("Pole '" + field + "' jest wymagane");
        }
        return value;
    }

    static int requireRange(int value, int min, int max, String field) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    "Pole '" + field + "' musi mieścić się w zakresie " + min + "–" + max + ", jest: " + value);
        }
        return value;
    }
}
