package pl.mojrytm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.mojrytm.service.Fixtures.FRI;
import static pl.mojrytm.service.Fixtures.LONG_AGO;
import static pl.mojrytm.service.Fixtures.MON;
import static pl.mojrytm.service.Fixtures.NEXT_MON;
import static pl.mojrytm.service.Fixtures.PREV_MON;
import static pl.mojrytm.service.Fixtures.PREV_SUN;
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
import java.util.OptionalInt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.mojrytm.model.Entry;

class HabitTrackerWeekTest {

    private static Entry[] everyDayOf(LocalDate monday) {
        Entry[] entries = new Entry[7];
        for (int i = 0; i < 7; i++) {
            entries[i] = done(monday.plusDays(i));
        }
        return entries;
    }

    @Nested
    class DailyHabit {

        @Test
        void fullyDonePastWeekIsOneHundredPercent() {
            WeekProgress progress = tracker(checkDaily(LONG_AGO), everyDayOf(PREV_MON)).weekProgress(PREV_MON, TODAY);

            assertEquals(new WeekProgress(7, 7), progress);
            assertTrue(progress.completed());
            assertEquals(OptionalInt.of(100), progress.percent());
        }

        @Test
        void partiallyDonePastWeekGivesRoundedPercent() {
            HabitTracker tracker = tracker(checkDaily(LONG_AGO),
                    done(PREV_MON), done(PREV_MON.plusDays(1)), done(PREV_MON.plusDays(4)), done(PREV_SUN));

            WeekProgress progress = tracker.weekProgress(PREV_MON, TODAY);

            assertEquals(new WeekProgress(4, 7), progress);
            assertFalse(progress.completed());
            assertEquals(OptionalInt.of(57), progress.percent());
        }

        @Test
        void currentWeekExpectsOnlyDaysUpToToday() {
            WeekProgress progress = tracker(checkDaily(LONG_AGO), done(MON), done(TUE), done(FRI))
                    .weekProgress(TODAY, TODAY);

            assertEquals(new WeekProgress(3, 6), progress);
            assertEquals(OptionalInt.of(50), progress.percent());
        }

        @Test
        void anyDayOfTheWeekSelectsTheSameWeek() {
            HabitTracker tracker = tracker(checkDaily(LONG_AGO), done(MON));

            assertEquals(tracker.weekProgress(MON, TODAY), tracker.weekProgress(SUN, TODAY));
        }

        @Test
        void entryInTheFutureIsNotCounted() {
            WeekProgress progress = tracker(checkDaily(LONG_AGO), done(SUN)).weekProgress(TODAY, TODAY);

            assertEquals(new WeekProgress(0, 6), progress);
        }

        @Test
        void habitCreatedMidWeekExpectsDaysFromCreationOnly() {
            // wpis z wtorku (przed założeniem w środę) nie liczy się nigdzie
            WeekProgress progress = tracker(checkDaily(WED), done(TUE), done(WED), done(THU))
                    .weekProgress(TODAY, TODAY);

            assertEquals(new WeekProgress(2, 4), progress);
        }

        @Test
        void pausedDaysLeaveTheDenominatorAndDoNotCountAsDone() {
            HabitTracker tracker = tracker(checkDaily(LONG_AGO), pauses(pause(TUE, WED)),
                    done(MON), done(TUE), done(THU));

            assertEquals(new WeekProgress(2, 4), tracker.weekProgress(TODAY, TODAY));
        }

        @Test
        void weekEntirelyInPauseIsSkipped() {
            WeekProgress progress = tracker(checkDaily(LONG_AGO), pauses(pause(PREV_MON, PREV_SUN)), done(PREV_MON))
                    .weekProgress(PREV_MON, TODAY);

            assertEquals(new WeekProgress(0, 0), progress);
            assertFalse(progress.completed());
            assertEquals(OptionalInt.empty(), progress.percent());
        }

        @Test
        void weekBeforeCreationIsSkipped() {
            assertEquals(new WeekProgress(0, 0), tracker(checkDaily(MON)).weekProgress(PREV_MON, TODAY));
        }

        @Test
        void weekInTheFutureIsSkipped() {
            assertEquals(new WeekProgress(0, 0), tracker(checkDaily(LONG_AGO)).weekProgress(NEXT_MON, TODAY));
        }

        @Test
        void minimalEntriesCountAsDone() {
            WeekProgress progress = tracker(checkDaily(LONG_AGO), minimal(MON), done(TUE)).weekProgress(TODAY, TODAY);

            assertEquals(new WeekProgress(2, 6), progress);
        }

        @Test
        void countHabitCountsDaysThatReachedTheTarget() {
            WeekProgress progress = tracker(countDaily(8, LONG_AGO), amount(MON, 8), amount(TUE, 5), amount(WED, 9))
                    .weekProgress(TODAY, TODAY);

            assertEquals(new WeekProgress(2, 6), progress);
        }

        @Test
        void todayCountsAsExpectedEvenWhenNotDoneYet() {
            assertEquals(new WeekProgress(0, 1), tracker(checkDaily(LONG_AGO)).weekProgress(MON, MON));
        }
    }

    @Nested
    class WeeklyHabit {

        @Test
        void reachingTheNumberOfDaysCompletesTheWeek() {
            WeekProgress progress = tracker(checkWeekly(3, LONG_AGO), done(MON), done(WED), done(FRI))
                    .weekProgress(TODAY, TODAY);

            assertEquals(new WeekProgress(3, 3), progress);
            assertTrue(progress.completed());
        }

        @Test
        void tooFewDaysLeaveTheWeekOpen() {
            WeekProgress progress = tracker(checkWeekly(3, LONG_AGO), done(MON), done(WED)).weekProgress(TODAY, TODAY);

            assertEquals(new WeekProgress(2, 3), progress);
            assertFalse(progress.completed());
            assertEquals(OptionalInt.of(67), progress.percent());
        }

        @Test
        void moreDaysThanRequiredStillGiveAtMostOneHundredPercent() {
            WeekProgress progress = tracker(checkWeekly(3, LONG_AGO), done(MON), done(TUE), done(WED), done(THU))
                    .weekProgress(TODAY, TODAY);

            assertTrue(progress.completed());
            assertEquals(OptionalInt.of(100), progress.percent());
        }

        @Test
        void severalEntriesOnOneDayCountAsOneDay() {
            WeekProgress progress = tracker(checkWeekly(3, LONG_AGO), done(MON), minimal(MON), done(TUE))
                    .weekProgress(TODAY, TODAY);

            assertEquals(2, progress.done());
        }

        @Test
        void daysStillAheadInTheCurrentWeekAreAvailable() {
            // dziś poniedziałek: do końca tygodnia są jeszcze dni, więc oczekujemy pełnego N
            assertEquals(new WeekProgress(0, 3), tracker(checkWeekly(3, LONG_AGO)).weekProgress(MON, MON));
        }

        @Test
        void partialPauseLowersExpectationToTheAvailableDays() {
            // pauza pon–czw zostawia pt–nd, czyli 3 dni dostępne
            HabitTracker three = tracker(checkWeekly(3, LONG_AGO), pauses(pause(PREV_MON, PREV_MON.plusDays(3))));
            HabitTracker five = tracker(checkWeekly(5, LONG_AGO), pauses(pause(PREV_MON, PREV_MON.plusDays(3))));

            assertEquals(3, three.weekProgress(PREV_MON, TODAY).expected());
            assertEquals(3, five.weekProgress(PREV_MON, TODAY).expected());
        }

        @Test
        void expectationNeverExceedsTheNumberOfPerWeekDays() {
            HabitTracker tracker = tracker(checkWeekly(2, LONG_AGO), pauses(pause(PREV_MON, PREV_MON.plusDays(1))));

            assertEquals(2, tracker.weekProgress(PREV_MON, TODAY).expected());
        }

        @Test
        void entriesOnPausedDaysStillCountTowardsTheWeek() {
            HabitTracker tracker = tracker(checkWeekly(3, LONG_AGO), pauses(pause(PREV_MON, PREV_MON.plusDays(3))),
                    done(PREV_MON.plusDays(1)), done(PREV_MON.plusDays(2)), done(PREV_MON.plusDays(4)));

            WeekProgress progress = tracker.weekProgress(PREV_MON, TODAY);

            assertEquals(new WeekProgress(3, 3), progress);
            assertTrue(progress.completed());
        }

        @Test
        void weekEntirelyInPauseIsSkippedEvenWithEntries() {
            WeekProgress progress = tracker(checkWeekly(3, LONG_AGO), pauses(pause(PREV_MON, PREV_SUN)),
                    done(PREV_MON), done(PREV_MON.plusDays(1))).weekProgress(PREV_MON, TODAY);

            assertEquals(0, progress.expected());
            assertFalse(progress.completed());
            assertEquals(OptionalInt.empty(), progress.percent());
        }

        @Test
        void habitCreatedLateInTheWeekExpectsOnlyWhatIsPossible() {
            // założony w sobotę: dostępne sobota i niedziela, więc oczekujemy 2 zamiast 3
            LocalDate created = PREV_SUN.minusDays(1);
            WeekProgress progress = tracker(checkWeekly(3, created), done(created), done(PREV_SUN))
                    .weekProgress(PREV_MON, TODAY);

            assertEquals(new WeekProgress(2, 2), progress);
            assertTrue(progress.completed());
        }

        @Test
        void entriesBeforeCreationAreIgnored() {
            WeekProgress progress = tracker(checkWeekly(3, WED), done(TUE), done(WED)).weekProgress(TODAY, TODAY);

            assertEquals(new WeekProgress(1, 3), progress);
        }

        @Test
        void countHabitCountsOnlyDaysThatReachedTheTarget() {
            WeekProgress progress = tracker(countWeekly(3, 8, LONG_AGO), amount(MON, 8), amount(TUE, 5), amount(WED, 8))
                    .weekProgress(TODAY, TODAY);

            assertEquals(new WeekProgress(2, 3), progress);
        }

        @Test
        void weekInTheFutureHasNoDoneDaysButStillExpectsTheDays() {
            assertEquals(new WeekProgress(0, 3), tracker(checkWeekly(3, LONG_AGO)).weekProgress(NEXT_MON, TODAY));
        }
    }

    @Nested
    class WeekViewCells {

        @Test
        void containsSevenCellsFromMondayToSunday() {
            WeekView view = tracker(checkDaily(LONG_AGO), done(MON)).week(WED, TODAY);

            assertEquals(MON, view.weekStart());
            assertEquals(7, view.days().size());
            assertEquals(MON, view.days().get(0).date());
            assertEquals(SUN, view.days().get(6).date());
            assertEquals(DayStatus.DONE, view.days().get(0).status());
            assertEquals(DayStatus.NOT_DONE, view.days().get(1).status());
        }

        @Test
        void flagsFutureDaysAndCarriesTheWeekProgress() {
            HabitTracker tracker = tracker(checkDaily(LONG_AGO), done(MON));
            WeekView view = tracker.week(TODAY, TODAY);

            assertFalse(view.days().get(5).future());
            assertTrue(view.days().get(6).future());
            assertEquals(tracker.weekProgress(TODAY, TODAY), view.progress());
        }

        @Test
        void cellsCanBeReadButNotChanged() {
            WeekView view = tracker(checkDaily(LONG_AGO)).week(MON, TODAY);

            assertThrows(UnsupportedOperationException.class,
                    () -> view.days().add(view.days().get(0)));
        }
    }
}
