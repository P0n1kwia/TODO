package pl.mojrytm;

import javafx.application.Application;
import pl.mojrytm.ui.MojRytmApp;

/**
 * Punkt wejścia aplikacji. Osobna klasa, bo JavaFX odmawia startu, gdy główna klasa
 * rozszerza {@link Application}, a uruchamiamy z classpath (jar, jpackage).
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(MojRytmApp.class, args);
    }
}
