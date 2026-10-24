package pl.mojrytm.ui;

import java.time.LocalDate;
import java.util.Optional;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import pl.mojrytm.model.Habit;

/** Okno ustawienia ilości dla wybranego dnia nawyku typu COUNT (uzupełnianie przeszłych dni). */
final class AmountDialog {

    private AmountDialog() {
    }

    static Optional<AmountChoice> show(Window owner, Habit habit, LocalDate date, double currentAmount,
                                       boolean currentMinimal) {
        Dialog<AmountChoice> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(Messages.get("dialog.amount.title"));
        dialog.setHeaderText(habit.name() + ": " + Formats.longDate(date));
        ButtonType save = new ButtonType(Messages.get("dialog.habit.save"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(Messages.get("dialog.habit.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(save, cancel);

        TextField field = new TextField(Formats.amount(currentAmount));
        field.setPrefColumnCount(10);
        Label caption = new Label(Messages.format("dialog.amount.caption", Formats.target(habit)));
        VBox content = new VBox(10, caption, field);
        content.setPadding(new Insets(12));

        CheckBox minimal = new CheckBox();
        if (habit.minimumVersion() != null) {
            minimal.setText(Messages.format("dialog.amount.minimal", habit.minimumVersion()));
            minimal.setSelected(currentMinimal);
            content.getChildren().add(minimal);
        }
        dialog.getDialogPane().setContent(content);

        Node saveButton = dialog.getDialogPane().lookupButton(save);
        Runnable refresh = () -> saveButton.setDisable(AmountChoice.parse(field.getText(), minimal.isSelected()).isEmpty());
        field.textProperty().addListener((observable, previous, current) -> refresh.run());
        refresh.run();

        dialog.setResultConverter(button -> button == save
                ? AmountChoice.parse(field.getText(), minimal.isSelected()).orElse(null)
                : null);
        Platform.runLater(() -> {
            field.requestFocus();
            field.selectAll();
        });
        return dialog.showAndWait();
    }
}
