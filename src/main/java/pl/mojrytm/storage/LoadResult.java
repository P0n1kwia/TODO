package pl.mojrytm.storage;

import java.nio.file.Path;
import pl.mojrytm.model.AppData;

/** Wynik otwarcia pliku danych. UI decyduje, co pokazać w każdym z przypadków. */
public sealed interface LoadResult {

    /** Dane wczytane (albo puste, gdy pliku jeszcze nie ma). */
    record Loaded(AppData data) implements LoadResult {
    }

    /**
     * Plik był uszkodzony. Nie został nadpisany, tylko przeniesiony do {@code corruptFile}.
     *
     * @param reason powód uszkodzenia (do pokazania w komunikacie)
     */
    record Corrupt(Path corruptFile, String reason) implements LoadResult {
    }

    /** Plik pochodzi z nowszej wersji aplikacji. Nic nie zostało zmienione. */
    record NewerVersion(int fileVersion, int appVersion) implements LoadResult {
    }
}
