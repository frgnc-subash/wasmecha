package laundry.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import laundry.model.Customer;
import laundry.model.LaundryFacility;
import laundry.model.Owner;
import laundry.model.Scenario;
import org.springframework.stereotype.Service;

/**
 * Runs a simulation on a background "ArrivalGate" thread, so the GUI never
 * blocks. Each run gets a fresh LaundryFacility.
 */
@Service
public class SimulationService {

    public static final int NUM_CUSTOMERS = 50;

    // Thread-safe list: listeners are added on the EDT, read by every thread.
    private final List<Consumer<String>> logListeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    // volatile: replaced on each run, read by the GUI timer.
    private volatile LaundryFacility facility = new LaundryFacility(Scenario.NORMAL, this::publish);

    public void addLogListener(Consumer<String> listener) {
        logListeners.add(listener);
    }

    public LaundryFacility getFacility() {
        return facility;
    }

    /** Starts a run unless one is already going. onFinished runs off the EDT. */
    public void start(Scenario scenario, Runnable onFinished) {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        LaundryFacility shop = new LaundryFacility(scenario, this::publish);
        facility = shop;

        Thread gate = new Thread(() -> {
            try {
                runSimulation(shop);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                running.set(false);
                onFinished.run();
            }
        }, "ArrivalGate");
        gate.setDaemon(true);
        gate.start();
    }

    private void runSimulation(LaundryFacility shop) throws InterruptedException {
        shop.log("simulation started: " + shop.getScenario() + ", " + NUM_CUSTOMERS + " customers");
        List<Thread> threads = new ArrayList<>();

        if (shop.getScenario() == Scenario.CONGESTED) {
            Thread owner = new Thread(new Owner(shop), "Owner");
            threads.add(owner);
            owner.start();
        }

        // Customers arrive every 0-3 seconds, each on its own thread.
        for (int id = 1; id <= NUM_CUSTOMERS; id++) {
            Thread.sleep(ThreadLocalRandom.current().nextInt(0, 3001));
            Thread customer = new Thread(new Customer(id, shop), "Customer-" + id);
            threads.add(customer);
            customer.start();
        }

        // Wait for everyone to leave before printing the statistics.
        for (Thread t : threads) {
            t.join();
        }
        shop.finish();
    }

    private void publish(String line) {
        System.out.println(line);
        for (Consumer<String> listener : logListeners) {
            listener.accept(line);
        }
    }
}
