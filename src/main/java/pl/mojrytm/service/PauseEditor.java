package pl.mojrytm.service;

import java.util.ArrayList;
import java.util.List;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Pause;

/** Dodawanie i usuwanie pauz. Pauzy mogą dotyczyć przeszłości (uzupełnienie) i przyszłości (plan). */
public final class PauseEditor {

    public AppData add(AppData data, Pause pause) {
        List<Pause> pauses = new ArrayList<>(data.pauses());
        pauses.add(pause);
        return data.withPauses(pauses);
    }

    /** Usuwa pierwszą pauzę równą podanej; gdy jej nie ma, zwraca te same dane. */
    public AppData remove(AppData data, Pause pause) {
        List<Pause> pauses = new ArrayList<>(data.pauses());
        return pauses.remove(pause) ? data.withPauses(pauses) : data;
    }
}
