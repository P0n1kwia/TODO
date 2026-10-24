package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Area;
import pl.mojrytm.model.Goal;
import pl.mojrytm.storage.JsonStorage;

class GoalsScreenTest {

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock("2026-10-24T10:00:00Z");
    private AppModel model;
    private GoalsScreen screen;

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
        screen = FxTestSupport.onFx(() -> new GoalsScreen(model));
    }

    private List<String> texts(String styleClass) {
        return screen.lookupAll(styleClass).stream().map(node -> ((Label) node).getText()).toList();
    }

    private Node card(String areaName) {
        return screen.lookupAll(".area-card").stream()
                .filter(card -> ((Label) card.lookup(".area-name")).getText().equals(areaName))
                .findFirst().orElseThrow();
    }

    private static Node goalRow(Node card, String goalName) {
        return card.lookupAll(".goal-row").stream()
                .filter(row -> ((Label) row.lookup(".goal-name")).getText().equals(goalName))
                .findFirst().orElseThrow();
    }

    private static void click(Node button) {
        FxTestSupport.onFx(((Button) button)::fire);
    }

    private Area area(String id) {
        return model.data().areas().stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();
    }

    private Goal goal(String id) {
        return model.data().goals().stream().filter(g -> g.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void withoutAreasOffersToCreateOneOrStartFromTheDefaults() {
        open(AppData.empty());

        assertEquals(List.of("Nie masz jeszcze żadnych obszarów. Dodaj własny albo zacznij od przykładowych."),
                texts(".habit-meta").subList(1, 2));
        assertEquals(1, screen.lookupAll(".new-area-button").size());
        assertEquals(1, screen.lookupAll(".defaults-button").size());
        assertEquals(List.of(), texts(".area-name"));
    }

    @Test
    void defaultAreasAreAddedOnceAndTheButtonDisappears() {
        open(AppData.empty());

        click(screen.lookup(".defaults-button"));

        assertEquals(List.of("Sen", "Ciało", "Pieniądze", "Relacje"), texts(".area-name"));
        assertEquals(0, screen.lookupAll(".defaults-button").size());
        assertEquals("Dodano przykładowe obszary: Sen, Ciało, Pieniądze, Relacje",
                model.statusProperty().get().text());
        assertEquals(List.of("Sen", "Ciało", "Pieniądze", "Relacje"),
                model.data().areas().stream().map(Area::name).toList());
    }

    @Test
    void listsActiveAreasWithGoalsAndHabitCountsThenArchivedAreas() {
        open(UiTestData.sampleWithGoals());

        assertEquals(List.of("Sen", "Ciało", "Stary"), texts(".area-name"));
        assertEquals(List.of("Spać 8 godzin", "Wygasły", "Biegać", "Dawny cel"), texts(".goal-name"));
        // Spać 8 godzin: Spacer i Woda; Wygasły: tylko zarchiwizowany nawyk, więc 0
        assertEquals(List.of("2 nawyki", "0 nawyków", "1 nawyk", "0 nawyków"),
                texts(".goal-row .habit-meta"));
        assertEquals(List.of("Zarchiwizowane obszary"), texts(".section-title"));
        assertTrue(card("Stary").getStyleClass().contains("archived"));
        assertFalse(card("Sen").getStyleClass().contains("archived"));
    }

    @Test
    void anAreaWithoutGoalsSaysSo() {
        open(AppData.empty().withAreas(List.of(new Area("sen", "Sen", false))));

        assertEquals(List.of("Ten obszar nie ma jeszcze celów."), texts(".area-card .habit-meta"));
    }

    @Test
    void archivingAnAreaKeepsItsGoalsAndHabitsAndMovesItToTheArchivedList() {
        open(UiTestData.sampleWithGoals());

        click(card("Ciało").lookup(".archive-area-button"));

        assertTrue(area("cialo").archived());
        assertFalse(goal("c1").archived());
        assertEquals(4, model.data().habits().size());
        assertEquals(List.of("Sen", "Ciało", "Stary"), texts(".area-name"));
        assertTrue(card("Ciało").getStyleClass().contains("archived"));
        assertEquals("Zarchiwizowano obszar: Ciało. Jego cele i nawyki zostają, możesz go przywrócić w zakładce Cele.",
                model.statusProperty().get().text());
    }

    @Test
    void restoringAnAreaBringsItBack() {
        open(UiTestData.sampleWithGoals());

        click(card("Stary").lookup(".archive-area-button"));

        assertFalse(area("old").archived());
        assertFalse(card("Stary").getStyleClass().contains("archived"));
        assertEquals(List.of(), texts(".section-title"));
        assertEquals("Przywrócono obszar: Stary", model.statusProperty().get().text());
    }

    @Test
    void archivingAGoalDimsItAndRestoringUndoesIt() {
        open(UiTestData.sampleWithGoals());

        click(goalRow(card("Ciało"), "Biegać").lookup(".archive-goal-button"));

        assertTrue(goal("c1").archived());
        assertTrue(goalRow(card("Ciało"), "Biegać").getStyleClass().contains("archived"));
        assertEquals("Zarchiwizowano cel: Biegać. Jego nawyki zostają, możesz go przywrócić w zakładce Cele.",
                model.statusProperty().get().text());

        click(goalRow(card("Ciało"), "Biegać").lookup(".archive-goal-button"));

        assertFalse(goal("c1").archived());
        assertEquals("Przywrócono cel: Biegać", model.statusProperty().get().text());
    }

    @Test
    void archiveButtonsCarryTheirStateInTheirLabel() {
        open(UiTestData.sampleWithGoals());

        assertEquals("Archiwizuj", ((Button) card("Sen").lookup(".archive-area-button")).getText());
        assertEquals("Przywróć", ((Button) card("Stary").lookup(".archive-area-button")).getText());
        assertEquals("Przywróć", ((Button) goalRow(card("Sen"), "Wygasły").lookup(".archive-goal-button")).getText());
    }

    @Test
    void anArchivedAreaCannotGetGoalsOrHaveItsGoalsChanged() {
        open(UiTestData.sampleWithGoals());

        Node archived = card("Stary");

        assertEquals(null, archived.lookup(".add-goal-button"));
        assertEquals(null, archived.lookup(".rename-goal-button"));
        assertEquals(null, archived.lookup(".archive-goal-button"));
        assertEquals(1, archived.lookupAll(".rename-area-button").size());
        assertEquals(1, card("Sen").lookupAll(".add-goal-button").size());
    }

    @Test
    void redrawsWhenDataChangesElsewhere() {
        open(AppData.empty());

        FxTestSupport.onFx(() -> model.update(data -> model.areas().add(data, new Area("a", "Sen", false))));

        assertEquals(List.of("Sen"), texts(".area-name"));
        assertEquals(0, screen.lookupAll(".defaults-button").size());
    }
}
