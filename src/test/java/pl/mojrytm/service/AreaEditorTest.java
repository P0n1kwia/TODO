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

class AreaEditorTest {

    private final AreaEditor editor = new AreaEditor();

    private static Area area(String id, String name) {
        return new Area(id, name, false);
    }

    private static AppData with(Area... areas) {
        return AppData.empty().withAreas(List.of(areas));
    }

    private static List<String> names(AppData data) {
        return data.areas().stream().map(Area::name).toList();
    }

    @Test
    void addAppendsAtTheEnd() {
        AppData data = editor.add(editor.add(AppData.empty(), area("a", "Sen")), area("b", "Ciało"));

        assertEquals(List.of("Sen", "Ciało"), names(data));
    }

    @Test
    void addingTheSameIdTwiceIsRejected() {
        AppData data = with(area("a", "Sen"));

        assertThrows(IllegalArgumentException.class, () -> editor.add(data, area("a", "Ciało")));
    }

    @Test
    void namesAreUniqueIgnoringCaseAndSurroundingSpaces() {
        AppData data = with(area("a", "Sen"));

        assertTrue(editor.nameTaken(data, "sen", null));
        assertTrue(editor.nameTaken(data, "  SEN ", null));
        assertFalse(editor.nameTaken(data, "Ciało", null));
        assertThrows(IllegalArgumentException.class, () -> editor.add(data, area("b", "sen")));
    }

    @Test
    void anArchivedAreaStillBlocksItsName() {
        AppData data = with(new Area("a", "Sen", true));

        assertTrue(editor.nameTaken(data, "Sen", null));
    }

    @Test
    void nameIsNotTakenByTheAreaBeingRenamed() {
        AppData data = with(area("a", "Sen"), area("b", "Ciało"));

        assertFalse(editor.nameTaken(data, "SEN", "a"));
        assertTrue(editor.nameTaken(data, "Sen", "b"));
    }

    @Test
    void renameChangesOnlyThatAreaKeepingOrderAndArchiveFlag() {
        AppData data = with(area("a", "Sen"), new Area("b", "Ciało", true));

        AppData renamed = editor.rename(editor.rename(data, "a", "Sen i regeneracja"), "b", "Sport");

        assertEquals(List.of(area("a", "Sen i regeneracja"), new Area("b", "Sport", true)), renamed.areas());
    }

    @Test
    void renameCanOnlyChangeLetterCaseOfTheSameName() {
        AppData data = with(area("a", "sen"));

        assertEquals(List.of("Sen"), names(editor.rename(data, "a", "Sen")));
    }

    @Test
    void renameToAnotherAreasNameIsRejected() {
        AppData data = with(area("a", "Sen"), area("b", "Ciało"));

        assertThrows(IllegalArgumentException.class, () -> editor.rename(data, "b", "sen"));
    }

    @Test
    void renamingAnUnknownAreaIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> editor.rename(AppData.empty(), "x", "Sen"));
    }

    @Test
    void archivingAndRestoringChangesOnlyTheFlag() {
        AppData data = with(area("a", "Sen"), area("b", "Ciało"));

        AppData archived = editor.setArchived(data, "a", true);

        assertEquals(List.of(new Area("a", "Sen", true), area("b", "Ciało")), archived.areas());
        assertEquals(data, editor.setArchived(archived, "a", false));
    }

    @Test
    void settingTheSameArchiveStateReturnsTheSameData() {
        AppData data = with(area("a", "Sen"));

        assertSame(data, editor.setArchived(data, "a", false));
    }

    @Test
    void archivingAnUnknownAreaIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> editor.setArchived(AppData.empty(), "x", true));
    }

    @Test
    void addAllAddsEachNameOnceAsAnActiveAreaWithItsOwnId() {
        AppData data = editor.addAll(AppData.empty(), List.of(" Sen ", "Ciało", "sen", "  "));

        assertEquals(List.of("Sen", "Ciało"), names(data));
        assertFalse(data.areas().get(0).archived());
        assertFalse(data.areas().get(0).id().equals(data.areas().get(1).id()));
    }

    @Test
    void addAllSkipsNamesThatAlreadyExist() {
        AppData data = with(new Area("a", "Sen", true), area("b", "Ciało"));

        AppData result = editor.addAll(data, List.of("Sen", "Pieniądze", "ciało"));

        assertEquals(List.of("Sen", "Ciało", "Pieniądze"), names(result));
        assertEquals("a", result.areas().get(0).id());
    }
}
