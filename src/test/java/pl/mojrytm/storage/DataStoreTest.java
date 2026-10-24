package pl.mojrytm.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Settings;

class DataStoreTest {

    private static final Duration NEVER = Duration.ofHours(1);
    private static final Duration SOON = Duration.ofMillis(20);

    private final List<AppData> written = new CopyOnWriteArrayList<>();
    private final AtomicInteger failuresToInject = new AtomicInteger();
    private DataStore store;

    private DataStore storeWith(Duration delay, DataStore.Writer writer, java.util.function.Consumer<IOException> onError) {
        store = new DataStore(AppData.empty(), writer, Executors.newSingleThreadScheduledExecutor(), delay, onError);
        return store;
    }

    private DataStore storeWith(Duration delay) {
        return storeWith(delay, this::record, e -> { });
    }

    private void record(AppData data) throws IOException {
        if (failuresToInject.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
            throw new IOException("zapis się nie udał");
        }
        written.add(data);
    }

    private static AppData withDayStart(int hour) {
        return new AppData(AppData.CURRENT_SCHEMA_VERSION, new Settings(hour, null), null, null, null, null, null, null, null, null, null, null);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (store != null) {
            store.close();
        }
    }

    @Test
    void getReturnsInitialDataAndThenTheLatestUpdate() {
        storeWith(NEVER);
        assertEquals(AppData.empty(), store.get());

        store.update(d -> withDayStart(6));

        assertEquals(withDayStart(6), store.get());
    }

    @Test
    void updateDoesNotWriteBeforeTheDelayPasses() {
        storeWith(NEVER).update(d -> withDayStart(6));

        assertEquals(List.of(), written);
    }

    @Test
    void severalUpdatesInARowBecomeOneWriteOfTheLatestData() throws IOException {
        storeWith(NEVER);
        for (int hour = 1; hour <= 5; hour++) {
            int h = hour;
            store.update(d -> withDayStart(h));
        }

        store.flush();

        assertEquals(List.of(withDayStart(5)), written);
    }

    @Test
    void flushWithoutChangesWritesNothing() throws IOException {
        storeWith(NEVER);
        store.flush();
        store.update(d -> withDayStart(6));
        store.flush();
        store.flush();

        assertEquals(List.of(withDayStart(6)), written);
    }

    @Test
    void writesInTheBackgroundAfterTheDelay() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<String> writerThread = new AtomicReference<>();
        storeWith(SOON, data -> {
            writerThread.set(Thread.currentThread().getName());
            written.add(data);
            done.countDown();
        }, e -> { });

        store.update(d -> withDayStart(6));

        assertTrue(done.await(5, TimeUnit.SECONDS), "zapis w tle nie nastąpił");
        assertEquals(List.of(withDayStart(6)), written);
        assertFalse(writerThread.get().equals(Thread.currentThread().getName()));
    }

    @Test
    void failedFlushKeepsTheChangeAndTheNextFlushRetries() throws IOException {
        storeWith(NEVER);
        store.update(d -> withDayStart(6));
        failuresToInject.set(1);

        assertThrows(IOException.class, () -> store.flush());
        assertEquals(List.of(), written);
        store.flush();

        assertEquals(List.of(withDayStart(6)), written);
    }

    @Test
    void failedBackgroundWriteIsReportedAndRetriedOnTheNextUpdate() throws Exception {
        CountDownLatch reported = new CountDownLatch(1);
        CountDownLatch saved = new CountDownLatch(1);
        AtomicReference<IOException> error = new AtomicReference<>();
        storeWith(SOON, data -> {
            if (failuresToInject.getAndDecrement() > 0) {
                throw new IOException("brak miejsca na dysku");
            }
            written.add(data);
            saved.countDown();
        }, e -> {
            error.set(e);
            reported.countDown();
        });
        failuresToInject.set(1);

        store.update(d -> withDayStart(6));
        assertTrue(reported.await(5, TimeUnit.SECONDS), "błąd nie został zgłoszony");
        assertEquals("brak miejsca na dysku", error.get().getMessage());
        assertEquals(List.of(), written);

        store.update(d -> withDayStart(7));
        assertTrue(saved.await(5, TimeUnit.SECONDS), "ponowny zapis nie nastąpił");
        assertEquals(List.of(withDayStart(7)), written);
    }

    @Test
    void unexpectedRuntimeErrorInTheWriterIsReportedToo() throws Exception {
        CountDownLatch reported = new CountDownLatch(1);
        AtomicReference<IOException> error = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();
        storeWith(SOON, data -> {
            if (calls.getAndIncrement() == 0) {
                throw new IllegalStateException("bug");
            }
            written.add(data);
        }, e -> {
            error.set(e);
            reported.countDown();
        });

        store.update(d -> withDayStart(6));

        assertTrue(reported.await(5, TimeUnit.SECONDS));
        assertInstanceOf(IllegalStateException.class, error.get().getCause());
        store.flush(); // zmiana nie przepadła, zapis się powtarza
        assertEquals(List.of(withDayStart(6)), written);
    }

    @Test
    void onlyTheLastScheduledSaveStaysActiveWhenUpdatesComeInARow() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        store = new DataStore(AppData.empty(), this::record, executor, NEVER, e -> { });

        for (int hour = 1; hour <= 5; hour++) {
            int h = hour;
            store.update(d -> withDayStart(h));
        }

        long active = executor.getQueue().stream().filter(task -> !((Future<?>) task).isCancelled()).count();
        assertEquals(1, active);
    }

    @Test
    void closeFlushesPendingChangesAndLaterUpdatesAreRejected() throws IOException {
        storeWith(NEVER).update(d -> withDayStart(6));

        store.close();

        assertEquals(List.of(withDayStart(6)), written);
        assertThrows(IllegalStateException.class, () -> store.update(d -> withDayStart(7)));
        store.close(); // drugie zamknięcie jest bezpieczne
        assertEquals(1, written.size());
    }

    @Test
    void failingUpdateOperationLeavesDataAndStateUntouched() throws IOException {
        storeWith(NEVER);

        assertThrows(IllegalArgumentException.class, () -> store.update(d -> {
            throw new IllegalArgumentException("zła zmiana");
        }));
        assertThrows(NullPointerException.class, () -> store.update(d -> null));
        store.flush();

        assertEquals(AppData.empty(), store.get());
        assertEquals(List.of(), written);
    }

    @Test
    void updateIsNotBlockedByASlowWrite() throws Exception {
        CountDownLatch writeStarted = new CountDownLatch(1);
        CountDownLatch releaseWrite = new CountDownLatch(1);
        storeWith(NEVER, data -> {
            writeStarted.countDown();
            try {
                releaseWrite.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
            written.add(data);
        }, e -> { });
        store.update(d -> withDayStart(1));
        Thread slowFlush = new Thread(() -> {
            try {
                store.flush();
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        });
        slowFlush.start();
        assertTrue(writeStarted.await(5, TimeUnit.SECONDS));

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> store.update(d -> withDayStart(2)));
        assertEquals(withDayStart(2), store.get());

        releaseWrite.countDown();
        slowFlush.join(5000);
        store.flush();
        assertEquals(List.of(withDayStart(1), withDayStart(2)), written);
    }

    @Test
    void worksTogetherWithJsonStorage(@TempDir Path dir) throws Exception {
        JsonStorage storage = new JsonStorage(dir, Clock.systemDefaultZone());
        CountDownLatch errors = new CountDownLatch(1);
        store = DataStore.open(AppData.empty(), storage, e -> errors.countDown());

        store.update(d -> withDayStart(6));

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!storage.dataFile().toFile().exists() && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertEquals(new LoadResult.Loaded(withDayStart(6)), storage.load());
        assertEquals(1, errors.getCount(), "nie powinno być błędów zapisu");
    }
}
