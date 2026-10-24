package pl.mojrytm.ui;

import java.time.Clock;
import javafx.application.Application;
import javafx.stage.Stage;
import pl.mojrytm.storage.JsonStorage;

/** Aplikacja JavaFX: całą pracę przy starcie i zamknięciu wykonuje {@link StartupFlow}. */
public final class MojRytmApp extends Application {

    @Override
    public void start(Stage stage) {
        Clock clock = Clock.systemDefaultZone();
        new StartupFlow(stage, new JsonStorage(DataLocation.resolve(), clock), clock).run();
    }
}
