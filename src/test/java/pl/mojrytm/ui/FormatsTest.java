package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import pl.mojrytm.service.DayStatus;
import pl.mojrytm.service.Streak;
import pl.mojrytm.service.StreakUnit;
import pl.mojrytm.service.WeekProgress;

class FormatsTest {

    @Test
    void formatsDatesInPolish() {
        assertEquals("sobota, 24 października 2026", Formats.longDate(LocalDate.of(2026, 10, 24)));
        assertEquals("24.10.2026", Formats.numericDate(LocalDate.of(2026, 10, 24)));
        assertEquals("05.01.2027", Formats.numericDate(LocalDate.of(2027, 1, 5)));
    }

    @Test
    void numericRangeCollapsesAOneDayPause() {
        LocalDate day = LocalDate.of(2026, 10, 12);

        assertEquals("12.10.2026", Formats.numericRange(day, day));
        assertEquals("12.10.2026 – 15.10.2026", Formats.numericRange(day, day.plusDays(3)));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "2026-10-19|19–25 października 2026",
            "2026-10-26|26 października – 1 listopada 2026",
            "2026-12-28|28 grudnia 2026 – 3 stycznia 2027",
            "2026-03-02|2–8 marca 2026"
    })
    void formatsAWeekRange(String monday, String expected) {
        assertEquals(expected, Formats.weekRange(LocalDate.parse(monday)));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "0|DAYS|Seria: 0 dni", "1|DAYS|Seria: 1 dzień", "2|DAYS|Seria: 2 dni", "5|DAYS|Seria: 5 dni",
            "12|DAYS|Seria: 12 dni", "22|DAYS|Seria: 22 dni",
            "1|WEEKS|Seria: 1 tydzień", "3|WEEKS|Seria: 3 tygodnie", "5|WEEKS|Seria: 5 tygodni",
            "12|WEEKS|Seria: 12 tygodni", "23|WEEKS|Seria: 23 tygodnie"
    })
    void formatsStreaksWithPolishPlurals(int length, StreakUnit unit, String expected) {
        assertEquals(expected, Formats.streak(new Streak(length, unit)));
    }

    @Test
    void formatsWeekProgress() {
        assertEquals("4/6 (67%)", Formats.progress(new WeekProgress(4, 6)));
        assertEquals("3/3 (100%)", Formats.progress(new WeekProgress(3, 3)));
        assertEquals("0/2 (0%)", Formats.progress(new WeekProgress(0, 2)));
        assertEquals("—", Formats.progress(new WeekProgress(0, 0)));
    }

    @Test
    void symbolsDistinguishDoneMinimalAndNotDone() {
        assertEquals("✓", Formats.symbol(DayStatus.DONE));
        assertEquals("(✓)", Formats.symbol(DayStatus.DONE_MINIMAL));
        assertEquals("", Formats.symbol(DayStatus.NOT_DONE));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"8.0|8", "2.5|2,5", "0.333333|0,333", "1000.0|1000", "0.0|0", "12.75|12,75"})
    void formatsAmountsWithADecimalComma(double value, String expected) {
        assertEquals(expected, Formats.amount(value));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"8|8.0", "2,5|2.5", "3.25|3.25", " 4 |4.0", "-1|-1.0", "1e3|1000.0"})
    void parsesAmountsWithCommaOrDot(String text, double expected) {
        assertEquals(OptionalDouble.of(expected), Formats.parseAmount(text));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "abc", "NaN", "Infinity", "-Infinity", "1,5,2", "2 szklanki"})
    void rejectsTextThatIsNotAFiniteNumber(String text) {
        assertEquals(OptionalDouble.empty(), Formats.parseAmount(text));
    }

    @Test
    void nullIsNotAnAmount() {
        assertEquals(OptionalDouble.empty(), Formats.parseAmount(null));
    }

    @Test
    void formattedAmountsCanBeParsedBack() {
        assertEquals(OptionalDouble.of(2.5), Formats.parseAmount(Formats.amount(2.5)));
    }
}
