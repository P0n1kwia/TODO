package pl.mojrytm.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Counter;

/**
 * Zmiany na liście liczników „dni od / dni bez". Licznik to tylko nazwa i dzień startu; „zaczynam
 * od nowa" przesuwa start na dziś (poprzedni wynik nie jest zapamiętywany). Licznik można usunąć,
 * bo nie ma historii, którą trzeba chronić. Naruszenia to błąd programisty
 * ({@link IllegalArgumentException}).
 */
public final class CounterEditor {

    public AppData add(AppData data, Counter counter) {
        if (indexOf(data, counter.id()) >= 0) {
            throw new IllegalArgumentException("Licznik o takim id już istnieje: " + counter.id());
        }
        List<Counter> counters = new ArrayList<>(data.counters());
        counters.add(counter);
        return data.withCounters(counters);
    }

    public AppData rename(AppData data, String counterId, String newName) {
        int index = requireIndex(data, counterId);
        List<Counter> counters = new ArrayList<>(data.counters());
        counters.set(index, new Counter(counterId, newName, data.counters().get(index).startDate()));
        return data.withCounters(counters);
    }

    /** Ustawia start licznika na {@code today} (dzień logiczny podaje wołający). */
    public AppData restart(AppData data, String counterId, LocalDate today) {
        int index = requireIndex(data, counterId);
        Counter current = data.counters().get(index);
        if (current.startDate().equals(today)) {
            return data;
        }
        List<Counter> counters = new ArrayList<>(data.counters());
        counters.set(index, new Counter(counterId, current.name(), today));
        return data.withCounters(counters);
    }

    public AppData remove(AppData data, String counterId) {
        int index = requireIndex(data, counterId);
        List<Counter> counters = new ArrayList<>(data.counters());
        counters.remove(index);
        return data.withCounters(counters);
    }

    private static int requireIndex(AppData data, String counterId) {
        int index = indexOf(data, counterId);
        if (index < 0) {
            throw new IllegalArgumentException("Nie ma licznika o id: " + counterId);
        }
        return index;
    }

    private static int indexOf(AppData data, String counterId) {
        List<Counter> counters = data.counters();
        for (int i = 0; i < counters.size(); i++) {
            if (counters.get(i).id().equals(counterId)) {
                return i;
            }
        }
        return -1;
    }
}
