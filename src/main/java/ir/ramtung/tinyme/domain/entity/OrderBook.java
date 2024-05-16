package ir.ramtung.tinyme.domain.entity;

import lombok.Getter;
import org.jgroups.util.Tuple;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

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
        Order chippestBuyOrder = this.buyQueue.getLast();
        Order mostExpensiveSellOrder = this.sellQueue.getFirst();

        int minDistanceToLastTradePrice = Integer.MAX_VALUE;
        int openingPrice = chippestBuyOrder.getPrice();
        int maximumTradableQuantity = 0;


        for (int price = chippestBuyOrder.getPrice(); price <= mostExpensiveSellOrder.getPrice(); price++) {
            int tradableQuantity = calculateQuantityForAnOpeningPrice(price);
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

    protected int calculateQuantityForAnOpeningPrice(int openingPrice){
        int possibleBuyQuantity = 0;
        int possibleSellQuantity = 0;
        for (Order buyOrder:this.getBuyQueue()){
            if (buyOrder.getPrice() >= openingPrice)
                possibleBuyQuantity += buyOrder.getQuantity();
            else
                break;
        }
        for (Order sellOrder:this.getSellQueue()){
            if (sellOrder.getPrice() <= openingPrice)
                possibleBuyQuantity += sellOrder.getQuantity();
            else
                break;
        }
        return Math.min(possibleSellQuantity, possibleBuyQuantity);

    }

    public long getTradableQuantityByOpeningPrice(int openingPrice){
        long buyPossible = buyQueue.stream()
                .filter(order -> order.getPrice() <= openingPrice)
                .mapToInt(Order::getTotalQuantity)
                .sum();
        long sellPossible = sellQueue.stream()
                .filter(order -> order.getPrice() >= openingPrice)
                .mapToInt(Order::getTotalQuantity)
                .sum();

        return Math.min(buyPossible, sellPossible);
    }


}
