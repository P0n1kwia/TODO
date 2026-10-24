package pl.mojrytm.ui;

import java.util.Optional;
import java.util.function.Predicate;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

/** Okno z jednym polem: nazwa nowego albo zmienianego obszaru lub celu. Walidację robi {@link NameForm}. */
final class NameDialog {

    private NameDialog() {
    }

    /**
     * @param taken czy podana nazwa koliduje z inną (np. z innym obszarem); sprawdzane przy każdej zmianie tekstu
     * @return nazwa bez spacji na brzegach albo pusty wynik po anulowaniu
     */
    static Optional<String> show(Window owner, String title, String prompt, String initial, Predicate<String> taken) {
        return build(owner, title, prompt, initial, taken).showAndWait();
    }

    /** Zbudowane okno bez pokazywania (osobno, żeby dało się je sprawdzić w teście). */
    static Dialog<String> build(Window owner, String title, String prompt, String initial, Predicate<String> taken) {
        Dialog<String> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        ButtonType save = new ButtonType(Messages.get("dialog.name.save"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(Messages.get("dialog.name.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(save, cancel);

        TextField name = new TextField(initial);
        name.setId("name-field");
        name.setPromptText(prompt);
        name.setPrefColumnCount(28);

        Label problem = new Label();
        problem.setId("name-problem");
        problem.getStyleClass().add("form-problem");
        problem.setWrapText(true);
        problem.setMaxWidth(380);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label(Messages.get("dialog.name.label")), name);
        grid.add(problem, 1, 1);
        dialog.getDialogPane().setContent(grid);

        Node saveButton = dialog.getDialogPane().lookupButton(save);
        Runnable refresh = () -> {
            Optional<NameForm.Problem> found = form(name, taken).problem();
            saveButton.setDisable(found.isPresent());
            problem.setText(found.map(p -> Messages.get("name.form.problem." + p.name())).orElse(""));
        };
        name.textProperty().addListener((observable, previous, current) -> refresh.run());
        refresh.run();

        dialog.setResultConverter(button -> button == save ? form(name, taken).cleanName() : null);
        Platform.runLater(name::requestFocus);
        return dialog;
    }

    private static NameForm form(TextField name, Predicate<String> taken) {
        String text = name.getText();
        return new NameForm(text, !text.isBlank() && taken.test(text));
    }
}
