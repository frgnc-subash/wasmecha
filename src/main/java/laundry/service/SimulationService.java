package laundry.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import laundry.model.Customer;
import laundry.model.LaundryFacility;

/**
 * Orchestrates a single simulation run: spawns customer arrivals on a
 * background thread so callers (e.g. the GUI's Start button) never block.
 */
@Service
public class SimulationService {

    public static final int DEFAULT_NUM_CUSTOMERS = 50;
    public static final int MIN_NUM_CUSTOMERS = 1;
    public static final int MAX_NUM_CUSTOMERS = 300;

    private final LaundryFacility facility;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public SimulationService(LaundryFacility facility) {
        this.facility = facility;
    }

    public boolean isRunning() {
        return running.get();
    }

    /**
     * Starts a simulation run of {@code customerCount} customers in the
     * background. {@code onFinished} is invoked (off the EDT) once every
     * customer thread has completed.
     */
    public void start(int customerCount, Consumer<Long> onFinished) {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        Thread orchestrator = new Thread(() -> runSimulation(customerCount, onFinished), "SimulationOrchestrator");
        orchestrator.setDaemon(true);
        orchestrator.start();
    }

    private void runSimulation(int customerCount, Consumer<Long> onFinished) {
        facility.reset();
        List<Thread> customerThreads = new ArrayList<>();

        facility.log("Smart Laundry Facility Simulation starting...");
        facility.log(LaundryFacility.NUM_WASHERS + " washers, "
                + LaundryFacility.NUM_DRYERS + " dryers, "
                + LaundryFacility.NUM_KIOSKS + " payment kiosks, "
                + customerCount + " customers.");

        long start = System.currentTimeMillis();

        for (int i = 1; i <= customerCount; i++) {
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(0, 3001)); // 0-3s
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            Thread t = new Thread(new Customer(i, facility), "Customer-" + i + "-Thread");
            customerThreads.add(t);
            t.start();
        }

        for (Thread t : customerThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        long duration = System.currentTimeMillis() - start;
        facility.log("Simulation finished in " + duration + " ms.");

        running.set(false);
        onFinished.accept(duration);
    }
}
