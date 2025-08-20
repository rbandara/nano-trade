package com.dta.nano;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class OrderBook {
    private final Map<String, TreeMap<Integer, Integer>> bids = new HashMap<>();
    private final Map<String, TreeMap<Integer, Integer>> asks = new HashMap<>();

    public void updateAdd(String symbol, char buySell, int shares, int price) {
        Map<String, TreeMap<Integer, Integer>> book = buySell == 'B' ? bids : asks;
        book.computeIfAbsent(symbol, k -> new TreeMap<>(buySell == 'B' ? Map.Entry.comparingByKey().reversed() : Map.Entry.comparingByKey()))
                .merge(price, shares, Integer::sum);
    }

    public int getBestBid(String symbol) {
        TreeMap<Integer, Integer> bid = bids.getOrDefault(symbol, new TreeMap<>());
        return bid.isEmpty() ? 0 : bid.firstKey();
    }

    public int getBestAsk(String symbol) {
        TreeMap<Integer, Integer> ask = asks.getOrDefault(symbol, new TreeMap<>());
        return ask.isEmpty() ? Integer.MAX_VALUE : ask.firstKey();
    }
}