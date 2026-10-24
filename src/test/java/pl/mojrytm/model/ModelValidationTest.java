package pl.mojrytm.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ModelValidationTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 24);

    private static Habit checkDaily(String name) {
        return new Habit("h", null, name, HabitKind.CHECK, Schedule.DAILY, null, null, null, null, DAY, false);
    }

    @Test
    void checkDailyHabitIsValid() {
        assertDoesNotThrow(() -> checkDaily("Spacer"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void habitNameMustNotBeBlank(String name) {
        assertThrows(IllegalArgumentException.class, () -> checkDaily(name));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 8, -1})
    void weeklyHabitNeedsTimesPerWeekFromOneToSeven(int times) {
        assertThrows(IllegalArgumentException.class, () ->
                new Habit("h", null, "Bieg", HabitKind.CHECK, Schedule.WEEKLY, times, null, null, null, DAY, false));
    }

    @Test
    void weeklyHabitWithoutTimesPerWeekIsInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                new Habit("h", null, "Bieg", HabitKind.CHECK, Schedule.WEEKLY, null, null, null, null, DAY, false));
    }

    @Test
    void weeklyHabitWithBoundaryTimesIsValid() {
        for (int times : new int[] {1, 7}) {
            assertDoesNotThrow(() ->
                    new Habit("h", null, "Bieg", HabitKind.CHECK, Schedule.WEEKLY, times, null, null, null, DAY, false));
        }
    }

    @Test
    void dailyHabitMustNotHaveTimesPerWeek() {
        assertThrows(IllegalArgumentException.class, () ->
                new Habit("h", null, "Bieg", HabitKind.CHECK, Schedule.DAILY, 3, null, null, null, DAY, false));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY})
    void countHabitNeedsPositiveFiniteTarget(double target) {
        assertThrows(IllegalArgumentException.class, () ->
                new Habit("h", null, "Woda", HabitKind.COUNT, Schedule.DAILY, null, target, "szklanki", null, DAY, false));
    }

    @Test
    void countHabitWithoutTargetIsInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                new Habit("h", null, "Woda", HabitKind.COUNT, Schedule.DAILY, null, null, null, null, DAY, false));
    }

    @Test
    void checkHabitMustNotHaveTarget() {
        assertThrows(IllegalArgumentException.class, () ->
                new Habit("h", null, "Spacer", HabitKind.CHECK, Schedule.DAILY, null, 5.0, null, null, DAY, false));
    }

    @Test
    void entryValueMustBeNonNegativeAndFinite() {
        assertDoesNotThrow(() -> new Entry("e", "h", DAY, 0.0, false));
        assertThrows(IllegalArgumentException.class, () -> new Entry("e", "h", DAY, -0.5, false));
        assertThrows(IllegalArgumentException.class, () -> new Entry("e", "h", DAY, Double.NaN, false));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6})
    void checkInMoodAndEnergyMustBeOneToFive(int value) {
        assertThrows(IllegalArgumentException.class, () -> new CheckIn(DAY, value, 3));
        assertThrows(IllegalArgumentException.class, () -> new CheckIn(DAY, 3, value));
    }

    @Test
    void checkInBoundariesAreValid() {
        assertDoesNotThrow(() -> new CheckIn(DAY, 1, 5));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 11})
    void wheelScoreMustBeOneToTen(int score) {
        assertThrows(IllegalArgumentException.class, () -> new WheelRating(DAY, "a", score));
    }

    @Test
    void pauseMayLastOneDayButNotEndBeforeItStarts() {
        assertDoesNotThrow(() -> new Pause(DAY, DAY, "wyjazd"));
        assertThrows(IllegalArgumentException.class, () -> new Pause(DAY, DAY.minusDays(1), "wyjazd"));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 24})
    void dayStartHourMustBeAnHourOfTheDay(int hour) {
        assertThrows(IllegalArgumentException.class, () -> new Settings(hour, null));
    }

    @Test
    void defaultDayStartsAtFour() {
        assertEquals(4, Settings.defaults().dayStartHour());
    }

    @Test
    void appDataKeepsImmutableCopiesOfLists() {
        List<Area> areas = new ArrayList<>(List.of(new Area("a", "Sen")));
        AppData data = new AppData(1, null, areas, null, null, null, null, null, null, null, null, null);

        areas.add(new Area("b", "Ciało"));

        assertEquals(1, data.areas().size());
        assertThrows(UnsupportedOperationException.class, () -> data.areas().add(new Area("c", "Relacje")));
    }

    @Test
    void withEntriesReplacesOnlyTheEntries() {
        AppData data = new AppData(1, new Settings(6, null), List.of(new Area("a", "Sen")),
                null, null, null, null, null, null, null, null, null);
        Entry entry = new Entry("e", "h", DAY, 1.0, false);

        AppData changed = data.withEntries(List.of(entry));

        assertEquals(List.of(entry), changed.entries());
        assertEquals(data.areas(), changed.areas());
        assertEquals(data.settings(), changed.settings());
        assertEquals(List.of(), data.entries());
    }

    @Test
    void emptyAppDataUsesCurrentSchemaAndDefaultSettings() {
        AppData data = AppData.empty();

        assertEquals(AppData.CURRENT_SCHEMA_VERSION, data.schemaVersion());
        assertEquals(Settings.defaults(), data.settings());
        assertEquals(List.of(), data.habits());
    }

    @Test
    void idsAreUniqueNonBlankText() {
        String first = Ids.newId();

        assertDoesNotThrow(() -> new Area(first, "Sen"));
        assertEquals(36, first.length());
        assertNotEquals(first, Ids.newId());
    }
}
