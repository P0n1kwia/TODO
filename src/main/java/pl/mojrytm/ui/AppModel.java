package pl.mojrytm.ui;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import pl.mojrytm.model.AppData;
import pl.mojrytm.service.AreaEditor;
import pl.mojrytm.service.CounterBoard;
import pl.mojrytm.service.CounterEditor;
import pl.mojrytm.service.EntryEditor;
import pl.mojrytm.service.EntryRejectedException;
import pl.mojrytm.service.GoalBoard;
import pl.mojrytm.service.GoalEditor;
import pl.mojrytm.service.HabitBoard;
import pl.mojrytm.service.HabitEditor;
import pl.mojrytm.service.LogicalClock;
import pl.mojrytm.service.NoteBoard;
import pl.mojrytm.service.NoteEditor;
import pl.mojrytm.service.PauseEditor;
import pl.mojrytm.service.WheelBoard;
import pl.mojrytm.service.WheelEditor;
import pl.mojrytm.storage.DataStore;
import pl.mojrytm.storage.JsonStorage;

/**
 * Stan aplikacji dla widoków: bieżące dane, dzisiejszy dzień logiczny i komunikat w pasku stanu.
 * Widoki obserwują {@link #dataProperty()} i {@link #todayProperty()} i rysują się od nowa.
 * Wszystkie metody wołamy z wątku JavaFX; zapis na dysk odbywa się w tle w {@link DataStore}.
 */
final class AppModel {

    private final DataStore store;
    private final LogicalClock clock;
    private final HabitBoard board = new HabitBoard();
    private final GoalBoard goalBoard = new GoalBoard();
    private final CounterBoard counterBoard = new CounterBoard();
    private final NoteBoard noteBoard = new NoteBoard();
    private final WheelBoard wheelBoard = new WheelBoard();
    private final EntryEditor entryEditor;
    private final HabitEditor habitEditor = new HabitEditor();
    private final PauseEditor pauseEditor = new PauseEditor();
    private final AreaEditor areaEditor = new AreaEditor();
    private final GoalEditor goalEditor = new GoalEditor();
    private final CounterEditor counterEditor = new CounterEditor();
    private final NoteEditor noteEditor = new NoteEditor();
    private final WheelEditor wheelEditor = new WheelEditor();

    private final List<Runnable> closeHooks = new ArrayList<>();

    private final ObjectProperty<AppData> data = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> today = new SimpleObjectProperty<>();
    private final ObjectProperty<StatusMessage> status = new SimpleObjectProperty<>();

    AppModel(AppData initial, JsonStorage storage, Clock clock) {
        this.clock = new LogicalClock(clock);
        this.entryEditor = new EntryEditor(this.clock);
        this.store = DataStore.open(initial, storage,
                error -> Platform.runLater(() -> showError(Messages.format("status.saveError", error.getMessage()))));
        this.data.set(initial);
        this.today.set(this.clock.today(initial.settings()));
    }

    AppData data() {
        return data.get();
    }

    ReadOnlyObjectProperty<AppData> dataProperty() {
        return data;
    }

    LocalDate today() {
        return today.get();
    }

    ReadOnlyObjectProperty<LocalDate> todayProperty() {
        return today;
    }

    ReadOnlyObjectProperty<StatusMessage> statusProperty() {
        return status;
    }

    HabitBoard board() {
        return board;
    }

    GoalBoard goalBoard() {
        return goalBoard;
    }

    CounterBoard counterBoard() {
        return counterBoard;
    }

    NoteBoard noteBoard() {
        return noteBoard;
    }

    WheelBoard wheelBoard() {
        return wheelBoard;
    }

    EntryEditor entries() {
        return entryEditor;
    }

    HabitEditor habits() {
        return habitEditor;
    }

    PauseEditor pauses() {
        return pauseEditor;
    }

    AreaEditor areas() {
        return areaEditor;
    }

    GoalEditor goals() {
        return goalEditor;
    }

    CounterEditor counters() {
        return counterEditor;
    }

    NoteEditor notes() {
        return noteEditor;
    }

    WheelEditor wheel() {
        return wheelEditor;
    }

    /**
     * Zmienia dane i planuje zapis. Naruszenie reguły wpisów pokazuje komunikat i zostawia dane bez zmian
     * (wynik false). Inne wyjątki to błędy programu i lecą dalej.
     */
    boolean update(UnaryOperator<AppData> change) {
        try {
            store.update(change);
        } catch (EntryRejectedException e) {
            showError(Messages.get("error.entry." + e.reason().name()));
            return false;
        }
        AppData updated = store.get();
        data.set(updated);
        today.set(clock.today(updated.settings()));
        return true;
    }

    /**
     * Rejestruje czynność, którą trzeba wykonać tuż przed zamknięciem okna, jeszcze przed wymuszonym
     * zapisem danych (np. zapis niezapisanego tekstu z pola edycji).
     */
    void addCloseHook(Runnable hook) {
        closeHooks.add(hook);
    }

    /** Wykonuje zarejestrowane czynności zamknięcia, w kolejności rejestracji, na wątku JavaFX. */
    void runCloseHooks() {
        closeHooks.forEach(Runnable::run);
    }

    /** Sprawdza, czy zmienił się dzień logiczny (np. minęła 4:00); wołane okresowo. */
    void refreshToday() {
        today.set(clock.today(data.get().settings()));
    }

    void showInfo(String text) {
        status.set(new StatusMessage(text, false));
    }

    void showError(String text) {
        status.set(new StatusMessage(text, true));
    }

    void clearStatus() {
        status.set(null);
    }

    /** Wymusza zapis i zamyka magazyn. Gdy zapis się nie uda, można wywołać ponownie. */
    void shutdown() throws IOException {
        store.flush();
        store.close();
    }
}
