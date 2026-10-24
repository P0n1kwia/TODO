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

Testy widoków (`*ScreenTest`, `MainViewTest`) używają prawdziwego toolkitu JavaFX, więc potrzebują ekranu. Bez niego są pomijane, a nie oblewane (zwykłe `mvn clean verify` przechodzi, ale ich nie uruchamia). Na Windowsie uruchamiają się zawsze. Na serwerze bez ekranu przed commitem uruchom też: `xvfb-run -a mvn clean verify`.

Folder danych można podmienić właściwością systemową `-Dmojrytm.dataDir=<folder>` (próby bez ruszania `~/.moj-rytm`). Domyślnie `~/.moj-rytm`.

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

### Jak jest zrobione UI (krok 3)

* `StartupFlow` prowadzi start (wczytanie w `Task`, ekrany dla uszkodzonego pliku z propozycją przywrócenia kopii i dla pliku z nowszej wersji) i koniec (zamknięcie okna wymusza zapis w tle, przy błędzie można ponowić). `AppModel` trzyma dane, dzisiejszy dzień logiczny i komunikat jako właściwości, które widoki obserwują; `AppModel.update(...)` zamienia odrzucenie reguły (`EntryRejectedException`) na komunikat w pasku stanu.
* Ekrany to `TodayScreen`, `WeekScreen`, `HabitsScreen`, `GoalsScreen`, `CountersScreen`, `JournalScreen` (złożony z paneli: wpis dzienny, przegląd tygodnia, wolne notatki, lista), `WheelScreen`, `PausesScreen`. Każdy jest `VBox` budowanym od nowa po zmianie danych i dodawanym w `Screens.create(...)`, który owija go w `ScrollPane`. Okna: `HabitDialog`, `AmountDialog`, `NameDialog` (nazwa obszaru lub celu). Style w `src/main/resources/pl/mojrytm/ui/app.css` (motyw jasny; stan niesie też symbol, nie tylko kolor).
* Logikę wejścia (walidację formularzy, formaty, odmianę, parsowanie) trzymamy w czystych klasach bez JavaFX (`HabitForm`, `PauseForm`, `NameForm`, `CounterForm`, `DailyNoteForm`, `NoteState`, `GoalChoice`, `HabitChoice`, `AmountChoice`, `Formats`, `PolishPlural`, `DateConverter`), żeby dało się je testować zwykłymi testami. Zestawienia dla ekranów dają `service.HabitBoard` (wyniki nawyków) i `service.GoalBoard` (hierarchia Obszar → Cel → Nawyk).
* Każdy tekst widoczny w UI jest w `messages_pl.properties`. Test sprawdza, że klucze użyte w kodzie istnieją, a dla rodzin kluczy składanych z członów (`error.entry.<powód>`, `habit.form.problem.<problem>`, `pauses.form.problem.<problem>`, `name.form.problem.<problem>`, `counters.form.problem.<problem>`, `journal.daily.problem.<problem>`, `journal.list.type.<rodzaj>`) że każdy powód ma komunikat.

## Model danych

Zapisujemy tylko fakty. Serie, procenty i inne wartości wyliczane liczy kod, nigdy nie trafiają do pliku. Dzięki temu zmiana zasad liczenia nie psuje starych danych.

Hierarchia: Obszar → Cel → Nawyk.

* `Area`: id, name (np. Sen, Ciało, Pieniądze, Relacje), archived
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
  "schemaVersion": 2,
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
* Kolejność pól w pliku wynika z kolejności składowych rekordu. Format pilnuje plik wzorcowy `src/test/resources/pl/mojrytm/storage/sample-v2.json` (test zapisuje przykładowe dane i porównuje bajt po bajcie). Przy zmianie modelu zaktualizuj go świadomie, a poprzedni plik wzorcowy (`sample-v1.json`) zostaw jako wejście testu migracji.
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

Doprecyzowania z kroku 2 (`service`), do zakwestionowania, jeśli coś jest nie tak:

* Stan dnia to `NOT_DONE`, `DONE_MINIMAL` albo `DONE`. COUNT z sumą ≥ celu jest `DONE`, nawet jeśli któryś wpis ma `minimal = true`. Sumy porównujemy z tolerancją 1e-9.
* Nawyk dzienny: dzień w pauzie jest pomijany całkowicie. Wpis z takiego dnia nie wydłuża serii i nie wchodzi do licznika procentu (widok nadal go pokazuje).
* Nawyk tygodniowy: wpisy w dniach pauzy liczą się do N (to „ile razy w tygodniu"), a oczekiwane = `min(N, dni dostępne)`. Tydzień w całości w pauzie jest pomijany nawet z wpisami. Procent nie przekracza 100.
* Bieżący tydzień nawyku dziennego: oczekiwane to dni do dziś włącznie. Dla tygodniowego dni przed nami są jeszcze dostępne.
* Seria dzienna kończy się na dniu założenia nawyku. Seria tygodniowa kończy się na tygodniu, który skończył się przed założeniem.
* Edycja wpisów (`EntryEditor`): jeden wpis na nawyk na dzień, dni w pauzie można zaznaczać, nawyk zarchiwizowany jest tylko do odczytu, dzień „z przyszłości" liczy się według dnia logicznego. Sumy COUNT są zaokrąglane do 6 miejsc po przecinku, żeby `0.1 + 0.2` nie trafiło do pliku jako `0.30000000000000004`.

Doprecyzowania z etapu 4 (obszary i cele), do zakwestionowania, jeśli coś jest nie tak:

* Schemat 2: `Area` ma `archived`. Migracja v1→v2 ustawia istniejącym obszarom `archived: false`.
* Archiwizacja obszaru albo celu niczego nie kaskaduje: cele, nawyki i ich historia zostają, można je dalej odhaczać. Zarchiwizowany obszar i cel znikają z list wyboru (okno nawyku, koło życia), ale cel, do którego nawyk jest już przypisany, zostaje na liście przy edycji (z dopiskiem "zarchiwizowany"), żeby edycja go nie gubiła.
* Nazwy obszarów są unikalne w całych danych, nazwy celów w obszarze, bez względu na wielkość liter i spacje na brzegach, także wśród zarchiwizowanych (zamiast duplikatu przywracamy stary wpis). Kontroluje to `AreaEditor`/`GoalEditor` (UI pyta przez `nameTaken`).
* Cel zostaje w swoim obszarze (przenoszenia nie ma), nazwę można zmienić. Nawyk przenosi się między celami w swoim oknie edycji; `HabitEditor` odrzuca cel, którego nie ma. Nawyk wskazujący nieistniejący cel (np. po ręcznej edycji pliku) traktujemy jak nawyk bez celu.
* Ekran „Nawyki” grupuje aktywne nawyki według celu (nagłówki "Obszar › Cel"), a gdy żaden nawyk nie ma celu, pokazuje jedną listę jak wcześniej. Zarchiwizowane nawyki są jedną listą z celem w opisie. Ekran „Cele” zarządza obszarami i celami oraz pokazuje liczbę aktywnych nawyków celu.
* Przykładowe obszary (Sen, Ciało, Pieniądze, Relacje; nazwy w `messages_pl.properties`) dodaje przycisk na pustym ekranie „Cele”; nic nie jest tworzone samo.

* Liczniki: licznik to nazwa i dzień startu; wynik to liczba pełnych dni od startu do dziś logicznego (w dniu startu 0, nazajutrz 1; start w przyszłości daje 0), liczy `service.CounterBoard`. Start w formularzu nie może być w przyszłości. „Zaczynam od nowa” ustawia start na dziś (nie działa w dniu startu) i nie zapamiętuje poprzedniego wyniku, więc pyta o potwierdzenie; usunięcie licznika też (liczniki nie mają historii, więc usuwamy, a nie archiwizujemy).

* Dziennik: wpis dzienny jest jeden na dzień (można pisać wstecz, nie w przyszłość), przegląd tygodnia jeden na tydzień (data = poniedziałek; bieżący tydzień dozwolony, przyszły nie), a wolnych notatek jest dowolnie wiele (dzisiejsza data, można je tylko usunąć, z potwierdzeniem). Zapis zastępuje poprzednią treść (id zostaje), pusty tekst usuwa wpis lub przegląd. Tagi wpisuje się po przecinku; zapisują się małymi literami, bez „#” i bez powtórzeń (`Tags`). Notatka może wskazywać nawyk (zarchiwizowany tylko wtedy, gdy już do niego należy). Lista pokazuje 50 najnowszych wpisów, z filtrem tagu; starsze otwiera się przez wybór dnia lub tygodnia.
* Przegląd tygodnia pokazuje obok pola tekstu wynik tygodnia z `HabitBoard.weekSummary`: wiersze nawyków (wykonanie i seria dziś), „najlepszy” (najwyższy procent, przy remisie pierwszy w danych) i „do poprawy” (najniższy, tylko gdy jest słabszy od najlepszego). Nawyki, od których nic nie oczekujemy (pauza), nie biorą udziału w porównaniu.
* Panele dziennika nie są rysowane od nowa po zmianie danych (żeby nie gubić pisanego tekstu). Niezapisany wpis dzienny i przegląd zapisują się przy zmianie dnia lub tygodnia, przy przejściu do innej zakładki i przy zamknięciu okna (`AppModel.addCloseHook`, uruchamiany w `StartupFlow` przed wymuszonym zapisem); napis „Niezapisane zmiany” pokazuje stan. Awaria programu zgubi niezapisany tekst.

* Koło życia: ocena 1–10 dla każdego aktywnego obszaru, jedna na obszar na dzień. Zapis tego samego dnia nadpisuje ocenę (zostaje na swoim miejscu), starsze zostają. Oceniać można tylko dziś i tylko aktywne obszary. Oceny zarchiwizowanego obszaru zostają w pliku, ale nie trafiają do widoku ani na wykres. Suwak startuje od ostatniej oceny (albo 5), obok widać „ostatnio: N z daty · zmiana ±M” (zmiana względem poprzedniej oceny z wcześniejszego dnia). JavaFX nie ma wykresu radarowego, więc zmianę pokazuje `LineChart` z linią na obszar; oś X to dni, w których oceniano (równe odstępy, nie proporcjonalne do czasu). Dwie oceny tego samego obszaru z tego samego dnia w pliku (np. po ręcznej edycji) liczą się jako jedna, późniejsza w danych. Liczy to `WheelBoard`, zapisuje `WheelEditor`.

Jak jest zrobiona warstwa `service` (krok 2): `LogicalClock` jest jedynym miejscem zamieniającym `Clock` na datę (dzień logiczny z `settings.dayStartHour`), `Weeks` i `Pauses` to kalendarz, `HabitTracker` liczy dni, tydzień i serię jednego nawyku (dzisiejszy dzień podaje wołający), a `EntryEditor` zmienia wpisy i zgłasza `EntryRejectedException` z kodem powodu, który UI mapuje na komunikat z `messages_pl.properties`. Etap 4: `AreaEditor`, `GoalEditor`, `CounterEditor`, `NoteEditor`, `WheelEditor` i `HabitEditor` zmieniają listy (błąd programisty to `IllegalArgumentException`, UI sprawdza wcześniej), a `GoalBoard`, `CounterBoard`, `NoteBoard` i `WheelBoard` składają zestawienia dla widoków.

## Dashboard (docelowo)

Wykonanie tygodnia i seria przy każdym nawyku, mapa cieplna z ostatnich 12 tygodni (siatka kwadratów), najczęściej pomijany nawyk, najgorszy dzień tygodnia, trend nastroju i energii oraz zestawienie nawyków z nastrojem (czy dni z wykonanym nawykiem są lepsze). Koło życia pokazywane jako zmiana ocen w czasie. Wszystko liczone w `service`.

## Etapy

Budujemy po kolei. Każdy etap ma być używalny samodzielnie, a przed następnym go testuję na sobie.

1. **Nawyki:** model, zapis JSON z kopiami i migracjami, nawyki CHECK i COUNT, dzienne i tygodniowe, widok „Dziś" i „Tydzień", serie, pauzy.
2. **Check-in i dashboard:** nastrój i energia, wykresy, mapa cieplna, statystyki.
3. **Zadania:** skrzynka, widok „Dziś" z limitem trzech priorytetów.
4. **Notatki, liczniki, cele:** wpis dzienny, przegląd tygodnia, licznik „dni od", hierarchia Obszar → Cel → Nawyk, koło życia.
5. **Szlify:** ustawienia, motyw jasny i ciemny, skróty klawiszowe, instalator `jpackage`.

Stan: etap 1 jest zaimplementowany (kroki 0–3: szkielet, model i storage, service, UI) i czeka na testy użytkownika. Etap 4 jest w toku (etapy 2 i 3 odłożone na prośbę użytkownika): zrobione 4a (schemat v2), 4b (obszary i cele), 4c (liczniki) i 4d (dziennik: wpis dzienny, przegląd tygodnia, wolne notatki) i 4e (koło życia). Etap 4 czeka na testy użytkownika.

Poza zakresem na razie: synchronizacja między urządzeniami, konta, przypomnienia systemowe, punkty i poziomy, funkcje AI.

## Jak pracować ze mną

* Przed większą zmianą (nowy moduł, zmiana modelu danych, nowa zależność) przedstaw krótki plan i poczekaj na akceptację.
* Małe kroki: jedna funkcja na raz, `mvn clean verify` zielone po każdym kroku.
* Nie przepisuj działającego kodu bez pytania i nie rób refaktoryzacji „przy okazji".
* Przy zmianie modelu danych zawsze: podbij `schemaVersion`, dopisz migrację, dopisz test.
* Przy niejasności zapytaj, zamiast zgadywać.
* Gdy zmieni się architektura lub zasady, zaktualizuj ten plik.
