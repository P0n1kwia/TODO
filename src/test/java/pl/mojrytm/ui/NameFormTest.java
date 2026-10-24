package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import pl.mojrytm.ui.NameForm.Problem;

class NameFormTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void blankNameIsRejected(String name) {
        assertEquals(Optional.of(Problem.NAME_EMPTY), new NameForm(name, false).problem());
    }

    @Test
    void takenNameIsRejected() {
        assertEquals(Optional.of(Problem.NAME_TAKEN), new NameForm("Sen", true).problem());
    }

    @Test
    void emptyIsReportedBeforeTaken() {
        assertEquals(Optional.of(Problem.NAME_EMPTY), new NameForm("  ", true).problem());
    }

    @Test
    void validNameHasNoProblemAndIsTrimmed() {
        NameForm form = new NameForm("  Ciało ", false);

        assertEquals(Optional.empty(), form.problem());
        assertEquals("Ciało", form.cleanName());
    }

    @Test
    void anInvalidFormHasNoCleanName() {
        assertThrows(IllegalStateException.class, () -> new NameForm("", false).cleanName());
        assertThrows(IllegalStateException.class, () -> new NameForm("Sen", true).cleanName());
    }

    @ParameterizedTest
    @EnumSource(Problem.class)
    void everyProblemHasAMessage(Problem problem) {
        assertTrue(Messages.has("name.form.problem." + problem.name()));
    }
}
