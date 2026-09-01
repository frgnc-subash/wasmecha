package laundry.model;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Each customer is an independent thread that moves through three
 * synchronized stages of the facility: washing, drying, then payment.
 */
public class Customer implements Runnable {

    private final int id;
    private final LaundryFacility facility;

    public Customer(int id, LaundryFacility facility) {
        this.id = id;
        this.facility = facility;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        log("arrived at the facility");

        try {
            wash();
            dry();
            pay();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log("was interrupted before finishing laundry");
            return;
        }

        long totalTime = System.currentTimeMillis() - start;
        facility.recordCompletion(totalTime);
        log("all done, total time = " + totalTime + " ms");
    }

    private void wash() throws InterruptedException {
        boolean success = false;
        while (!success) {
            log("waiting for a washing machine");
            facility.acquireWasher();
            log("washing machine acquired, washing started");
            try {
                int washTime = randomMillis(4000, 6000);
                Thread.sleep(washTime);

                // 5% chance the machine fails mid-cycle.
                if (ThreadLocalRandom.current().nextInt(100) < 5) {
                    log("washing machine FAILED mid-cycle, will retry");
                } else {
                    log("washing finished");
                    success = true;
                }
            } finally {
                facility.releaseWasher();
            }
            if (!success) {
                Thread.sleep(500); // brief pause before retrying
            }
        }
    }

    private void dry() throws InterruptedException {
        log("waiting for a dryer");
        facility.acquireDryer();
        log("dryer acquired, drying started");
        try {
            int dryTime = randomMillis(3000, 5000);
            Thread.sleep(dryTime);
            log("drying finished");
        } finally {
            facility.releaseDryer();
        }
    }

    private void pay() throws InterruptedException {
        boolean success = false;
        while (!success) {
            log("waiting for a payment kiosk");
            facility.acquireKiosk();
            log("payment kiosk acquired, processing payment");
            try {
                int payTime = randomMillis(1000, 2000);
                Thread.sleep(payTime);

                // 5% chance the kiosk fails during payment.
                if (ThreadLocalRandom.current().nextInt(100) < 5) {
                    log("payment kiosk FAILED, will retry in 2s");
                } else {
                    log("payment completed");
                    success = true;
                }
            } finally {
                facility.releaseKiosk();
            }
            if (!success) {
                Thread.sleep(2000);
            }
        }
    }

    private int randomMillis(int minInclusive, int maxInclusive) {
        return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
    }

    private void log(String message) {
        facility.log(String.format("[%s] Customer %d: %s", Thread.currentThread().getName(), id, message));
    }
}
