package pl.mojrytm.ui;

import java.util.function.Predicate;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Ekran „Dziennik": wpis dzienny, przegląd tygodnia, wolne notatki i lista ostatnich wpisów. Panele
 * zostają na miejscu przy zmianie danych (żeby nie gubić pisanego tekstu), a niezapisany tekst jest
 * zapisywany przy zmianie dnia lub tygodnia, przy opuszczeniu ekranu i przy zamknięciu okna.
 */
final class JournalScreen extends VBox {

    private final DailyNotePanel daily;
    private final WeeklyReviewPanel weekly;

    JournalScreen(AppModel model) {
        this(model, Confirmations::ask);
    }

    /** @param confirm pokazuje pytanie (tekst) i zwraca true, gdy użytkownik się zgodził */
    JournalScreen(AppModel model, Predicate<String> confirm) {
        super(18);
        getStyleClass().add("screen-content");
        Label title = new Label(Messages.get("journal.title"));
        title.getStyleClass().add("screen-title");
        Label intro = new Label(Messages.get("journal.intro"));
        intro.getStyleClass().add("habit-meta");
        intro.setWrapText(true);

        daily = new DailyNotePanel(model);
        weekly = new WeeklyReviewPanel(model);
        getChildren().addAll(title, intro, daily, weekly, new FreeNotesPanel(model),
                new NoteListPanel(model, confirm));

        // zamknięcie okna z niezapisanym tekstem też go zapisuje (przed wymuszonym zapisem danych)
        model.addCloseHook(this::saveUnsavedText);
        // ekran znika ze sceny, gdy użytkownik przechodzi do innej zakładki
        sceneProperty().addListener((observable, old, current) -> {
            if (current == null) {
                saveUnsavedText();
            }
        });
    }

    /** Zapisuje niezapisany wpis dzienny i przegląd tygodnia. */
    void saveUnsavedText() {
        daily.saveIfDirty();
        weekly.saveIfDirty();
    }
}
