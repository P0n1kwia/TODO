package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import pl.mojrytm.model.Habit;
import pl.mojrytm.model.HabitKind;
import pl.mojrytm.model.Schedule;
import pl.mojrytm.service.DayStatus;
import pl.mojrytm.service.GoalOption;
import pl.mojrytm.service.Streak;
import pl.mojrytm.service.StreakUnit;
import pl.mojrytm.service.WeekProgress;

class FormatsTest {

    @Test
    void formatsDatesInPolish() {
        assertEquals("Sobota, 24 października 2026", Formats.longDate(LocalDate.of(2026, 10, 24)));
        assertEquals("24.10.2026", Formats.numericDate(LocalDate.of(2026, 10, 24)));
        assertEquals("05.01.2027", Formats.numericDate(LocalDate.of(2027, 1, 5)));
    }

    @ParameterizedTest
    @CsvSource({"0,0 nawyków", "1,1 nawyk", "2,2 nawyki", "4,4 nawyki", "5,5 nawyków", "12,12 nawyków",
            "22,22 nawyki", "112,112 nawyków"})
    void habitCountUsesThePolishPlural(int count, String expected) {
        assertEquals(expected, Formats.habitCount(count));
    }

    @Test
    void goalLabelShowsAreaAndGoalAndMarksArchivedOnes() {
        assertEquals("Sen › Spać 8 godzin", Formats.goalLabel(new GoalOption("g", "Sen", "Spać 8 godzin", false)));
        assertEquals("Sen › Spać 8 godzin (zarchiwizowany)",
                Formats.goalLabel(new GoalOption("g", "Sen", "Spać 8 godzin", true)));
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

    private static Habit habit(HabitKind kind, Schedule schedule, Integer times, Double target, String unit) {
        return new Habit("h", null, "Nawyk", kind, schedule, times, target, unit, null, LocalDate.of(2026, 10, 1), false);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "1|1 raz w tygodniu", "2|2 razy w tygodniu", "4|4 razy w tygodniu", "5|5 razy w tygodniu",
            "7|7 razy w tygodniu"})
    void describesTheWeeklySchedule(int times, String expected) {
        assertEquals(expected, Formats.schedule(habit(HabitKind.CHECK, Schedule.WEEKLY, times, null, null)));
    }

    @Test
    void describesTheDailySchedule() {
        assertEquals("Codziennie", Formats.schedule(habit(HabitKind.CHECK, Schedule.DAILY, null, null, null)));
    }

    @Test
    void describesTheKindWithTheTarget() {
        assertEquals("Zaznaczenie", Formats.kind(habit(HabitKind.CHECK, Schedule.DAILY, null, null, null)));
        assertEquals("Ilość: 8 szklanki",
                Formats.kind(habit(HabitKind.COUNT, Schedule.DAILY, null, 8.0, "szklanki")));
        assertEquals("Ilość: 2,5", Formats.kind(habit(HabitKind.COUNT, Schedule.DAILY, null, 2.5, null)));
        assertEquals("", Formats.target(habit(HabitKind.CHECK, Schedule.DAILY, null, null, null)));
    }
}
