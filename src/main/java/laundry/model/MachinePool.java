package laundry.model;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A fixed group of identical machines (washers, dryers or payment kiosks).
 *
 * - A fair Semaphore limits how many customers can use the pool at once and
 *   makes the rest wait in arrival order (no starvation).
 * - A synchronized block then picks WHICH machine the customer gets, so the
 *   GUI can show "Washer 3 -> Customer 12". Two customers can never be
 *   assigned the same machine.
 * - Atomic counters track waiting / in-use / peak usage without locking.
 */
public class MachinePool {

    private final Semaphore permits;
    private final int[] occupant; // 0 = free, otherwise the customer id
    private final boolean[] broken; // machine failed during the current use

    private final AtomicInteger waiting = new AtomicInteger();
    private final AtomicInteger inUse = new AtomicInteger();
    private final AtomicInteger maxInUse = new AtomicInteger();

    public MachinePool(int size) {
        this.permits = new Semaphore(size, true);
        this.occupant = new int[size];
        this.broken = new boolean[size];
    }

    /** Blocks until a machine is free, then returns its index (0-based). */
    public int acquire(int customerId) throws InterruptedException {
        waiting.incrementAndGet();
        try {
            permits.acquire();
        } finally {
            waiting.decrementAndGet();
        }
        int slot = claimFreeMachine(customerId);
        // accumulateAndGet is an atomic "max = Math.max(max, current)".
        maxInUse.accumulateAndGet(inUse.incrementAndGet(), Math::max);
        return slot;
    }

    /** Gives the machine back and wakes the next waiting customer. */
    public void release(int slot) {
        synchronized (this) {
            occupant[slot] = 0;
            broken[slot] = false;
        }
        inUse.decrementAndGet();
        permits.release();
    }

    public synchronized void markBroken(int slot) {
        broken[slot] = true;
    }

    // Holding a semaphore permit guarantees at least one machine is free.
    private synchronized int claimFreeMachine(int customerId) {
        for (int i = 0; i < occupant.length; i++) {
            if (occupant[i] == 0) {
                occupant[i] = customerId;
                return i;
            }
        }
        throw new IllegalStateException("permit granted but no machine free");
    }

    // ----- read-only views used by the GUI and statistics -----

    public int size() {
        return occupant.length;
    }

    public synchronized int occupantOf(int slot) {
        return occupant[slot];
    }

    public synchronized boolean isBroken(int slot) {
        return broken[slot];
    }

    public int getWaiting() {
        return waiting.get();
    }

    public int getMaxInUse() {
        return maxInUse.get();
    }
}
