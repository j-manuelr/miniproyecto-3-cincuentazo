package np.cincuentazo.thread;

import javafx.application.Platform;

/**
 * Background thread that counts down from a given number of seconds and fires
 * two callbacks: a per-second tick (for the UI countdown display) and a final
 * timeout callback when the timer expires.
 *
 * <p>Both callbacks are dispatched to the JavaFX Application Thread.
 * Call {@link #cancel()} from any thread to stop the timer before it expires.
 */
public class TurnTimerThread extends Thread {

    /**
     * Callback invoked once per second on the JavaFX Application Thread.
     */
    public interface TickCallback {
        /**
         * Called each second during the countdown.
         *
         * @param secondsRemaining seconds left until timeout (≥ 0)
         */
        void onTick(int secondsRemaining);
    }

    private final int totalSeconds;
    private final TickCallback onTick;
    private final Runnable onTimeout;
    private volatile boolean cancelled = false;

    /**
     * Constructs a countdown timer thread.
     *
     * @param totalSeconds total duration of the countdown in seconds
     * @param onTick       callback invoked each second with the remaining time
     * @param onTimeout    callback invoked when the countdown reaches zero
     */
    public TurnTimerThread(int totalSeconds, TickCallback onTick, Runnable onTimeout) {
        this.totalSeconds = totalSeconds;
        this.onTick       = onTick;
        this.onTimeout    = onTimeout;
        setDaemon(true);
        setName("TurnTimerThread");
    }

    /**
     * Counts down one second at a time, dispatching {@code onTick} to the JavaFX
     * thread each second. Dispatches {@code onTimeout} when zero is reached,
     * unless the timer has been cancelled.
     */
    @Override
    public void run() {
        try {
            for (int remaining = totalSeconds; remaining >= 0 && !cancelled; remaining--) {
                final int r = remaining;
                Platform.runLater(() -> onTick.onTick(r));
                if (remaining > 0) Thread.sleep(1000);
            }
            if (!cancelled) {
                Platform.runLater(onTimeout);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Cancels this timer. The {@code onTimeout} callback will NOT be invoked.
     * Safe to call from any thread.
     */
    public void cancel() {
        cancelled = true;
        interrupt();
    }

    /**
     * Returns whether this timer has been cancelled.
     *
     * @return {@code true} if cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }
}
