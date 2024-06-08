package ir.ramtung.tinyme.domain.service;

import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.messaging.request.MatchingState;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;

@Service
public class Matcher {
    @Autowired
    private MatchingControlList controls;

    public MatchResult match(Order newOrder) {
        OrderBook orderBook = newOrder.getSecurity().getOrderBook();
        LinkedList<Trade> trades = new LinkedList<>();
        int executedQuantity = 0;

        while (orderBook.hasOrderOfType(newOrder.getSide().opposite()) && newOrder.getQuantity() > 0) {
            Order matchingOrder = orderBook.matchWithFirst(newOrder);
            if (matchingOrder == null)
                break;

            Trade trade = new Trade(newOrder.getSecurity(), matchingOrder.getPrice(), Math.min(newOrder.getQuantity(), matchingOrder.getQuantity()), newOrder, matchingOrder);
            MatchingOutcome outcome = controls.canTrade(newOrder, trade);
            if(outcome != MatchingOutcome.EXECUTED){
                rollbackTrades(newOrder, trades);
                return new MatchResult(outcome, newOrder);
            }

            controls.tradeAccepted(newOrder, trade);
            trades.add(trade);
            executedQuantity += trade.getQuantity();

            if (newOrder.getQuantity() >= matchingOrder.getQuantity()) {
                newOrder.decreaseQuantity(matchingOrder.getQuantity());
                orderBook.removeFirst(matchingOrder.getSide());
                if (matchingOrder instanceof IcebergOrder icebergOrder) {
                    icebergOrder.decreaseQuantity(matchingOrder.getQuantity());
                    icebergOrder.replenish();
                    if (icebergOrder.getQuantity() > 0)
                        orderBook.enqueue(icebergOrder);
                }
            } else {
                matchingOrder.decreaseQuantity(newOrder.getQuantity());
                newOrder.makeQuantityZero();
            }
        }
        if (executedQuantity < newOrder.getMinimumExecutionQuantity()) {
            rollbackTrades(newOrder, trades);
            return MatchResult.notEnoughInitialExecution();
        }

        updateSecurityMarkertPrice(newOrder.getSecurity(), trades);
        return MatchResult.executed(newOrder, trades);
    }

    public MatchResult auctionSubmit(Order newOrder) {
        if(newOrder.getBroker().getCredit() < newOrder.getValue())
            return MatchResult.notEnoughCredit();
        else {
            Security securityOfNewOrder = newOrder.getSecurity();
            securityOfNewOrder.getOrderBook().enqueue(newOrder);
            securityOfNewOrder.updateOpeningPrice();
            if(newOrder.getSide() == Side.BUY)
                newOrder.getBroker().decreaseCreditBy(newOrder.getValue());
            return MatchResult.auctioned();
        }
    }

    public MatchResult auctionMatch(OrderBook orderBook){
        LinkedList<Trade> auctionTrades = new LinkedList<Trade>();

        while(orderBook.hasOrderOfType(orderBook.getBuyQueue().getFirst().getSide().opposite()) && orderBook.getBuyQueue().getFirst().getQuantity() > 0) {
            Order topBuyOrder = orderBook.getBuyQueue().getFirst();
            Order matchingOrder = orderBook.matchWithFirst(topBuyOrder);
            if (matchingOrder == null)
                break;
            Trade trade = new Trade(topBuyOrder.getSecurity(), matchingOrder.getSecurity().getOpeningPrice(), Math.min(topBuyOrder.getQuantity(), matchingOrder.getQuantity()), topBuyOrder, matchingOrder);
            trade.increaseSellersCredit();
            topBuyOrder.getBroker().increaseCreditBy((long) trade.getQuantity()*topBuyOrder.getPrice() - trade.getTradedValue());

            if (topBuyOrder.getQuantity() >= matchingOrder.getQuantity()) {
                topBuyOrder.decreaseQuantity(matchingOrder.getQuantity());
                orderBook.removeFirst(matchingOrder.getSide());
                if (matchingOrder instanceof IcebergOrder icebergOrder) {
                    icebergOrder.decreaseQuantity(matchingOrder.getQuantity());
                    icebergOrder.replenish();
                    if (icebergOrder.getQuantity() > 0)
                        orderBook.enqueue(icebergOrder);
                }
            } else {
                matchingOrder.decreaseQuantity(topBuyOrder.getQuantity());
                topBuyOrder.makeQuantityZero();
            }

            if(topBuyOrder.getQuantity() == 0)
                orderBook.removeFirst(Side.BUY);

            auctionTrades.add(trade);
        }

        if (!auctionTrades.isEmpty()) {
            for (Trade trade : auctionTrades) {
                trade.getBuy().getShareholder().incPosition(trade.getSecurity(), trade.getQuantity());
                trade.getSell().getShareholder().decPosition(trade.getSecurity(), trade.getQuantity());
            }
        }

        return MatchResult.executed(null, auctionTrades);
    }

    private void rollbackTrades(Order newOrder, LinkedList<Trade> trades) {
        if(newOrder.getSide() == Side.BUY) {
            newOrder.getBroker().increaseCreditBy(trades.stream().mapToLong(Trade::getTradedValue).sum());
            trades.forEach(trade -> trade.getSell().getBroker().decreaseCreditBy(trade.getTradedValue()));

            ListIterator<Trade> it = trades.listIterator(trades.size());
            while (it.hasPrevious()) {
                newOrder.getSecurity().getOrderBook().restoreSellOrder(it.previous().getSell());
            }
        }
        else { //newOrder.getSide() == Side.SELL
            newOrder.getBroker().decreaseCreditBy(trades.stream().mapToLong(Trade::getTradedValue).sum());
            trades.forEach(trade -> trade.getBuy().getBroker().increaseCreditBy(trade.getTradedValue()));

            ListIterator<Trade> it = trades.listIterator(trades.size());
            while(it.hasPrevious()){
                newOrder.getSecurity().getOrderBook().restoreBuyOrder(it.previous().getBuy());
            }
        }
    }

    public MatchResult execute(Order order) {
        MatchResult result = null;
        if(order.getSecurity().getMatchingState() == MatchingState.AUCTION)
            result = auctionSubmit(order);
        else
            result = match(order);

        if (result.outcome() == MatchingOutcome.NOT_ENOUGH_CREDIT)
            return result;

        if (result.outcome() == MatchingOutcome.NOT_ENOUGH_INITIAL_EXECUTION){
            return result;
        }
        if(result.outcome() == MatchingOutcome.AUCTIONED)
            return result;

        if (result.remainder().getQuantity() > 0) {
            if (order.getSide() == Side.BUY) {
                if (!order.getBroker().hasEnoughCredit(order.getValue())) {
                    rollbackTrades(order, result.trades());
                    return MatchResult.notEnoughCredit();
                }
                order.getBroker().decreaseCreditBy(order.getValue());
            }
            order.getSecurity().getOrderBook().enqueue(result.remainder());
        }
        if (!result.trades().isEmpty()) {
            for (Trade trade : result.trades()) {
                trade.getBuy().getShareholder().incPosition(trade.getSecurity(), trade.getQuantity());
                trade.getSell().getShareholder().decPosition(trade.getSecurity(), trade.getQuantity());
            }
        }
        return result;
    }

    private void updateSecurityMarkertPrice(Security security, LinkedList<Trade> trades){
        if (!trades.isEmpty()) {
            int lastTradePrice = trades.getLast().getPrice();
            security.updateMarketPrice(lastTradePrice);
        }


    }
}
