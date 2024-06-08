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


    public int calculateOpeningPrice(int lastTradePrice) {
        if (buyQueue.isEmpty() || sellQueue.isEmpty()) {
            return 0;
        }

        Order cheapestBuyOrder = buyQueue.getLast();
        Order mostExpensiveSellOrder = sellQueue.getLast();
        return findBestOpeningPrice(cheapestBuyOrder, mostExpensiveSellOrder, lastTradePrice);
    }

    private int findBestOpeningPrice(Order cheapestBuyOrder, Order mostExpensiveSellOrder, int lastTradePrice) {
        int minDistanceToLastTradePrice = Integer.MAX_VALUE;
        int openingPrice = cheapestBuyOrder.getPrice();
        int maximumTradableQuantity = 0;

        for (int price = cheapestBuyOrder.getPrice(); price <= mostExpensiveSellOrder.getPrice(); price++) {
            int tradableQuantity = getTradableQuantityByOpeningPrice(price);
            int distanceToLastTradePrice = Math.abs(price - lastTradePrice);

            if (isBetterOpeningPrice(tradableQuantity, maximumTradableQuantity, distanceToLastTradePrice, minDistanceToLastTradePrice, price, openingPrice)) {
                maximumTradableQuantity = tradableQuantity;
                openingPrice = price;
                minDistanceToLastTradePrice = distanceToLastTradePrice;
            }
        }

        return openingPrice;
    }

    private boolean isBetterOpeningPrice(int tradableQuantity, int maximumTradableQuantity, int distanceToLastTradePrice, int minDistanceToLastTradePrice, int price, int openingPrice) {
        if (tradableQuantity > maximumTradableQuantity) {
            return true;
        } else if (tradableQuantity == maximumTradableQuantity) {
            if (distanceToLastTradePrice < minDistanceToLastTradePrice) {
                return true;
            } else if (distanceToLastTradePrice == minDistanceToLastTradePrice) {
                return price < openingPrice;
            }
        }
        return false;
    }

    public int getTradableQuantityByOpeningPrice(int openingPrice) {
        int buyPossible = calculateTradableQuantity(buyQueue, openingPrice, true);
        int sellPossible = calculateTradableQuantity(sellQueue, openingPrice, false);
        return Math.min(buyPossible, sellPossible);
    }

    private int calculateTradableQuantity(List<Order> orders, int openingPrice, boolean isBuy) {
        return orders.stream()
                .filter(order -> isOrderPriceValid(order, openingPrice, isBuy))
                .mapToInt(Order::getTotalQuantity)
                .sum();
    }

    private boolean isOrderPriceValid(Order order, int openingPrice, boolean isBuy) {
        return isBuy ? order.getPrice() >= openingPrice : order.getPrice() <= openingPrice;
    }

    public boolean isEmpty(){ return buyQueue.isEmpty() && sellQueue.isEmpty(); }
}
