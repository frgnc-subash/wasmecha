package laundry.model;

/**
 * Bonus scenario: the shop owner, running on its own thread.
 *
 * Sleeps on a latch (no busy-waiting) until 30 customers are stuck at
 * payment, then travels in, repairs both kiosks and releases the queue.
 */
public class Owner implements Runnable {

    private final LaundryFacility shop;

    public Owner(LaundryFacility shop) {
        this.shop = shop;
    }

    @Override
    public void run() {
        try {
            shop.log("both payment kiosks are OUT OF ORDER today, owner on standby");
            shop.awaitOwnerCall();

            shop.setOwnerStatus("Called in, travelling");
            shop.log("got the call, travelling to the laundromat");
            Thread.sleep(3000);

            shop.setOwnerStatus("Repairing kiosks");
            shop.log("arrived, repairing both kiosks");
            Thread.sleep(2000);

            shop.repairKiosks();
            shop.setOwnerStatus("Kiosks repaired");
            shop.log("kiosks back ONLINE, the payment queue can move");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
