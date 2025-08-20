package com.dta.nano;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;

public class ItchReceiver {
    private static final int BUFFER_SIZE = 65536;
    private final DatagramChannel channel;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private final RingBuffer<MarketEvent> ringBuffer;
    private final OrderBook orderBook = new OrderBook();

    // Event for Disruptor
    public static class MarketEvent {
        private String symbol;
        private char buySell;
        private int shares;
        private int price;

        public void set(String symbol, char buySell, int shares, int price) {
            this.symbol = symbol;
            this.buySell = buySell;
            this.shares = shares;
            this.price = price;
        }

        public static final EventFactory<MarketEvent> FACTORY = MarketEvent::new;
    }

    public ItchReceiver(String multicastGroup, int port) throws Exception {
        // Setup UDP channel
        channel = DatagramChannel.open(StandardProtocolFamily.INET);
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        channel.bind(new InetSocketAddress(port));
        channel.join(InetAddress.getByName(multicastGroup), NetworkInterface.getByName("eth0"));

        // Setup Disruptor
        Disruptor<MarketEvent> disruptor = new Disruptor<>(MarketEvent.FACTORY, 1024, DaemonThreadFactory.INSTANCE);
        disruptor.handleEventsWith((event, sequence, endOfBatch) -> {
            orderBook.updateAdd(event.symbol, event.buySell, event.shares, event.price);
            // Trigger routing logic in FixRouter (injected later)
        });
        disruptor.start();
        ringBuffer = disruptor.getRingBuffer();
    }

    public void start() throws Exception {
        while (!Thread.interrupted()) {
            buffer.clear();
            channel.receive(buffer);
            buffer.flip();
            processMessages(buffer);
        }
    }

    private void processMessages(ByteBuffer buf) {
        while (buf.hasRemaining()) {
            short msgLength = buf.getShort();
            if (msgLength <= 0 || buf.remaining() < msgLength - 2) break;

            char msgType = (char) buf.get();
            if (msgType == 'A') { // Add Order
                buf.getShort(); // Stock Locate
                buf.getLong(); // Timestamp
                buf.getLong(); // Order Reference
                char buySell = (char) buf.get();
                int shares = buf.getInt();
                String symbol = getAlpha(buf, 8);
                int price = buf.getInt();

                // Publish to Disruptor
                long sequence = ringBuffer.next();
                try {
                    MarketEvent event = ringBuffer.get(sequence);
                    event.set(symbol, buySell, shares, price);
                } finally {
                    ringBuffer.publish(sequence);
                }
            } else {
                buf.position(buf.position() + msgLength - 3); // Skip
            }
        }
    }

    private String getAlpha(ByteBuffer buf, int len) {
        byte[] bytes = new byte[len];
        buf.get(bytes);
        return new String(bytes).trim();
    }

    public OrderBook getOrderBook() {
        return orderBook;
    }
}