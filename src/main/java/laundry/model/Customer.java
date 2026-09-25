package laundry.model;

import java.util.concurrent.ThreadLocalRandom;

/**
 * One customer = one thread. Goes through wash -> dry -> pay, and only ever
 * logs about ITSELF (the thread name is printed on every line).
 */
public class Customer implements Runnable {

    private static final int FAILURE_PERCENT = 5;

    private final int id;
    private final LaundryFacility shop;

    public Customer(int id, LaundryFacility shop) {
        this.id = id;
        this.shop = shop;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        shop.customerArrived();
        shop.log("ENTERED the laundromat");
        try {
            wash();
            dry();
            pay();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            shop.log("interrupted, leaving early");
            return;
        }
        long total = System.currentTimeMillis() - start;
        shop.recordCompletion(total);
        shop.log(String.format("EXITED the laundromat (total %.1f s)", total / 1000.0));
    }

    /** 4-6 s wash. 5% chance the machine fails: unload, re-queue, retry. */
    private void wash() throws InterruptedException {
        MachinePool washers = shop.washers();
        while (true) {
            shop.log("waiting for a washing machine");
            int slot = washers.acquire(id);
            String machine = "Washer " + (slot + 1);
            try {
                shop.log("started washing on " + machine);
                sleepBetween(4000, 6000);
                if (!fails()) {
                    shop.log("finished washing on " + machine);
                    return;
                }
                washers.markBroken(slot);
                shop.recordWasherFailure();
                shop.log(machine + " FAILED mid-cycle, unloading to retry");
                Thread.sleep(1000); // unload wet clothes
            } finally {
                washers.release(slot); // always give the machine back
            }
        }
    }

    /** 3-5 s drying. */
    private void dry() throws InterruptedException {
        MachinePool dryers = shop.dryers();
        shop.log("waiting for a dryer");
        int slot = dryers.acquire(id);
        try {
            shop.log("started drying on Dryer " + (slot + 1));
            sleepBetween(3000, 5000);
            shop.log("finished drying on Dryer " + (slot + 1));
        } finally {
            dryers.release(slot);
        }
    }

    /** 1-2 s payment. 5% chance the kiosk fails: retry after 2 s. */
    private void pay() throws InterruptedException {
        shop.awaitWorkingKiosks(); // blocks only in the congested scenario
        MachinePool kiosks = shop.kiosks();
        while (true) {
            shop.log("waiting for a payment kiosk");
            int slot = kiosks.acquire(id);
            String kiosk = "Kiosk " + (slot + 1);
            boolean paid;
            try {
                shop.log("paying at " + kiosk);
                sleepBetween(1000, 2000);
                paid = !fails();
                if (!paid) {
                    kiosks.markBroken(slot);
                    shop.recordKioskFailure();
                    shop.log(kiosk + " FAILED during payment, retrying in 2 s");
                }
            } finally {
                kiosks.release(slot);
            }
            if (paid) {
                shop.log("payment successful at " + kiosk);
                return;
            }
            Thread.sleep(2000);
        }
    }

    private static boolean fails() {
        return ThreadLocalRandom.current().nextInt(100) < FAILURE_PERCENT;
    }

    private static void sleepBetween(int minMillis, int maxMillis) throws InterruptedException {
        Thread.sleep(ThreadLocalRandom.current().nextInt(minMillis, maxMillis + 1));
    }
}
