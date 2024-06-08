package ir.ramtung.tinyme.domain.entity;

import java.util.LinkedList;

public class StopLimitOrderBook extends OrderBook{

    public LinkedList<Order> activatedStopLimits(int marketPrice){
        LinkedList<Order> activated = new LinkedList<>();
        activated.addAll(ActivatePossibleStopLimitsInBuyQueue(marketPrice));
        activated.addAll(ActivatePossibleStopLimitsInSellQueue(marketPrice));
        return activated;
    }

    private LinkedList<Order> ActivatePossibleStopLimitsInBuyQueue(int marketPrice) {
        LinkedList<Order> buyQueue = getBuyQueue();
        return processQueue(buyQueue, marketPrice, true, Side.BUY);
    }

    private LinkedList<Order> ActivatePossibleStopLimitsInSellQueue(int marketPrice) {
        LinkedList<Order> sellQueue = getSellQueue();
        return processQueue(sellQueue, marketPrice, false, Side.SELL);
    }

    private LinkedList<Order> processQueue(LinkedList<Order> queue, int marketPrice, boolean isBuyQueue, Side side) {
        LinkedList<Order> activatedStopLimits = new LinkedList<>();
        LinkedList<Order> ordersToRemove = new LinkedList<>();

        for (Order order : queue) {
            StopLimitOrder stopLimitOrder = (StopLimitOrder) order; // Cast Type
            if ((isBuyQueue && stopLimitOrder.getStopPrice() <= marketPrice) ||
                    (!isBuyQueue && stopLimitOrder.getStopPrice() >= marketPrice)) {
                Order activatedOrder = stopLimitOrder.activate();
                ordersToRemove.add(activatedOrder);
                activatedStopLimits.push(activatedOrder);
            }
        }

        for (Order order : ordersToRemove) {
            removeByOrderId(side, order.getOrderId());
        }

        return activatedStopLimits;
    }


}
