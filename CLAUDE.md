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
mvn javafx:run          # uruchomienie aplikacji (javafx-maven-plugin)
```

W sesji bez ekranu (chmura, Linux) aplikację uruchamia się pod `xvfb-run -a mvn javafx:run`.

### Pakowanie (`jpackage`)

Docelowy system to Windows. Najpierw wersja bez instalatora (`app-image`: folder z `MojRytm.exe`, nie wymaga dodatkowych narzędzi):

```
mvn clean verify
mvn dependency:copy-dependencies -DoutputDirectory=target/input -DincludeScope=runtime
copy target\moj-rytm-0.1.0-SNAPSHOT.jar target\input\
jpackage --type app-image --name MojRytm --app-version 0.1.0 --input target/input --main-jar moj-rytm-0.1.0-SNAPSHOT.jar --main-class pl.mojrytm.Launcher --dest target/dist
```

Instalator `.exe`: ta sama komenda z `--type exe --win-menu --win-shortcut` zamiast `--type app-image` (wymaga zainstalowanego WiX Toolset). `jpackage` nie robi cross-kompilacji, więc instalator Windows buduje się na Windowsie. Przy zmianie wersji projektu zaktualizuj nazwę jara i `--app-version`.

Stan sprawdzenia: `verify`, `test`, `javafx:run` i `app-image` zostały sprawdzone na Linuksie (zamiast `copy` było `cp`); aplikacja z `app-image` startuje i pokazuje okno. Wariant Windows (`.exe`, WiX) nie był jeszcze uruchamiany. Przy starcie z classpath JavaFX wypisuje ostrzeżenie „Unsupported JavaFX configuration" (bez `module-info`); nie wpływa na działanie.

## Architektura

Cztery warstwy, zależności tylko w dół:

```
pl.mojrytm
├── model      czyste klasy danych, bez JavaFX i bez I/O
├── storage    odczyt i zapis JSON, kopie zapasowe, migracje
├── service    logika: serie, statystyki, tygodnie, dzień logiczny (bez UI)
└── ui         widoki JavaFX, bez liczenia czegokolwiek
```

Punkt wejścia: `pl.mojrytm.Launcher` (osobna klasa, bo JavaFX nie startuje klasy rozszerzającej `Application` bezpośrednio z classpath). Bez `module-info.java`, projekt działa na classpath.

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

Jak to jest zrobione (krok 1):

* Model to niezmienne rekordy z walidacją w konstruktorach. `AppData` jest niezmienną migawką całego pliku. Zmiana danych = nowa migawka przez `DataStore.update(...)`. Nieprawidłowa wartość w pliku (np. nastrój 9) traktowana jest jak uszkodzony plik.
* Kolejność pól w pliku wynika z kolejności składowych rekordu. Format pilnuje plik wzorcowy `src/test/resources/pl/mojrytm/storage/sample-v1.json` (test zapisuje przykładowe dane i porównuje bajt po bajcie). Przy zmianie modelu zaktualizuj go świadomie.
* `JsonStorage.load()` zwraca `LoadResult`: `Loaded` (brak pliku = puste dane), `Corrupt` (plik przeniesiony na bok, z najnowszą kopią do zaproponowania) albo `NewerVersion` (nic nie zmieniono). `restoreFromBackup(...)` przywraca kopię. Błąd odczytu (np. brak uprawnień) to zwykły `IOException`, nie uszkodzenie.
* Kopia zapasowa powstaje przy pierwszym zapisie danego dnia i jest kopią dotychczasowego pliku, czyli stanem sprzed pierwszej zmiany tego dnia. Data w nazwie to data kalendarzowa z `Clock`. Kopii nie tworzymy z pliku, którego nie da się wczytać. Niepowodzenie kopii tylko loguje ostrzeżenie i nie blokuje zapisu.
* `DataStore` zapisuje w tle na własnym wątku (opóźnienie 500 ms, kolejne zmiany przesuwają termin). Wątek JavaFX nigdy nie czeka na dysk przy `update`. Nieudany zapis zgłasza callback, zmiany zostają w pamięci i zapis powtarza się przy kolejnej zmianie, `flush()` lub `close()`. Wczytanie przy starcie UI opakowuje w `Task`, wynik przekazuje przez `Platform.runLater`.
* Wartości ilościowe (`targetAmount`, `Entry.value`) to `double`. Porównania sum z celem robi `service` z niewielką tolerancją.

Do rozstrzygnięcia w etapie 5 (ustawienia): `settings.dataDir` leży w samym pliku danych, więc aplikacja nie wie, gdzie szukać pliku, jeśli folder został zmieniony. Potrzebny jest stały wskaźnik poza folderem danych (np. mały plik w `~/.moj-rytm/`).

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

Doprecyzowania uzgodnione przy starcie etapu 1:

* `Pause` jest globalna (dotyczy wszystkich nawyków), bo nie ma `habitId`.
* CHECK: jeden wpis na nawyk na dzień. Odznaczenie usuwa wpis.
* Tygodniowy nawyk COUNT: dzień „z wpisem" to dzień, w którym suma ≥ `targetAmount` (lub wpis z `minimal = true`).
* COUNT z `minimal = true`: dzień jest zaliczony niezależnie od `value`.
* Wpis na dzień przed `created` jest blokowany z komunikatem.
* Nawyk tygodniowy w tygodniu z częściową pauzą albo założony w środku tygodnia: oczekiwane = `min(N, dni dostępne w tym tygodniu)`. Dostępne to dni od `created`, poza pauzą, do końca tygodnia.

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
