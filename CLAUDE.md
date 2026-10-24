# Mój Rytm: desktopowy tracker nawyków

Aplikacja desktopowa dla jednej osoby, napisana w Javie. Pomaga zmieniać życie małymi krokami: nawyki dzienne i tygodniowe, liczniki, zadania z limitem priorytetów, notatki, check-in nastroju oraz dashboard pokazujący, co działa, a co nie. Dane trzymane lokalnie w pliku JSON.

**Zasada naczelna:** aplikacja ma być jedną pętlą (zaplanuj, zrób, zobacz efekt, wyciągnij wnioski), a nie zbiorem niezależnych modułów. Przy każdej nowej funkcji pytaj, który krok pętli ona obsługuje. Jeśli żaden, nie dodawaj jej.

## Stos technologiczny

* Java 21 (LTS), Maven.
* JavaFX do interfejsu, w tym wbudowane `LineChart` i `BarChart` do dashboardu. Opcjonalnie motyw AtlantaFX.
* Jackson (`jackson-databind` + `jackson-datatype-jsr310`) do JSON.
* JUnit 5 do testów. Bez frameworków typu Spring.
* Instalator na końcu przez `jpackage`.
* Nowych zależności nie dodawaj bez uzgodnienia ze mną.

Pakiet bazowy: `pl.mojrytm` (do zmiany, jeśli wolę inny).

## Komendy

```
mvn clean verify        # kompilacja i wszystkie testy (musi przechodzić przed każdym commitem)
mvn test                # tylko testy
mvn javafx:run          # uruchomienie aplikacji (wymaga javafx-maven-plugin)
```

Po założeniu projektu uzupełnij tę sekcję o komendę `jpackage` i sprawdź, czy powyższe działają.

## Architektura

Cztery warstwy, zależności tylko w dół:

```
pl.mojrytm
├── model      czyste klasy danych, bez JavaFX i bez I/O
├── storage    odczyt i zapis JSON, kopie zapasowe, migracje
├── service    logika: serie, statystyki, tygodnie, dzień logiczny (bez UI)
└── ui         widoki JavaFX, bez liczenia czegokolwiek
```

* Widoki nie liczą serii ani procentów. Biorą gotowe wyniki z `service`.
* UI budujemy w kodzie Javy, bez FXML i bez SceneBuilder, chyba że zdecyduję inaczej.
* Żadnego I/O na wątku JavaFX. Zapis i odczyt przez `Task`, wynik do UI przez `Platform.runLater`.
* Czas zawsze przez wstrzyknięty `Clock`, nigdy `LocalDate.now()` w logice. Dzięki temu testy są deterministyczne.
* Nazwy klas, metod i zmiennych po angielsku. Teksty widoczne w UI i komentarze po polsku. Teksty UI trzymaj w jednym miejscu (`messages_pl.properties`), nie rozsiane po kodzie.

## Model danych

Zapisujemy tylko fakty. Serie, procenty i inne wartości wyliczane liczy kod, nigdy nie trafiają do pliku. Dzięki temu zmiana zasad liczenia nie psuje starych danych.

Hierarchia: Obszar → Cel → Nawyk.

* `Area`: id, name (np. Sen, Ciało, Pieniądze, Relacje)
* `Goal`: id, areaId, name, archived
* `Habit`: id, goalId (opcjonalnie), name, kind (`CHECK` lub `COUNT`), schedule (`DAILY` lub `WEEKLY` z `timesPerWeek` 1–7), targetAmount i unit (tylko `COUNT`), minimumVersion (tekst, opcjonalnie), created (LocalDate), archived
* `Entry`: id, habitId, date, value, minimal (czy zaliczone wersją minimalną)
* `Task`: id, title, status (`INBOX`, `TODAY`, `DONE`), dueDate (opcjonalnie), doneDate
* `Note`: id, date, type (`DAILY`, `WEEKLY_REVIEW`, `FREE`), text, tags, habitId (opcjonalnie)
* `CheckIn`: date, mood (1–5), energy (1–5)
* `Counter`: id, name, startDate (licznik „dni od" lub „dni bez")
* `Pause`: from, to, reason (choroba, wyjazd)
* `WheelRating`: date, areaId, score (1–10), koło życia

Identyfikatory to UUID jako tekst. Nawyków, celów i obszarów nie usuwamy, tylko archiwizujemy (`archived`), żeby historia zostawała spójna.

Szkielet pliku:

```json
{
  "schemaVersion": 1,
  "settings": { "dayStartHour": 4, "dataDir": null },
  "areas": [], "goals": [], "habits": [], "entries": [],
  "tasks": [], "notes": [], "checkIns": [], "counters": [],
  "pauses": [], "wheelRatings": []
}
```

## Zapis danych

* Domyślna lokalizacja: `~/.moj-rytm/data.json`. Folder danych ma być konfigurowalny w ustawieniach (żebym mógł go wskazać w Dropboksie lub OneDrive).
* Całość trzymana w pamięci. Zapis po każdej zmianie z opóźnieniem około 500 ms (kilka zmian pod rząd to jeden zapis). Przy zamykaniu aplikacji zapis wymuszony.
* Zapis atomowy: najpierw `data.json.tmp`, potem `Files.move` z `ATOMIC_MOVE` i `REPLACE_EXISTING`. Nigdy nie piszemy bezpośrednio do `data.json`.
* Kopia zapasowa raz dziennie do `backups/data-RRRR-MM-DD.json`, trzymamy ostatnie 14.
* Jeśli `data.json` jest uszkodzony: nie nadpisuj go. Zmień nazwę na `data.corrupt-<znacznik czasu>.json`, pokaż komunikat i zaproponuj przywrócenie z najnowszej kopii.
* `schemaVersion` jest w pliku od pierwszej wersji. Każda zmiana modelu oznacza podniesienie wersji, dopisanie migracji w `Migrator` (łańcuch migracji wersja po wersji) i test migracji na przykładowym pliku. Jeśli plik ma nowszą wersję niż aplikacja, odmów otwarcia z czytelnym komunikatem i nic nie zmieniaj.
* Jackson: `JavaTimeModule`, `WRITE_DATES_AS_TIMESTAMPS` wyłączone (daty jako `2026-10-24`), czytelne wcięcia i stabilna kolejność pól (plik ma dać się przeczytać i porównać w diffie), `FAIL_ON_UNKNOWN_PROPERTIES` wyłączone.

## Zasady logiki

To najbardziej błędogenna część aplikacji. Każda reguła ma mieć testy jednostkowe.

* Dzień logiczny: dzień kończy się o `dayStartHour` (domyślnie 4:00), nie o północy. Wpis z godziny 1:30 liczy się do poprzedniego dnia. Cała logika operuje na dniu logicznym, nie na dacie z zegara.
* Tydzień: poniedziałek–niedziela (ISO, `WeekFields.ISO`).
* Nawyk CHECK dzienny: dzień zaliczony, gdy istnieje wpis na ten dzień.
* Nawyk COUNT: suma wartości z dnia ≥ `targetAmount` oznacza zaliczenie.
* Nawyk tygodniowy (N razy): tydzień zaliczony, gdy liczba różnych dni z wpisem ≥ N.
* Wersja minimalna: wpis z `minimal = true` liczy się jako zaliczony, ale jest oznaczony w widokach i statystykach.
* Seria dzienna: kolejne zaliczone dni liczone wstecz od dziś. Jeśli dziś jeszcze nie zrobiono, seria nie jest przerwana i liczymy od wczoraj.
* Seria tygodniowa: kolejne zaliczone tygodnie. Bieżący, jeszcze niezaliczony tydzień nie przerywa serii.
* Pauza: dni objęte pauzą nie przerywają serii i nie wchodzą do mianownika procentów. Tydzień w całości w pauzie jest pomijany.
* Wykonanie tygodnia (%): zaliczone / oczekiwane. Dla nawyków dziennych oczekiwane są dni od założenia nawyku (`created`) do dziś w danym tygodniu, dla tygodniowych wartość N. Dni przed założeniem nie liczą się nigdzie.
* Przyszłość: nie można zaznaczyć dnia z przyszłości. Wsteczne uzupełnianie przeszłych dni jest dozwolone.
* Zadania: w statusie `TODAY` mogą być najwyżej 3. Próba dodania czwartego pokazuje komunikat, nie zgadzamy się po cichu.

## Dashboard (docelowo)

Wykonanie tygodnia i seria przy każdym nawyku, mapa cieplna z ostatnich 12 tygodni (siatka kwadratów), najczęściej pomijany nawyk, najgorszy dzień tygodnia, trend nastroju i energii oraz zestawienie nawyków z nastrojem (czy dni z wykonanym nawykiem są lepsze). Koło życia pokazywane jako zmiana ocen w czasie. Wszystko liczone w `service`.

## Etapy

Budujemy po kolei. Każdy etap ma być używalny samodzielnie, a przed następnym go testuję na sobie.

1. **Nawyki:** model, zapis JSON z kopiami i migracjami, nawyki CHECK i COUNT, dzienne i tygodniowe, widok „Dziś" i „Tydzień", serie, pauzy.
2. **Check-in i dashboard:** nastrój i energia, wykresy, mapa cieplna, statystyki.
3. **Zadania:** skrzynka, widok „Dziś" z limitem trzech priorytetów.
4. **Notatki, liczniki, cele:** wpis dzienny, przegląd tygodnia, licznik „dni od", hierarchia Obszar → Cel → Nawyk, koło życia.
5. **Szlify:** ustawienia, motyw jasny i ciemny, skróty klawiszowe, instalator `jpackage`.

Poza zakresem na razie: synchronizacja między urządzeniami, konta, przypomnienia systemowe, punkty i poziomy, funkcje AI.

## Jak pracować ze mną

* Przed większą zmianą (nowy moduł, zmiana modelu danych, nowa zależność) przedstaw krótki plan i poczekaj na akceptację.
* Małe kroki: jedna funkcja na raz, `mvn clean verify` zielone po każdym kroku.
* Nie przepisuj działającego kodu bez pytania i nie rób refaktoryzacji „przy okazji".
* Przy zmianie modelu danych zawsze: podbij `schemaVersion`, dopisz migrację, dopisz test.
* Przy niejasności zapytaj, zamiast zgadywać.
* Gdy zmieni się architektura lub zasady, zaktualizuj ten plik.
