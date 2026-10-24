package pl.mojrytm.ui;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.concurrent.Task;

/** Praca poza wątkiem JavaFX (I/O). Wyniki wracają na wątek JavaFX przez {@link Task}. */
final class Background {

    private Background() {
    }

    /** Wywołać z wątku JavaFX; {@code onSuccess} i {@code onFailure} też wykonają się na nim. */
    static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> onFailure.accept(task.getException()));
        Thread thread = new Thread(task, "moj-rytm-tlo");
        thread.setDaemon(true);
        thread.start();
    }
}
