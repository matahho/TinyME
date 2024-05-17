package ir.ramtung.tinyme.domain.entity;

import lombok.Getter;
import org.jgroups.util.Tuple;

import java.util.*;

@Getter
public class OrderBook {
    private final LinkedList<Order> buyQueue;
    private final LinkedList<Order> sellQueue;

    public OrderBook() {
        buyQueue = new LinkedList<>();
        sellQueue = new LinkedList<>();
    }

    public void enqueue(Order order) {
        List<Order> queue = getQueue(order.getSide());
        ListIterator<Order> it = queue.listIterator();
        while (it.hasNext()) {
            if (order.queuesBefore(it.next())) {
                it.previous();
                break;
            }
        }
        order.queue();
        it.add(order);
    }

    private LinkedList<Order> getQueue(Side side) {
        return side == Side.BUY ? buyQueue : sellQueue;
    }

    public Order findByOrderId(Side side, long orderId) {
        var queue = getQueue(side);
        for (Order order : queue) {
            if (order.getOrderId() == orderId)
                return order;
        }
        return null;
    }

    public boolean removeByOrderId(Side side, long orderId) {
        var queue = getQueue(side);
        var it = queue.listIterator();
        while (it.hasNext()) {
            if (it.next().getOrderId() == orderId) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public Order matchWithFirst(Order newOrder) {
        var queue = getQueue(newOrder.getSide().opposite());
        if (newOrder.matches(queue.getFirst()))
            return queue.getFirst();
        else
            return null;
    }

    public void putBack(Order order) {
        LinkedList<Order> queue = getQueue(order.getSide());
        order.queue();
        queue.addFirst(order);
    }

    public void restoreSellOrder(Order sellOrder) {
        removeByOrderId(Side.SELL, sellOrder.getOrderId());
        putBack(sellOrder);
    }

    public void restoreBuyOrder(Order buyOrder) {
        removeByOrderId(Side.BUY, buyOrder.getOrderId());
        putBack(buyOrder);
    }

    public boolean hasOrderOfType(Side side) {
        return !getQueue(side).isEmpty();
    }

    public void removeFirst(Side side) {
        getQueue(side).removeFirst();
    }

    public int totalSellQuantityByShareholder(Shareholder shareholder) {
        return sellQueue.stream()
                .filter(order -> order.getShareholder().equals(shareholder))
                .mapToInt(Order::getTotalQuantity)
                .sum();
    }

    public int calculateOpeningPrice(int lastTradePrice){
        if (buyQueue.isEmpty() || sellQueue.isEmpty()) {
            return 0;
        }
        Order cheapestBuyOrder = this.buyQueue.getLast();
        Order mostExpensiveSellOrder = this.sellQueue.getLast();

        int minDistanceToLastTradePrice = Integer.MAX_VALUE;
        int openingPrice = cheapestBuyOrder.getPrice();
        int maximumTradableQuantity = 0;


        for (int price = cheapestBuyOrder.getPrice(); price <= mostExpensiveSellOrder.getPrice(); price++) {
            int tradableQuantity = getTradableQuantityByOpeningPrice(price);
            int distanceToLastTradePrice = Math.abs(price - lastTradePrice);

            if (tradableQuantity > maximumTradableQuantity) {
                maximumTradableQuantity = tradableQuantity;
                openingPrice = price;
                minDistanceToLastTradePrice = distanceToLastTradePrice;

            } else if (tradableQuantity == maximumTradableQuantity) {
                if (distanceToLastTradePrice < minDistanceToLastTradePrice) {
                    openingPrice = price;
                    minDistanceToLastTradePrice = distanceToLastTradePrice;
                } else if (distanceToLastTradePrice == minDistanceToLastTradePrice) {
                    if (price < openingPrice) {
                        openingPrice = price;
                    }
                }
            }
        }

        return openingPrice ;
    }

    public int getTradableQuantityByOpeningPrice(int openingPrice){
        int buyPossible = buyQueue.stream()
                .filter(order -> order.getPrice() >= openingPrice)
                .mapToInt(Order::getTotalQuantity)
                .sum();
        int sellPossible = sellQueue.stream()
                .filter(order -> order.getPrice() <= openingPrice)
                .mapToInt(Order::getTotalQuantity)
                .sum();

        return Math.min(buyPossible, sellPossible);
    }
}
