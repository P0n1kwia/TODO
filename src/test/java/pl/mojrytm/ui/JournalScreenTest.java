package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Note;
import pl.mojrytm.model.NoteType;
import pl.mojrytm.storage.JsonStorage;

/** Dziś to sobota 2026-10-24, bieżący tydzień to 19–25 października. */
class JournalScreenTest {

    private static final LocalDate TODAY = UiTestData.TODAY;
    private static final LocalDate YESTERDAY = UiTestData.YESTERDAY;
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 19);

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock("2026-10-24T10:00:00Z");
    private final List<String> questions = new ArrayList<>();
    private boolean answer = true;
    private AppModel model;
    private JournalScreen screen;

    @BeforeAll
    static void toolkit() {
        FxTestSupport.assumeToolkit();
    }

    @AfterEach
    void tearDown() throws IOException {
        model.shutdown();
    }

    private void open(AppData data) {
        model = new AppModel(data, new JsonStorage(dir, clock), clock);
        screen = FxTestSupport.onFx(() -> new JournalScreen(model, question -> {
            questions.add(question);
            return answer;
        }));
    }

    private Node node(String id) {
        return screen.lookup("#" + id);
    }

    private void type(String id, String text) {
        FxTestSupport.onFx(() -> ((TextInputControl) node(id)).setText(text));
    }

    private String textOf(String id) {
        return ((TextInputControl) node(id)).getText();
    }

    private void click(String id) {
        FxTestSupport.onFx(((Button) node(id))::fire);
    }

    private boolean disabled(String id) {
        return node(id).isDisabled();
    }

    private void pick(String comboId, int index) {
        FxTestSupport.onFx(() -> ((ComboBox<?>) node(comboId)).getSelectionModel().select(index));
    }

    private void setDay(LocalDate day) {
        FxTestSupport.onFx(() -> ((DatePicker) node("daily-date")).setValue(day));
    }

    private List<String> texts(String styleClass) {
        return screen.lookupAll(styleClass).stream().map(n -> ((Label) n).getText()).toList();
    }

    private List<Note> notes(NoteType type) {
        return model.data().notes().stream().filter(note -> note.type() == type).toList();
    }

    private static Note daily(String id, LocalDate date, String text, List<String> tags, String habitId) {
        return new Note(id, date, NoteType.DAILY, text, tags, habitId);
    }

    // --- wpis dzienny ---

    @Test
    void theDailyEntryStartsOnTodayEmptyAndNothingToSave() {
        open(UiTestData.sample());

        assertEquals(TODAY, ((DatePicker) node("daily-date")).getValue());
        assertEquals("", textOf("daily-text"));
        assertTrue(disabled("daily-save"));
        assertTrue(disabled("daily-next"));
        assertFalse(node("daily-dirty").isVisible());
    }

    @Test
    void typingMarksTheEntryDirtyAndSavingStoresItWithTagsAndHabit() {
        open(UiTestData.sample());

        type("daily-text", "  Dobry dzień.  ");
        type("daily-tags", "Sen, #Energia");
        pick("daily-habit", 1);

        assertTrue(node("daily-dirty").isVisible());
        assertFalse(disabled("daily-save"));

        click("daily-save");

        Note saved = notes(NoteType.DAILY).get(0);
        assertEquals(TODAY, saved.date());
        assertEquals("Dobry dzień.", saved.text());
        assertEquals(List.of("sen", "energia"), saved.tags());
        assertEquals("spacer", saved.habitId());
        assertEquals("Zapisano wpis z dnia 24.10.2026", model.statusProperty().get().text());
        assertFalse(node("daily-dirty").isVisible());
        assertTrue(disabled("daily-save"));
    }

    @Test
    void anExistingEntryIsShownWithItsTagsAndHabit() {
        open(UiTestData.sample().withNotes(List.of(
                daily("d1", TODAY, "Był spacer", List.of("sen", "energia"), "woda"))));

        assertEquals("Był spacer", textOf("daily-text"));
        assertEquals("sen, energia", textOf("daily-tags"));
        assertEquals("Woda", ((ComboBox<?>) node("daily-habit")).getValue().toString());
        assertFalse(node("daily-dirty").isVisible());
    }

    @Test
    void clearingTheTextAndSavingRemovesTheEntry() {
        open(UiTestData.sample().withNotes(List.of(daily("d1", TODAY, "Coś", List.of(), null))));

        type("daily-text", "");
        assertTrue(node("daily-dirty").isVisible());
        click("daily-save");

        assertEquals(List.of(), model.data().notes());
        assertEquals("Usunięto wpis z dnia 24.10.2026", model.statusProperty().get().text());
    }

    @Test
    void changingTheDaySavesTheUnsavedTextOfTheOldDayFirst() {
        open(UiTestData.sample());

        type("daily-text", "Napisane dziś");
        click("daily-previous");

        assertEquals(YESTERDAY, ((DatePicker) node("daily-date")).getValue());
        assertEquals("", textOf("daily-text"));
        assertEquals(TODAY, notes(NoteType.DAILY).get(0).date());
        assertEquals("Napisane dziś", notes(NoteType.DAILY).get(0).text());
        assertFalse(disabled("daily-next"));

        click("daily-today");

        assertEquals(TODAY, ((DatePicker) node("daily-date")).getValue());
        assertEquals("Napisane dziś", textOf("daily-text"));
    }

    @Test
    void eachDayKeepsItsOwnEntry() {
        open(UiTestData.sample());
        type("daily-text", "Dziś");
        click("daily-save");

        click("daily-previous");
        type("daily-text", "Wczoraj");
        click("daily-save");
        click("daily-next");

        assertEquals("Dziś", textOf("daily-text"));
        assertEquals(2, notes(NoteType.DAILY).size());
    }

    @Test
    void aFutureDayIsRefusedAndKeepsTheTypedTextForTheCurrentDay() {
        open(UiTestData.sample());
        type("daily-text", "Tekst na dziś");

        setDay(TODAY.plusDays(1));

        assertEquals("Wpis może dotyczyć dzisiaj albo przeszłości.", ((Label) node("daily-problem")).getText());
        assertTrue(disabled("daily-save"));
        assertEquals("Tekst na dziś", textOf("daily-text"));
        assertEquals(List.of(), model.data().notes());

        setDay(TODAY);

        assertEquals("", ((Label) node("daily-problem")).getText());
        assertEquals("Tekst na dziś", textOf("daily-text"));
        assertFalse(disabled("daily-save"));
        assertEquals(List.of(), model.data().notes());
    }

    @Test
    void aMissingDayIsReported() {
        open(UiTestData.sample());

        setDay(null);

        assertEquals("Wybierz dzień (np. 24.10.2026).", ((Label) node("daily-problem")).getText());
        assertTrue(disabled("daily-save"));
    }

    @Test
    void theHabitListOffersActiveHabitsAndKeepsAnArchivedOneOnlyWhenLinked() {
        open(UiTestData.sample().withNotes(List.of(daily("d1", YESTERDAY, "Stary", List.of(), "stary"))));

        List<String> today = ((ComboBox<?>) node("daily-habit")).getItems().stream().map(Object::toString).toList();
        click("daily-previous");
        List<String> yesterday = ((ComboBox<?>) node("daily-habit")).getItems().stream().map(Object::toString).toList();

        assertEquals(List.of("Bez nawyku", "Spacer", "Woda", "Bieg"), today);
        assertEquals(List.of("Bez nawyku", "Spacer", "Woda", "Bieg", "Stary nawyk (zarchiwizowany)"), yesterday);
        assertEquals("Stary nawyk (zarchiwizowany)", ((ComboBox<?>) node("daily-habit")).getValue().toString());
    }

    @Test
    void leavingTheScreenSavesTheUnsavedText() {
        open(UiTestData.sample());
        StackPane host = new StackPane(screen);
        FxTestSupport.onFx(() -> new Scene(host));
        type("daily-text", "Zapisz mnie");
        type("weekly-text", "Przegląd na szybko");

        FxTestSupport.onFx(() -> host.getChildren().clear());

        assertEquals("Zapisz mnie", notes(NoteType.DAILY).get(0).text());
        assertEquals("Przegląd na szybko", notes(NoteType.WEEKLY_REVIEW).get(0).text());
    }

    @Test
    void closingTheWindowSavesTheUnsavedText() {
        open(UiTestData.sample());
        type("daily-text", "Przed zamknięciem");

        FxTestSupport.onFx(() -> model.runCloseHooks());

        assertEquals("Przed zamknięciem", notes(NoteType.DAILY).get(0).text());
    }

    // --- przegląd tygodnia ---

    @Test
    void theWeeklyReviewStartsOnTheCurrentWeekWithItsSummary() {
        open(UiTestData.sample());

        assertEquals("19–25 października 2026", ((Label) node("weekly-range")).getText());
        assertEquals(List.of(
                "Spacer: 1/6 (17%) · Seria: 1 dzień",
                "Woda: 0/6 (0%) · Seria: 0 dni",
                "Bieg: 0/3 (0%) · Seria: 0 tygodni"), texts(".summary-row"));
        assertEquals(List.of("Najlepszy: Spacer · 1/6 (17%)"), texts(".summary-best"));
        assertEquals(List.of("Do poprawy: Woda · 0/6 (0%)"), texts(".summary-weakest"));
        assertTrue(disabled("weekly-next"));
        assertTrue(disabled("weekly-save"));
    }

    @Test
    void withoutHabitsTheSummarySaysSo() {
        open(AppData.empty());

        assertEquals(List.of("W tym tygodniu nie było żadnych nawyków."),
                texts("#weekly-summary .habit-meta"));
        assertEquals(List.of(), texts(".summary-row"));
    }

    @Test
    void savingTheReviewStoresItOnTheMondayOfTheWeek() {
        open(UiTestData.sample());

        type("weekly-text", "Tydzień był dobry.");
        assertTrue(node("weekly-dirty").isVisible());
        click("weekly-save");

        Note review = notes(NoteType.WEEKLY_REVIEW).get(0);
        assertEquals(MONDAY, review.date());
        assertEquals("Tydzień był dobry.", review.text());
        assertEquals("Zapisano przegląd tygodnia: 19–25 października 2026", model.statusProperty().get().text());
        assertFalse(node("weekly-dirty").isVisible());
    }

    @Test
    void clearingTheReviewRemovesIt() {
        open(UiTestData.sample().withNotes(List.of(
                new Note("w1", MONDAY, NoteType.WEEKLY_REVIEW, "Stary przegląd", List.of(), null))));
        assertEquals("Stary przegląd", textOf("weekly-text"));

        type("weekly-text", "");
        click("weekly-save");

        assertEquals(List.of(), model.data().notes());
        assertEquals("Usunięto przegląd tygodnia: 19–25 października 2026", model.statusProperty().get().text());
    }

    @Test
    void goingToAnotherWeekSavesTheUnsavedReviewAndShowsThatWeek() {
        open(UiTestData.sample());
        type("weekly-text", "Ten tydzień");

        click("weekly-previous");

        assertEquals("12–18 października 2026", ((Label) node("weekly-range")).getText());
        assertEquals("", textOf("weekly-text"));
        assertEquals(MONDAY, notes(NoteType.WEEKLY_REVIEW).get(0).date());
        assertFalse(disabled("weekly-next"));

        click("weekly-current");

        assertEquals("19–25 października 2026", ((Label) node("weekly-range")).getText());
        assertEquals("Ten tydzień", textOf("weekly-text"));
    }

    @Test
    void thePastWeekSummaryIsOfThatWeek() {
        open(UiTestData.sample());

        click("weekly-previous");

        assertEquals(List.of(
                "Spacer: 0/7 (0%) · Seria: 1 dzień",
                "Woda: 0/7 (0%) · Seria: 0 dni",
                "Bieg: 0/3 (0%) · Seria: 0 tygodni"), texts(".summary-row"));
    }

    // --- wolne notatki ---

    @Test
    void aFreeNoteCannotBeAddedEmptyAndIsStoredWithTodaysDateTagsAndHabit() {
        open(UiTestData.sample());
        assertTrue(disabled("free-add"));

        type("free-text", "  Pomysł na tydzień ");
        type("free-tags", "Praca");
        pick("free-habit", 3);
        assertFalse(disabled("free-add"));
        click("free-add");

        Note free = notes(NoteType.FREE).get(0);
        assertEquals(TODAY, free.date());
        assertEquals("Pomysł na tydzień", free.text());
        assertEquals(List.of("praca"), free.tags());
        assertEquals("bieg", free.habitId());
        assertEquals("Dodano notatkę", model.statusProperty().get().text());
        assertEquals("", textOf("free-text"));
        assertEquals("", textOf("free-tags"));
        assertTrue(disabled("free-add"));
    }

    @Test
    void severalFreeNotesCanBeAdded() {
        open(UiTestData.sample());

        type("free-text", "Pierwsza");
        click("free-add");
        type("free-text", "Druga");
        click("free-add");

        assertEquals(List.of("Pierwsza", "Druga"), notes(NoteType.FREE).stream().map(Note::text).toList());
    }

    // --- lista ---

    private AppData withThreeNotes() {
        return UiTestData.sample().withNotes(List.of(
                daily("d1", TODAY.minusDays(2), "Wpis A", List.of("sen", "energia"), "spacer"),
                new Note("w1", LocalDate.of(2026, 10, 12), NoteType.WEEKLY_REVIEW, "Przegląd", List.of(), null),
                new Note("f1", TODAY, NoteType.FREE, "Pomysł", List.of("praca"), null)));
    }

    @Test
    void theListShowsNewestFirstWithTypeDateTagsAndHabit() {
        open(withThreeNotes());

        assertEquals(List.of("Notatka", "Wpis dzienny", "Przegląd tygodnia"), texts(".note-type"));
        assertEquals(List.of("24.10.2026", "22.10.2026", "Tydzień 12–18 października 2026"), texts(".note-date"));
        assertEquals(List.of("Pomysł", "Wpis A", "Przegląd"), texts(".note-text"));
        assertEquals(List.of("#praca", "#sen #energia · Nawyk: Spacer"), texts(".note-meta"));
    }

    @Test
    void onlyFreeNotesCanBeRemovedFromTheList() {
        open(withThreeNotes());

        assertEquals(1, screen.lookupAll(".note-card .remove-button").size());
    }

    @Test
    void removingAFreeNoteAsksAndDeletesOnlyThatNote() {
        open(withThreeNotes());

        click0(".note-card .remove-button");

        assertEquals(List.of("Usunąć tę notatkę? Tej operacji nie można cofnąć."), questions);
        assertEquals(List.of(), notes(NoteType.FREE));
        assertEquals(2, model.data().notes().size());
        assertEquals("Usunięto notatkę", model.statusProperty().get().text());
        assertEquals(2, screen.lookupAll(".note-card").size());
    }

    @Test
    void decliningTheRemovalKeepsTheNote() {
        open(withThreeNotes());
        answer = false;

        click0(".note-card .remove-button");

        assertEquals(1, notes(NoteType.FREE).size());
    }

    private void click0(String selector) {
        FxTestSupport.onFx(((Button) screen.lookup(selector))::fire);
    }

    @Test
    void theTagFilterListsAllTagsAlphabeticallyAndNarrowsTheList() {
        open(withThreeNotes());
        ComboBox<?> filter = (ComboBox<?>) node("note-tag-filter");

        assertEquals(List.of("Wszystkie tagi", "energia", "praca", "sen"),
                filter.getItems().stream().map(Object::toString).toList());

        pick("note-tag-filter", 3);

        assertEquals(List.of("Wpis A"), texts(".note-text"));

        pick("note-tag-filter", 0);

        assertEquals(3, screen.lookupAll(".note-card").size());
    }

    @Test
    void aVanishedTagResetsTheFilter() {
        open(withThreeNotes());
        pick("note-tag-filter", 2);
        assertEquals(List.of("Pomysł"), texts(".note-text"));

        click0(".note-card .remove-button");

        assertEquals(0, ((ComboBox<?>) node("note-tag-filter")).getSelectionModel().getSelectedIndex());
        assertEquals(2, screen.lookupAll(".note-card").size());
    }

    @Test
    void withoutNotesTheListShowsAHint() {
        open(UiTestData.sample());

        assertEquals(List.of("Nie ma jeszcze żadnych wpisów."), texts(".note-empty"));
    }

    @Test
    void theListShowsAtMostFiftyNewestNotes() {
        List<Note> many = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            many.add(new Note("n" + i, TODAY.minusDays(i), NoteType.FREE, "Notatka " + i, List.of(), null));
        }
        open(UiTestData.sample().withNotes(many));

        assertEquals(NoteListPanel.LIMIT, screen.lookupAll(".note-card").size());
        assertEquals("Notatka 0", texts(".note-text").get(0));
        assertNull(screen.lookupAll(".note-text").stream()
                .filter(n -> ((Label) n).getText().equals("Notatka 59")).findFirst().orElse(null));
    }

    @Test
    void anEntrySavedInThePanelAppearsInTheList() {
        open(UiTestData.sample());

        type("daily-text", "Nowy wpis");
        type("daily-tags", "sen");
        click("daily-save");

        assertEquals(List.of("Nowy wpis"), texts(".note-text"));
        assertEquals(List.of("#sen"), texts(".note-meta"));
        assertEquals(List.of("Wszystkie tagi", "sen"),
                ((ComboBox<?>) node("note-tag-filter")).getItems().stream().map(Object::toString).toList());
    }
}
