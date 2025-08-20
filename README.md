# NanoTrade: Low-Latency Order Router

## Overview
NanoTrade is a high-performance order routing system built in Java, designed to process NASDAQ ITCH 5.0 market data and route orders via the FIX 5.0 SP2 protocol. It demonstrates expertise in low-latency programming, financial protocols, and trading system architecture.

## Features
- **ITCH Parser**: Processes binary ITCH messages (e.g., Add Order, Trade) over UDP multicast using Java NIO's `DatagramChannel`. Direct `ByteBuffer` minimizes garbage collection.
- **Order Book**: Maintains real-time bid/ask prices using `TreeMap` for O(log n) updates.
- **FIX Router**: Implements QuickFIX/J for order submission (NewOrderSingle, 35=D) and execution reports (35=8). Handles session management (Logon, Heartbeat).
- **Low-Latency Design**: Uses LMAX Disruptor for lock-free event passing, off-heap memory, and JVM tuning (-XX:+UseParallelGC).
- **Smart Routing**: Routes orders based on market signals (e.g., wide bid-ask spreads).

## Technologies
- **Java**: Core language with NIO for networking and Disruptor for concurrency.
- **Gradle**: Build automation with dependencies for QuickFIX/J, Disruptor, and SLF4J.
- **ITCH 5.0**: Parses binary messages (e.g., 2-byte length, 1-byte type 'A', 8-byte timestamp in nanoseconds).
- **FIX 5.0 SP2**: Tag-value encoding (e.g., 35=D, 11=ClOrdID). Validates checksums (10=).
- **Optimization**: Direct buffers, pinned threads, and minimal object allocation for sub-microsecond processing.

## Challenges Overcome
- Parsing high-throughput ITCH data without GC pauses.
- Ensuring reliable FIX session management under high load.
- Integrating market data with order routing in real-time.

## Future Enhancements
- Add MoldUDP64 for ITCH sequence number validation.
- Implement advanced routing strategies (e.g., VWAP, liquidity-based).
- Support for multiple venues and FIX sessions.

## Build and Run
```bash
./gradlew build
java -XX:+UseParallelGC -Xms4G -Xmx4G -jar build/libs/nanotrade-1.0-SNAPSHOT.jar
```
