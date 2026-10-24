package pl.mojrytm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Area;
import pl.mojrytm.model.Goal;

class GoalEditorTest {

    private final GoalEditor editor = new GoalEditor();

    private static Goal goal(String id, String areaId, String name) {
        return new Goal(id, areaId, name, false);
    }

    /** Dwa obszary: "sen" i "cialo". */
    private static AppData withAreas() {
        return AppData.empty().withAreas(List.of(new Area("sen", "Sen", false), new Area("cialo", "Ciało", false)));
    }

    private static AppData with(Goal... goals) {
        return withAreas().withGoals(List.of(goals));
    }

    @Test
    void addAppendsAtTheEnd() {
        AppData data = editor.add(editor.add(withAreas(), goal("g1", "sen", "Spać 8 godzin")),
                goal("g2", "cialo", "Biegać"));

        assertEquals(List.of("g1", "g2"), data.goals().stream().map(Goal::id).toList());
    }

    @Test
    void addingTheSameIdTwiceIsRejected() {
        AppData data = with(goal("g1", "sen", "Spać 8 godzin"));

        assertThrows(IllegalArgumentException.class, () -> editor.add(data, goal("g1", "cialo", "Inny")));
    }

    @Test
    void addingToAnUnknownAreaIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> editor.add(withAreas(), goal("g1", "nie-ma", "Cel")));
    }

    @Test
    void namesAreUniqueWithinOneAreaIgnoringCase() {
        AppData data = with(goal("g1", "sen", "Spać 8 godzin"));

        assertTrue(editor.nameTaken(data, "sen", "spać 8 GODZIN", null));
        assertFalse(editor.nameTaken(data, "cialo", "Spać 8 godzin", null));
        assertThrows(IllegalArgumentException.class, () -> editor.add(data, goal("g2", "sen", "spać 8 godzin")));
        assertEquals(2, editor.add(data, goal("g2", "cialo", "Spać 8 godzin")).goals().size());
    }

    @Test
    void anArchivedGoalStillBlocksItsName() {
        AppData data = withAreas().withGoals(List.of(new Goal("g1", "sen", "Cel", true)));

        assertTrue(editor.nameTaken(data, "sen", "Cel", null));
    }

    @Test
    void nameIsNotTakenByTheGoalBeingRenamed() {
        AppData data = with(goal("g1", "sen", "Cel A"), goal("g2", "sen", "Cel B"));

        assertFalse(editor.nameTaken(data, "sen", "CEL A", "g1"));
        assertTrue(editor.nameTaken(data, "sen", "Cel A", "g2"));
    }

    @Test
    void renameKeepsAreaOrderAndArchiveFlag() {
        AppData data = withAreas().withGoals(List.of(goal("g1", "sen", "Cel A"), new Goal("g2", "cialo", "Cel B", true)));

        AppData renamed = editor.rename(data, "g2", "Nowa nazwa");

        assertEquals(List.of(goal("g1", "sen", "Cel A"), new Goal("g2", "cialo", "Nowa nazwa", true)), renamed.goals());
    }

    @Test
    void renameToAnotherGoalOfTheSameAreaIsRejected() {
        AppData data = with(goal("g1", "sen", "Cel A"), goal("g2", "sen", "Cel B"));

        assertThrows(IllegalArgumentException.class, () -> editor.rename(data, "g2", "cel a"));
    }

    @Test
    void renamingAnUnknownGoalIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> editor.rename(AppData.empty(), "x", "Cel"));
    }

    @Test
    void archivingAndRestoringChangesOnlyTheFlag() {
        AppData data = with(goal("g1", "sen", "Cel A"), goal("g2", "sen", "Cel B"));

        AppData archived = editor.setArchived(data, "g1", true);

        assertEquals(List.of(new Goal("g1", "sen", "Cel A", true), goal("g2", "sen", "Cel B")), archived.goals());
        assertEquals(data, editor.setArchived(archived, "g1", false));
    }

    @Test
    void settingTheSameArchiveStateReturnsTheSameData() {
        AppData data = with(goal("g1", "sen", "Cel A"));

        assertSame(data, editor.setArchived(data, "g1", false));
    }

    @Test
    void archivingAnUnknownGoalIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> editor.setArchived(AppData.empty(), "x", true));
    }
}
