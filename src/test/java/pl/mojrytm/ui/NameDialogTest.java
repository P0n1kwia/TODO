package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NameDialogTest {

    @BeforeAll
    static void toolkit() {
        FxTestSupport.assumeToolkit();
    }

    private static Dialog<String> open(String initial) {
        return FxTestSupport.onFx(() -> NameDialog.build(null, "Tytuł", "np. Zdrowie", initial,
                name -> name.trim().equalsIgnoreCase("sen")));
    }

    private static TextField field(Dialog<String> dialog) {
        return (TextField) dialog.getDialogPane().lookup("#name-field");
    }

    private static String problem(Dialog<String> dialog) {
        return ((Label) dialog.getDialogPane().lookup("#name-problem")).getText();
    }

    private static Node saveButton(Dialog<String> dialog) {
        return dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().get(0));
    }

    private static void type(Dialog<String> dialog, String text) {
        FxTestSupport.onFx(() -> field(dialog).setText(text));
    }

    @Test
    void emptyNameCannotBeSaved() {
        Dialog<String> dialog = open("");

        assertTrue(saveButton(dialog).isDisabled());
        assertEquals("Podaj nazwę.", problem(dialog));
    }

    @Test
    void aTakenNameCannotBeSavedEvenWithDifferentCase() {
        Dialog<String> dialog = open("");

        type(dialog, " SEN ");

        assertTrue(saveButton(dialog).isDisabled());
        assertTrue(problem(dialog).startsWith("Taka nazwa już istnieje"));
    }

    @Test
    void aFreeNameEnablesSavingAndClearsTheProblem() {
        Dialog<String> dialog = open("");

        type(dialog, "Ciało");

        assertFalse(saveButton(dialog).isDisabled());
        assertEquals("", problem(dialog));
    }

    @Test
    void savingReturnsTheTrimmedName() {
        Dialog<String> dialog = open("");
        type(dialog, "  Ciało ");
        AtomicReference<String> result = new AtomicReference<>();

        FxTestSupport.onFx(() -> {
            ((Button) saveButton(dialog)).fire();
            result.set(dialog.getResult());
        });

        assertEquals("Ciało", result.get());
    }

    @Test
    void cancelReturnsNothing() {
        Dialog<String> dialog = open("Ciało");
        ButtonType cancel = dialog.getDialogPane().getButtonTypes().get(1);
        AtomicReference<String> result = new AtomicReference<>("niezmienione");

        FxTestSupport.onFx(() -> {
            ((Button) dialog.getDialogPane().lookupButton(cancel)).fire();
            result.set(dialog.getResult());
        });

        assertNull(result.get());
    }

    @Test
    void startsWithTheGivenNameAndIsValidWhenItIsFree() {
        Dialog<String> dialog = open("Ciało");

        assertEquals("Ciało", field(dialog).getText());
        assertFalse(saveButton(dialog).isDisabled());
    }
}
