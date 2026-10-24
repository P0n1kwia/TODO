package pl.mojrytm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static pl.mojrytm.service.Fixtures.FRI;
import static pl.mojrytm.service.Fixtures.LONG_AGO;
import static pl.mojrytm.service.Fixtures.MON;
import static pl.mojrytm.service.Fixtures.PREV_MON;
import static pl.mojrytm.service.Fixtures.PREV_SUN;
import static pl.mojrytm.service.Fixtures.SAT;
import static pl.mojrytm.service.Fixtures.SUN;
import static pl.mojrytm.service.Fixtures.THU;
import static pl.mojrytm.service.Fixtures.TODAY;
import static pl.mojrytm.service.Fixtures.TUE;
import static pl.mojrytm.service.Fixtures.WED;
import static pl.mojrytm.service.Fixtures.amount;
import static pl.mojrytm.service.Fixtures.checkDaily;
import static pl.mojrytm.service.Fixtures.checkWeekly;
import static pl.mojrytm.service.Fixtures.countDaily;
import static pl.mojrytm.service.Fixtures.countWeekly;
import static pl.mojrytm.service.Fixtures.done;
import static pl.mojrytm.service.Fixtures.minimal;
import static pl.mojrytm.service.Fixtures.pause;
import static pl.mojrytm.service.Fixtures.pauses;
import static pl.mojrytm.service.Fixtures.tracker;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.mojrytm.model.Entry;

class HabitTrackerStreakTest {

    private static Streak days(int length) {
        return new Streak(length, StreakUnit.DAYS);
    }

    private static Streak weeks(int length) {
        return new Streak(length, StreakUnit.WEEKS);
    }

    @Nested
    class DailyHabit {

        @Test
        void noEntriesMeansNoStreak() {
            assertEquals(days(0), tracker(checkDaily(LONG_AGO)).streak(TODAY));
        }

        @Test
        void countsConsecutiveDaysUpToToday() {
            assertEquals(days(3), tracker(checkDaily(LONG_AGO), done(SAT), done(FRI), done(THU)).streak(TODAY));
        }

        @Test
        void todayNotDoneYetDoesNotBreakTheStreak() {
            assertEquals(days(2), tracker(checkDaily(LONG_AGO), done(FRI), done(THU)).streak(TODAY));
        }

        @Test
        void missedYesterdayLeavesOnlyToday() {
            assertEquals(days(1), tracker(checkDaily(LONG_AGO), done(SAT), done(THU)).streak(TODAY));
        }

        @Test
        void missedYesterdayAndNotDoneTodayMeansNoStreak() {
            assertEquals(days(0), tracker(checkDaily(LONG_AGO), done(THU), done(WED)).streak(TODAY));
        }

        @Test
        void aGapEndsTheStreak() {
            assertEquals(days(2), tracker(checkDaily(LONG_AGO), done(SAT), done(FRI), done(WED), done(TUE))
                    .streak(TODAY));
        }

        @Test
        void pausedDaysNeitherBreakNorExtendTheStreak() {
            HabitTracker tracker = tracker(checkDaily(LONG_AGO), pauses(pause(WED, THU)),
                    done(MON), done(TUE), done(FRI), done(SAT));

            assertEquals(days(4), tracker.streak(TODAY));
        }

        @Test
        void todayInPauseWithoutEntryKeepsTheStreakFromYesterday() {
            HabitTracker tracker = tracker(checkDaily(LONG_AGO), pauses(pause(SAT, SAT)), done(THU), done(FRI));

            assertEquals(days(2), tracker.streak(TODAY));
        }

        @Test
        void entryOnAPausedDayDoesNotExtendTheStreak() {
            HabitTracker tracker = tracker(checkDaily(LONG_AGO), pauses(pause(THU, THU)),
                    done(FRI), done(THU), done(WED));

            assertEquals(days(2), tracker.streak(TODAY));
        }

        @Test
        void streakEndsAtTheCreationDay() {
            assertEquals(days(3), tracker(checkDaily(THU), done(SAT), done(FRI), done(THU), done(WED)).streak(TODAY));
        }

        @Test
        void habitCreatedTodayStartsWithZeroOrOne() {
            assertEquals(days(0), tracker(checkDaily(TODAY)).streak(TODAY));
            assertEquals(days(1), tracker(checkDaily(TODAY), done(TODAY)).streak(TODAY));
        }

        @Test
        void habitCreatedInTheFutureHasNoStreak() {
            assertEquals(days(0), tracker(checkDaily(SUN), done(SAT)).streak(TODAY));
        }

        @Test
        void entriesBeforeCreationAreNotCounted() {
            assertEquals(days(0), tracker(checkDaily(WED), done(MON), done(TUE)).streak(TODAY));
        }

        @Test
        void futureEntriesAreIgnored() {
            assertEquals(days(1), tracker(checkDaily(LONG_AGO), done(SAT), done(SUN)).streak(TODAY));
        }

        @Test
        void minimalVersionKeepsTheStreakAlive() {
            assertEquals(days(2), tracker(checkDaily(LONG_AGO), minimal(FRI), done(SAT)).streak(TODAY));
        }

        @Test
        void countHabitNeedsTheTargetEachDay() {
            assertEquals(days(1), tracker(countDaily(8, LONG_AGO), amount(SAT, 8), amount(FRI, 5)).streak(TODAY));
            assertEquals(days(2), tracker(countDaily(8, LONG_AGO), amount(SAT, 8), amount(FRI, 9)).streak(TODAY));
        }

        @Test
        void longStreakAcrossTheYearBoundaryIsCountedInFull() {
            LocalDate created = TODAY.minusDays(399);
            List<Entry> entries = new ArrayList<>();
            for (LocalDate date = created; !date.isAfter(TODAY); date = date.plusDays(1)) {
                entries.add(done(date));
            }

            assertEquals(days(400), tracker(checkDaily(created), entries.toArray(new Entry[0])).streak(TODAY));
        }
    }

    @Nested
    class WeeklyHabit {

        /** Dwa dni w tygodniu zaczynającym się w {@code monday}: poniedziałek i środa. */
        private Entry[] twoDays(LocalDate monday) {
            return new Entry[] {done(monday), done(monday.plusDays(2))};
        }

        private Entry[] concat(Entry[]... parts) {
            List<Entry> all = new ArrayList<>();
            for (Entry[] part : parts) {
                all.addAll(List.of(part));
            }
            return all.toArray(new Entry[0]);
        }

        @Test
        void noEntriesMeansNoStreak() {
            assertEquals(weeks(0), tracker(checkWeekly(2, LONG_AGO)).streak(TODAY));
        }

        @Test
        void countsConsecutiveCompletedWeeksIncludingTheCurrentOne() {
            HabitTracker tracker = tracker(checkWeekly(2, LONG_AGO),
                    concat(twoDays(MON), twoDays(MON.minusWeeks(1)), twoDays(MON.minusWeeks(2))));

            assertEquals(weeks(3), tracker.streak(TODAY));
        }

        @Test
        void currentWeekNotCompletedYetDoesNotBreakTheStreak() {
            HabitTracker tracker = tracker(checkWeekly(2, LONG_AGO),
                    concat(new Entry[] {done(MON)}, twoDays(MON.minusWeeks(1)), twoDays(MON.minusWeeks(2))));

            assertEquals(weeks(2), tracker.streak(TODAY));
        }

        @Test
        void anIncompletePastWeekEndsTheStreak() {
            HabitTracker tracker = tracker(checkWeekly(2, LONG_AGO),
                    concat(twoDays(MON), new Entry[] {done(PREV_MON)}, twoDays(MON.minusWeeks(2))));

            assertEquals(weeks(1), tracker.streak(TODAY));
        }

        @Test
        void incompleteCurrentAndPreviousWeekMeansNoStreak() {
            HabitTracker tracker = tracker(checkWeekly(2, LONG_AGO),
                    concat(new Entry[] {done(MON)}, new Entry[] {done(PREV_MON)}, twoDays(MON.minusWeeks(2))));

            assertEquals(weeks(0), tracker.streak(TODAY));
        }

        @Test
        void weekEntirelyInPauseIsSkipped() {
            HabitTracker tracker = tracker(checkWeekly(2, LONG_AGO), pauses(pause(PREV_MON, PREV_SUN)),
                    concat(twoDays(MON), twoDays(MON.minusWeeks(2))));

            assertEquals(weeks(2), tracker.streak(TODAY));
        }

        @Test
        void partiallyPausedWeekNeedsOnlyTheAvailableDays() {
            // pauza pon–sob zostawia tylko niedzielę: wystarczy jeden wpis
            HabitTracker tracker = tracker(checkWeekly(2, LONG_AGO), pauses(pause(PREV_MON, PREV_SUN.minusDays(1))),
                    concat(twoDays(MON), new Entry[] {done(PREV_SUN)}));

            assertEquals(weeks(2), tracker.streak(TODAY));
        }

        @Test
        void streakEndsWhereTheHabitWasNotYetCreated() {
            LocalDate created = PREV_MON.plusDays(3);
            HabitTracker tracker = tracker(checkWeekly(2, created),
                    concat(twoDays(MON), new Entry[] {done(created), done(created.plusDays(1))},
                            twoDays(MON.minusWeeks(2))));

            assertEquals(weeks(2), tracker.streak(TODAY));
        }

        @Test
        void habitCreatedThisWeekCanCompleteItsFirstWeek() {
            assertEquals(weeks(1), tracker(checkWeekly(2, WED), done(WED), done(THU)).streak(TODAY));
            assertEquals(weeks(0), tracker(checkWeekly(2, WED), done(WED)).streak(TODAY));
        }

        @Test
        void minimalVersionCountsTowardsTheWeek() {
            assertEquals(weeks(1), tracker(checkWeekly(2, LONG_AGO), minimal(MON), done(TUE)).streak(TODAY));
        }

        @Test
        void countHabitNeedsTheTargetOnEachCountedDay() {
            HabitTracker tracker = tracker(countWeekly(2, 8, LONG_AGO), amount(MON, 8), amount(TUE, 5), amount(WED, 8));

            assertEquals(weeks(1), tracker.streak(TODAY));
        }

        @Test
        void longStreakOfWeeksIsCountedInFull() {
            List<Entry> entries = new ArrayList<>();
            for (int week = 0; week < 60; week++) {
                entries.addAll(List.of(twoDays(MON.minusWeeks(week))));
            }

            assertEquals(weeks(60), tracker(checkWeekly(2, MON.minusWeeks(59)), entries.toArray(new Entry[0]))
                    .streak(TODAY));
        }

        @Test
        void weekBeforeTheHabitExistedIsNeverEvaluated() {
            // jedyny tydzień z wpisami kończy się przed założeniem nawyku
            assertEquals(weeks(0), tracker(checkWeekly(2, MON), concat(twoDays(PREV_MON))).streak(TODAY));
        }
    }
}
