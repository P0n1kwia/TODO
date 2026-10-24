package pl.mojrytm.service;

import java.util.ArrayList;
import java.util.List;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Area;
import pl.mojrytm.model.Ids;

/**
 * Zmiany na liście obszarów. Obszarów nie usuwamy, tylko archiwizujemy; archiwizacja nie rusza
 * celów ani nawyków (ich historia zostaje). Nazwy są unikalne bez względu na wielkość liter,
 * także wśród zarchiwizowanych (zamiast duplikatu przywracamy stary obszar).
 * Naruszenia to błąd programisty ({@link IllegalArgumentException}): UI sprawdza je wcześniej
 * przez {@link #nameTaken}.
 */
public final class AreaEditor {

    /** Czy inny obszar niż {@code exceptAreaId} (null = żaden wyjątek) ma już taką nazwę. */
    public boolean nameTaken(AppData data, String name, String exceptAreaId) {
        return data.areas().stream()
                .anyMatch(area -> !area.id().equals(exceptAreaId) && Names.same(area.name(), name));
    }

    public AppData add(AppData data, Area area) {
        if (indexOf(data, area.id()) >= 0) {
            throw new IllegalArgumentException("Obszar o takim id już istnieje: " + area.id());
        }
        if (nameTaken(data, area.name(), null)) {
            throw new IllegalArgumentException("Obszar o takiej nazwie już istnieje: " + area.name());
        }
        List<Area> areas = new ArrayList<>(data.areas());
        areas.add(area);
        return data.withAreas(areas);
    }

    /** Dodaje obszary o podanych nazwach, pomijając puste i te, które już są. */
    public AppData addAll(AppData data, List<String> names) {
        AppData result = data;
        for (String name : names) {
            if (!name.isBlank() && !nameTaken(result, name, null)) {
                result = add(result, new Area(Ids.newId(), name.trim(), false));
            }
        }
        return result;
    }

    public AppData rename(AppData data, String areaId, String newName) {
        int index = requireIndex(data, areaId);
        if (nameTaken(data, newName, areaId)) {
            throw new IllegalArgumentException("Obszar o takiej nazwie już istnieje: " + newName);
        }
        List<Area> areas = new ArrayList<>(data.areas());
        areas.set(index, new Area(areaId, newName, data.areas().get(index).archived()));
        return data.withAreas(areas);
    }

    public AppData setArchived(AppData data, String areaId, boolean archived) {
        int index = requireIndex(data, areaId);
        Area current = data.areas().get(index);
        if (current.archived() == archived) {
            return data;
        }
        List<Area> areas = new ArrayList<>(data.areas());
        areas.set(index, current.withArchived(archived));
        return data.withAreas(areas);
    }

    private static int requireIndex(AppData data, String areaId) {
        int index = indexOf(data, areaId);
        if (index < 0) {
            throw new IllegalArgumentException("Nie ma obszaru o id: " + areaId);
        }
        return index;
    }

    private static int indexOf(AppData data, String areaId) {
        List<Area> areas = data.areas();
        for (int i = 0; i < areas.size(); i++) {
            if (areas.get(i).id().equals(areaId)) {
                return i;
            }
        }
        return -1;
    }
}
