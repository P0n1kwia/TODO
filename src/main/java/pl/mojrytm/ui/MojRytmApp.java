package pl.mojrytm.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Główne okno aplikacji. Na razie szkielet, widoki dojdą w kolejnych krokach. */
public final class MojRytmApp extends Application {

    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane(new Label(Messages.get("app.placeholder")));
        stage.setTitle(Messages.get("app.title"));
        stage.setScene(new Scene(root, 900, 600));
        stage.show();
    }
}
