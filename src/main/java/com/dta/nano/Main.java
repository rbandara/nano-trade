package com.dta.nano;

public class Main {
    public static void main(String[] args) throws Exception {
        ItchReceiver itch = new ItchReceiver("239.192.0.1", 12345); // Example multicast
        FixRouter fix = new FixRouter(itch.getOrderBook(), "localhost", 9876, "SENDER", "TARGET");

        // Start ITCH receiver in a thread
        new Thread(() -> {
            try {
                itch.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Simulate market evaluation
        while (true) {
            fix.evaluateAndRoute("BP");
            Thread.sleep(1000); // Simplified; use Disruptor events in production
        }
    }
}