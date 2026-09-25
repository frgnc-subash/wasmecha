# wasmecha

-  wasmecha is a simple simulation of customers washing their clothes with washing  machine and kiosk system in maven.

## Run

```bash
./mvnw package -DskipTests
java -jar target/wasmecha-1.0-SNAPSHOT.jar
```

Pick a scenario in the window and press **Start simulation**. The console prints the same event log as the GUI.

## Scenarios

- **Normal day**: 50 customers, 6 washers, 4 dryers, 2 kiosks. Takes about 80–90 s.
- **Congested (bonus)**: both kiosks are out of order. Customers pile up at payment, and the 30th one calls the owner, who travels in, repairs the kiosks and releases the queue. Takes about 100 s.

## Assumptions

- Each customer has one load and goes wash → dry → pay, in that order.
- Customers are served in arrival order at every stage (fair semaphores).
- When a washer fails mid-cycle, the customer spends 1 s unloading, releases the machine and rejoins the washer queue. The machine is usable again straight away.
- When a kiosk fails, the customer releases it, waits 2 s and queues again.
- Congested scenario: the owner can't take payments by hand. They arrive 3 s after the call, spend 2 s repairing both kiosks, and after that the kiosks work normally (the 5% failure chance still applies).
- Customers stuck at the broken kiosks wait rather than leave without paying.
