package pl.mojrytm.ui;

import javafx.scene.Node;
import javafx.stage.Window;

/** Pomoc przy oknach dialogowych. */
final class Windows {

    private Windows() {
    }

    /** Okno, w którym leży węzeł (właściciel dialogu); null, gdy węzła nie ma jeszcze na scenie. */
    static Window of(Node anchor) {
        return anchor.getScene() == null ? null : anchor.getScene().getWindow();
    }
}
