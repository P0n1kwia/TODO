package pl.mojrytm.ui;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import pl.mojrytm.service.DayStatus;
import pl.mojrytm.service.Streak;
import pl.mojrytm.service.StreakUnit;
import pl.mojrytm.service.WeekProgress;

/** Zamiana wyników z warstwy service na tekst. Nic tu nie liczy, tylko formatuje. */
final class Formats {

    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Messages.POLISH);
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d MMMM", Messages.POLISH);
    private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("d MMMM yyyy", Messages.POLISH);
    private static final DateTimeFormatter NUMERIC_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy", Messages.POLISH);

    private Formats() {
    }

    /** "sobota, 24 października 2026" */
    static String longDate(LocalDate date) {
        return LONG_DATE.format(date);
    }

    /** "24.10.2026" */
    static String numericDate(LocalDate date) {
        return NUMERIC_DATE.format(date);
    }

    /** Zakres dat pauzy: jeden dzień albo "12.10.2026 – 15.10.2026". */
    static String numericRange(LocalDate from, LocalDate to) {
        return from.equals(to) ? numericDate(from) : numericDate(from) + " – " + numericDate(to);
    }

    /** Tydzień od poniedziałku: "19–25 października 2026", na granicy miesięcy i lat z obiema datami. */
    static String weekRange(LocalDate monday) {
        LocalDate sunday = monday.plusDays(6);
        if (monday.getYear() != sunday.getYear()) {
            return DAY_MONTH_YEAR.format(monday) + " – " + DAY_MONTH_YEAR.format(sunday);
        }
        if (monday.getMonth() != sunday.getMonth()) {
            return DAY_MONTH.format(monday) + " – " + DAY_MONTH_YEAR.format(sunday);
        }
        return monday.getDayOfMonth() + "–" + DAY_MONTH_YEAR.format(sunday);
    }

    /** "Seria: 5 dni" albo "Seria: 3 tygodnie". */
    static String streak(Streak streak) {
        int n = streak.length();
        String unit = streak.unit() == StreakUnit.DAYS
                ? PolishPlural.pick(n, Messages.get("streak.days.one"), Messages.get("streak.days.few"),
                        Messages.get("streak.days.many"))
                : PolishPlural.pick(n, Messages.get("streak.weeks.one"), Messages.get("streak.weeks.few"),
                        Messages.get("streak.weeks.many"));
        return Messages.format("streak.label", n + " " + unit);
    }

    /** "4/6 (67%)", a gdy nic nie oczekujemy (pauza, przyszłość) myślnik. */
    static String progress(WeekProgress progress) {
        OptionalInt percent = progress.percent();
        if (percent.isEmpty()) {
            return Messages.get("progress.none");
        }
        return progress.done() + "/" + progress.expected() + " (" + percent.getAsInt() + "%)";
    }

    /** Symbol kratki dnia: ✓ zrobione, (✓) wersja minimalna, puste gdy nie zrobione. */
    static String symbol(DayStatus status) {
        return switch (status) {
            case DONE -> Messages.get("day.symbol.done");
            case DONE_MINIMAL -> Messages.get("day.symbol.minimal");
            case NOT_DONE -> "";
        };
    }

    /** Ilość bez zbędnych zer, z przecinkiem dziesiętnym: 8, 2,5, 0,333. */
    static String amount(double value) {
        DecimalFormat format = new DecimalFormat("0.###", DecimalFormatSymbols.getInstance(Messages.POLISH));
        format.setGroupingUsed(false);
        return format.format(value);
    }

    /** Wczytuje ilość wpisaną przez użytkownika (przecinek lub kropka); puste, gdy to nie liczba. */
    static OptionalDouble parseAmount(String text) {
        if (text == null) {
            return OptionalDouble.empty();
        }
        try {
            double value = Double.parseDouble(text.trim().replace(',', '.'));
            return Double.isFinite(value) ? OptionalDouble.of(value) : OptionalDouble.empty();
        } catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
    }
}
