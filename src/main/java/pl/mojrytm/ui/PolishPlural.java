package pl.mojrytm.ui;

/** Wybór formy rzeczownika po liczbie: 1 dzień, 2 dni, 5 dni, 22 dni, 12 dni. */
final class PolishPlural {

    private PolishPlural() {
    }

    static String pick(int count, String one, String few, String many) {
        if (count == 1) {
            return one;
        }
        int lastDigit = count % 10;
        int lastTwoDigits = count % 100;
        boolean isFew = lastDigit >= 2 && lastDigit <= 4 && !(lastTwoDigits >= 12 && lastTwoDigits <= 14);
        return isFew ? few : many;
    }
}
