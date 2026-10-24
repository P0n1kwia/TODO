package pl.mojrytm.ui;

import java.nio.file.Path;
import java.time.Clock;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import pl.mojrytm.model.AppData;
import pl.mojrytm.storage.JsonStorage;
import pl.mojrytm.storage.LoadResult;

/**
 * Start i koniec aplikacji: wczytanie danych w tle, ekrany dla uszkodzonego pliku i pliku
 * z nowszej wersji, zbudowanie głównego okna oraz zamknięcie z wymuszonym zapisem.
 */
final class StartupFlow {

    private static final Duration DAY_CHECK_INTERVAL = Duration.seconds(30);

    private final Stage stage;
    private final JsonStorage storage;
    private final Clock clock;

    StartupFlow(Stage stage, JsonStorage storage, Clock clock) {
        this.stage = stage;
        this.storage = storage;
        this.clock = clock;
    }

    void run() {
        stage.setTitle(Messages.get("app.title"));
        stage.setScene(new Scene(new StackPane(new Label(Messages.get("startup.loading"))), 900, 600));
        stage.show();
        Background.run(storage::load, this::onLoaded, this::onLoadFailed);
    }

    private void onLoaded(LoadResult result) {
        switch (result) {
            case LoadResult.Loaded loaded -> openMainWindow(loaded.data());
            case LoadResult.Corrupt corrupt -> offerRecovery(corrupt);
            case LoadResult.NewerVersion newer -> {
                alert(Alert.AlertType.ERROR, "startup.newer.title",
                        Messages.format("startup.newer.text", String.valueOf(newer.fileVersion()),
                                String.valueOf(newer.appVersion())), new ButtonType(Messages.get("button.quit")));
                Platform.exit();
            }
        }
    }

    private void onLoadFailed(Throwable error) {
        alert(Alert.AlertType.ERROR, "startup.failed.title",
                Messages.format("startup.failed.text", String.valueOf(error.getMessage())),
                new ButtonType(Messages.get("button.quit")));
        Platform.exit();
    }

    /** Uszkodzony plik został już odłożony na bok; pytamy, czy przywrócić kopię, czy zacząć od nowa. */
    private void offerRecovery(LoadResult.Corrupt corrupt) {
        String details = Messages.format("startup.corrupt.text", corrupt.reason(), corrupt.corruptFile().toString());
        ButtonType quit = new ButtonType(Messages.get("button.quit"), ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType fresh = new ButtonType(Messages.get("startup.corrupt.fresh"), ButtonBar.ButtonData.OTHER);

        if (corrupt.latestBackup().isEmpty()) {
            ButtonType choice = alert(Alert.AlertType.WARNING, "startup.corrupt.title",
                    details + "\n\n" + Messages.get("startup.corrupt.nobackup"), fresh, quit);
            if (choice == fresh) {
                openMainWindow(AppData.empty());
            } else {
                Platform.exit();
            }
            return;
        }

        Path backup = corrupt.latestBackup().get();
        ButtonType restore = new ButtonType(
                Messages.format("startup.corrupt.restore", backup.getFileName().toString()),
                ButtonBar.ButtonData.OK_DONE);
        ButtonType choice = alert(Alert.AlertType.WARNING, "startup.corrupt.title", details, restore, fresh, quit);
        if (choice == restore) {
            Background.run(() -> storage.restoreFromBackup(backup), this::openMainWindow, this::onRestoreFailed);
        } else if (choice == fresh) {
            openMainWindow(AppData.empty());
        } else {
            Platform.exit();
        }
    }

    private void onRestoreFailed(Throwable error) {
        ButtonType fresh = new ButtonType(Messages.get("startup.corrupt.fresh"), ButtonBar.ButtonData.OTHER);
        ButtonType quit = new ButtonType(Messages.get("button.quit"), ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType choice = alert(Alert.AlertType.ERROR, "startup.corrupt.title",
                Messages.format("startup.restore.failed", String.valueOf(error.getMessage())), fresh, quit);
        if (choice == fresh) {
            openMainWindow(AppData.empty());
        } else {
            Platform.exit();
        }
    }

    private void openMainWindow(AppData data) {
        AppModel model = new AppModel(data, storage, clock);
        MainView view = new MainView(model, Screens.create(model));
        Scene scene = new Scene(view, 1000, 680);
        scene.getStylesheets().add(StartupFlow.class.getResource("app.css").toExternalForm());
        stage.setScene(scene);

        Timeline dayWatch = new Timeline(new KeyFrame(DAY_CHECK_INTERVAL, event -> model.refreshToday()));
        dayWatch.setCycleCount(Animation.INDEFINITE);
        dayWatch.play();

        stage.setOnCloseRequest(event -> {
            event.consume();
            model.runCloseHooks();
            shutdown(model);
        });
    }

    /** Zamknięcie: wymuszony zapis w tle, a dopiero potem wyjście. Przy błędzie można ponowić. */
    private void shutdown(AppModel model) {
        Background.run(() -> {
            model.shutdown();
            return Boolean.TRUE;
        }, done -> Platform.exit(), error -> {
            ButtonType retry = new ButtonType(Messages.get("shutdown.failed.retry"), ButtonBar.ButtonData.OK_DONE);
            ButtonType quit = new ButtonType(Messages.get("shutdown.failed.quit"), ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType choice = alert(Alert.AlertType.ERROR, "shutdown.failed.title",
                    Messages.format("shutdown.failed.text", String.valueOf(error.getMessage())), retry, quit);
            if (choice == retry) {
                shutdown(model);
            } else {
                Platform.exit();
            }
        });
    }

    /** Okno dialogowe z własnymi przyciskami; zwraca wybrany (albo ostatni, gdy zamknięto krzyżykiem). */
    private ButtonType alert(Alert.AlertType type, String titleKey, String text, ButtonType... buttons) {
        Alert alert = new Alert(type, text, buttons);
        alert.initOwner(stage);
        alert.setTitle(Messages.get(titleKey));
        alert.setHeaderText(null);
        return alert.showAndWait().orElse(buttons[buttons.length - 1]);
    }
}
