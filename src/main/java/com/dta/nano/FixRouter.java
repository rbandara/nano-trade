package com.dta.nano;

import quickfix.*;
import quickfix.fix50sp2.*;0
import quickfix.field.*;

public class FixRouter extends MessageCracker implements Application {
    private SocketInitiator initiator;
    private final OrderBook orderBook;

    public FixRouter(OrderBook orderBook, String host, int port, String senderCompID, String targetCompID) throws Exception {
        this.orderBook = orderBook;
        SessionSettings settings = new SessionSettings();
        settings.setString(Session.SETTING_USE_DATA_DICTIONARY, "YES");
        settings.setString("DataDictionary", "FIX50SP2.xml");
        settings.setString("ConnectionType", "initiator");
        settings.setString("SocketConnectHost", host);
        settings.setString("SocketConnectPort", String.valueOf(port));
        settings.setString("SenderCompID", senderCompID);
        settings.setString("TargetCompID", targetCompID);
        settings.setString("HeartBtInt", "30");

        initiator = new SocketInitiator(this, new FileStoreFactory(settings), settings, new ScreenLogFactory(), new DefaultMessageFactory());
        initiator.start();
    }

    @Override
    public void onCreate(SessionID sessionID) {}
    @Override
    public void onLogon(SessionID sessionID) { System.out.println("FIX Logon"); }
    @Override
    public void onLogout(SessionID sessionID) {}
    @Override
    public void toAdmin(Message message, SessionID sessionID) {}
    @Override
    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {}
    @Override
    public void toApp(Message message, SessionID sessionID) throws DoNotSend {}
    @Override
    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        crack(message, sessionID);
    }

    public void onMessage(ExecutionReport report, SessionID sessionID) throws FieldNotFound {
        if (report.getExecType().getValue() == ExecType.FILL) {
            System.out.println("Filled: " + report.getLastQty().getValue() + " @ " + report.getLastPx().getValue());
        }
    }

    public void routeOrder(String symbol, char side, int qty, double price) throws SessionNotFound {
        NewOrderSingle order = new NewOrderSingle(
                new ClOrdID("ORD" + System.nanoTime()),
                new Symbol(symbol),
                new Side(side),
                new TransactTime(),
                new OrderQty(qty),
                new OrdType(OrdType.LIMIT)
        );
        order.set(new Price(price));
        Session.sendToTarget(order, initiator.getSessions().get(0));
    }

    public void evaluateAndRoute(String symbol) {
        int bestBid = orderBook.getBestBid(symbol);
        int bestAsk = orderBook.getBestAsk(symbol);
        if (bestBid > 0 && bestAsk < Integer.MAX_VALUE && bestAsk - bestBid > 100) { // Example: Wide spread
            routeOrder(symbol, Side.BUY, 100, bestBid / 10000.0); // ITCH price is scaled
        }
    }
}