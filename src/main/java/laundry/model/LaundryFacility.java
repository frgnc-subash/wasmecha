package laundry.model;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Shared state of ONE simulation run: the three machine pools, the
 * congested-scenario coordination and the statistics.
 *
 * Every customer thread, the owner thread and the GUI share this object, so
 * all mutable state is either thread-safe (Semaphore, CountDownLatch,
 * atomics) or volatile.
 */
public class LaundryFacility {

    public static final int NUM_WASHERS = 6;
    public static final int NUM_DRYERS = 4;
    public static final int NUM_KIOSKS = 2;

    /** Congested scenario: the owner is called when this many are stuck at payment. */
    public static final int OWNER_CALL_THRESHOLD = 30;

    private final Scenario scenario;
    private final Consumer<String> logger;
    private final long startMillis = System.currentTimeMillis();
    private volatile long endMillis; // 0 while running

    private final MachinePool washers = new MachinePool(NUM_WASHERS);
    private final MachinePool dryers = new MachinePool(NUM_DRYERS);
    private final MachinePool kiosks = new MachinePool(NUM_KIOSKS);

    // ----- Congested scenario (bonus) -----
    // volatile: written by the owner thread, read by customers and the GUI.
    private volatile boolean kiosksDown;
    private volatile String ownerStatus;
    // Customers wait on this latch until the owner repairs the kiosks.
    private final CountDownLatch kiosksRepaired;
    // The owner thread waits on this latch until a customer calls.
    private final CountDownLatch ownerCall = new CountDownLatch(1);
    private final AtomicBoolean ownerCalled = new AtomicBoolean(false);
    private final AtomicInteger stuckAtPayment = new AtomicInteger();

    // ----- Statistics: atomics so many threads can update without locks -----
    private final AtomicInteger arrived = new AtomicInteger();
    private final AtomicInteger served = new AtomicInteger();
    private final AtomicLong totalTimeMillis = new AtomicLong();
    private final AtomicInteger washerFailures = new AtomicInteger();
    private final AtomicInteger kioskFailures = new AtomicInteger();

    public LaundryFacility(Scenario scenario, Consumer<String> logger) {
        this.scenario = scenario;
        this.logger = logger;
        boolean congested = scenario == Scenario.CONGESTED;
        this.kiosksDown = congested;
        this.kiosksRepaired = new CountDownLatch(congested ? 1 : 0);
        this.ownerStatus = congested ? "On standby" : "Not needed";
    }

    /**
     * Logs a line tagged with the elapsed time and the name of the thread
     * that is ACTUALLY running, proving no thread acts for another.
     */
    public void log(String message) {
        logger.accept(String.format("[%5.1fs] %-12s | %s",
            elapsedMillis() / 1000.0, Thread.currentThread().getName(), message));
    }

    // ----- Payment queue / owner coordination -----

    /**
     * Called by a customer before paying. While the kiosks are down the
     * customer blocks here; the customer that makes the queue reach the
     * threshold calls the owner. compareAndSet guarantees exactly one call.
     */
    public void awaitWorkingKiosks() throws InterruptedException {
        if (!kiosksDown) {
            return;
        }
        int stuck = stuckAtPayment.incrementAndGet();
        log("kiosks are OUT OF ORDER, stuck in the payment queue (" + stuck + " waiting)");
        if (stuck >= OWNER_CALL_THRESHOLD && ownerCalled.compareAndSet(false, true)) {
            log(stuck + " customers stuck at payment -> CALLING THE OWNER!");
            ownerCall.countDown();
        }
        try {
            kiosksRepaired.await();
        } finally {
            stuckAtPayment.decrementAndGet();
        }
    }

    /** Owner thread blocks here until a customer calls. */
    public void awaitOwnerCall() throws InterruptedException {
        ownerCall.await();
    }

    /** Owner fixes the kiosks and releases every waiting customer at once. */
    public void repairKiosks() {
        kiosksDown = false;
        kiosksRepaired.countDown();
    }

    // ----- Statistics -----

    public void customerArrived() {
        arrived.incrementAndGet();
    }

    public void recordCompletion(long timeMillis) {
        served.incrementAndGet();
        totalTimeMillis.addAndGet(timeMillis);
    }

    public void recordWasherFailure() {
        washerFailures.incrementAndGet();
    }

    public void recordKioskFailure() {
        kioskFailures.incrementAndGet();
    }

    /** Marks the run as over and prints the final statistics. */
    public void finish() {
        endMillis = System.currentTimeMillis();
        log("================ STATISTICS ================");
        log("Scenario                   : " + scenario);
        log("Total customers served     : " + served.get());
        log(String.format("Average time per customer  : %.1f s", getAverageTimeSeconds()));
        log("Max washers in use at once : " + washers.getMaxInUse() + " / " + NUM_WASHERS);
        log("Max dryers in use at once  : " + dryers.getMaxInUse() + " / " + NUM_DRYERS);
        log("Washer failures            : " + washerFailures.get());
        log("Kiosk failures             : " + kioskFailures.get());
        log(String.format("Total simulation time      : %.1f s", elapsedMillis() / 1000.0));
    }

    // ----- Getters -----

    public MachinePool washers() {
        return washers;
    }

    public MachinePool dryers() {
        return dryers;
    }

    public MachinePool kiosks() {
        return kiosks;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public boolean areKiosksDown() {
        return kiosksDown;
    }

    public String getOwnerStatus() {
        return ownerStatus;
    }

    public void setOwnerStatus(String status) {
        this.ownerStatus = status;
    }

    public int getPaymentQueue() {
        return stuckAtPayment.get() + kiosks.getWaiting();
    }

    public int getArrived() {
        return arrived.get();
    }

    public int getServed() {
        return served.get();
    }

    public double getAverageTimeSeconds() {
        int n = served.get();
        return n == 0 ? 0 : totalTimeMillis.get() / 1000.0 / n;
    }

    public int getWasherFailures() {
        return washerFailures.get();
    }

    public int getKioskFailures() {
        return kioskFailures.get();
    }

    public long elapsedMillis() {
        long end = endMillis == 0 ? System.currentTimeMillis() : endMillis;
        return end - startMillis;
    }
}
