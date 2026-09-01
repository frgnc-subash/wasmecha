package laundry.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

/**
 * Holds the shared, synchronized resources of the laundry facility
 * (washers, dryers, payment kiosks) and the running statistics.
 *
 * A single Spring-managed instance is shared by every Customer thread
 * and by the GUI, which subscribes as a log listener to display events.
 */
@Component
public class LaundryFacility {

    public static final int NUM_WASHERS = 6;
    public static final int NUM_DRYERS = 4;
    public static final int NUM_KIOSKS = 2;

    // Counting semaphores model the fixed pool of each resource.
    // 'true' = fair mode, so waiting customers are served in arrival order.
    private final Semaphore washerSemaphore = new Semaphore(NUM_WASHERS, true);
    private final Semaphore dryerSemaphore = new Semaphore(NUM_DRYERS, true);
    private final Semaphore kioskSemaphore = new Semaphore(NUM_KIOSKS, true);

    // Track how many of each resource are currently in use, and the peak,
    // using atomics so updates from many customer threads stay consistent.
    private final AtomicInteger washersInUse = new AtomicInteger(0);
    private final AtomicInteger dryersInUse = new AtomicInteger(0);
    private final AtomicInteger kiosksInUse = new AtomicInteger(0);
    private final AtomicInteger maxWashersInUse = new AtomicInteger(0);
    private final AtomicInteger maxDryersInUse = new AtomicInteger(0);
    private final AtomicInteger maxKiosksInUse = new AtomicInteger(0);

    private final AtomicInteger customersServed = new AtomicInteger(0);
    private final AtomicLong totalServiceTimeMillis = new AtomicLong(0);

    // Listeners (e.g. the GUI's log panel) notified of every log message.
    private final List<Consumer<String>> logListeners = new CopyOnWriteArrayList<>();

    public void addLogListener(Consumer<String> listener) {
        logListeners.add(listener);
    }

    public void log(String message) {
        System.out.println(message);
        for (Consumer<String> listener : logListeners) {
            listener.accept(message);
        }
    }

    public int getNumWashers() {
        return NUM_WASHERS;
    }

    public int getNumDryers() {
        return NUM_DRYERS;
    }

    public int getNumKiosks() {
        return NUM_KIOSKS;
    }

    public int getWashersInUse() {
        return washersInUse.get();
    }

    public int getDryersInUse() {
        return dryersInUse.get();
    }

    public int getKiosksInUse() {
        return kiosksInUse.get();
    }

    public int getMaxWashersInUse() {
        return maxWashersInUse.get();
    }

    public int getMaxDryersInUse() {
        return maxDryersInUse.get();
    }

    public int getMaxKiosksInUse() {
        return maxKiosksInUse.get();
    }

    public int getCustomersServed() {
        return customersServed.get();
    }

    public double getAverageServiceTimeMillis() {
        int served = customersServed.get();
        return served == 0 ? 0.0 : (double) totalServiceTimeMillis.get() / served;
    }

    /** Clears all counters/stats so the facility can be reused for a fresh run. */
    public void reset() {
        washersInUse.set(0);
        dryersInUse.set(0);
        kiosksInUse.set(0);
        maxWashersInUse.set(0);
        maxDryersInUse.set(0);
        maxKiosksInUse.set(0);
        customersServed.set(0);
        totalServiceTimeMillis.set(0);
    }

    public void acquireWasher() throws InterruptedException {
        washerSemaphore.acquire();
        int current = washersInUse.incrementAndGet();
        maxWashersInUse.accumulateAndGet(current, Math::max);
    }

    public void releaseWasher() {
        washersInUse.decrementAndGet();
        washerSemaphore.release();
    }

    public void acquireDryer() throws InterruptedException {
        dryerSemaphore.acquire();
        int current = dryersInUse.incrementAndGet();
        maxDryersInUse.accumulateAndGet(current, Math::max);
    }

    public void releaseDryer() {
        dryersInUse.decrementAndGet();
        dryerSemaphore.release();
    }

    public void acquireKiosk() throws InterruptedException {
        kioskSemaphore.acquire();
        int current = kiosksInUse.incrementAndGet();
        maxKiosksInUse.accumulateAndGet(current, Math::max);
    }

    public void releaseKiosk() {
        kiosksInUse.decrementAndGet();
        kioskSemaphore.release();
    }

    public void recordCompletion(long totalTimeMillis) {
        customersServed.incrementAndGet();
        totalServiceTimeMillis.addAndGet(totalTimeMillis);
    }

    public void printStatistics() {
        System.out.println();
        System.out.println("========== Simulation Statistics ==========");
        System.out.println("Total customers served       : " + getCustomersServed());
        System.out.printf( "Average total time / customer: %.0f ms%n", getAverageServiceTimeMillis());
        System.out.println("Max concurrent washers in use : " + getMaxWashersInUse() + " / " + NUM_WASHERS);
        System.out.println("Max concurrent dryers in use  : " + getMaxDryersInUse() + " / " + NUM_DRYERS);
        System.out.println("Max concurrent kiosks in use  : " + getMaxKiosksInUse() + " / " + NUM_KIOSKS);
        System.out.println("=============================================");
    }
}
