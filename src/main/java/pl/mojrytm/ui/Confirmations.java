package pl.mojrytm.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

/** Pytanie "Tak / Nie" przed operacją, której nie da się cofnąć. */
final class Confirmations {

    private Confirmations() {
    }

    /** Pokazuje pytanie i czeka; true tylko po kliknięciu "Tak". */
    static boolean ask(String question) {
        ButtonType yes = new ButtonType(Messages.get("button.yes"), ButtonBar.ButtonData.YES);
        ButtonType no = new ButtonType(Messages.get("button.no"), ButtonBar.ButtonData.NO);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, question, yes, no);
        alert.setHeaderText(null);
        alert.setTitle(Messages.get("app.title"));
        return alert.showAndWait().filter(button -> button == yes).isPresent();
    }
}
