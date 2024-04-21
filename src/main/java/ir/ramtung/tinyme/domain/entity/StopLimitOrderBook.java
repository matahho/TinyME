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
        LinkedList<Order> activatedStopLimits = new LinkedList<>();
        LinkedList<Order> buyQueue = getBuyQueue();
        LinkedList<Order> ordersToRemove = new LinkedList<>();
        for (Order order : buyQueue) {
            StopLimitOrder stopLimitOrder = ((StopLimitOrder) order); // Cast Type
            if (stopLimitOrder.getStopPrice() <= marketPrice) {
                Order activedOrder = stopLimitOrder.activate();
                ordersToRemove.add(activedOrder);
                activatedStopLimits.push(activedOrder);
            }
        }

        for (Order order : ordersToRemove) {
            removeByOrderId(Side.BUY, order.getOrderId());
        }
        return activatedStopLimits;
    }
    private LinkedList<Order> ActivatePossibleStopLimitsInSellQueue(int marketPrice) {
        LinkedList<Order> activatedStopLimits = new LinkedList<>();
        LinkedList<Order> sellQueue = getSellQueue();
        LinkedList<Order> ordersToRemove = new LinkedList<>();
        for (Order order : sellQueue) {
            StopLimitOrder stopLimitOrder = ((StopLimitOrder) order); // Cast Type
            if (stopLimitOrder.getStopPrice() >= marketPrice) {
                Order activedOrder = stopLimitOrder.activate();
                ordersToRemove.add(activedOrder);
                activatedStopLimits.push(activedOrder);
            }
        }
        for (Order order : ordersToRemove) {
            removeByOrderId(Side.SELL, order.getOrderId());
        }
        return activatedStopLimits;
    }

}
