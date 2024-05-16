package ir.ramtung.tinyme.domain.entity;

import ir.ramtung.tinyme.messaging.exception.InvalidRequestException;
import ir.ramtung.tinyme.messaging.request.DeleteOrderRq;
import ir.ramtung.tinyme.messaging.request.EnterOrderRq;
import ir.ramtung.tinyme.domain.service.Matcher;
import ir.ramtung.tinyme.messaging.Message;
import ir.ramtung.tinyme.messaging.request.MatchingState;
import ir.ramtung.tinyme.messaging.request.OrderEntryType;
import lombok.Builder;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;

@Getter
@Builder
public class Security {
    private String isin;
    @Builder.Default
    private int tickSize = 1;
    @Builder.Default
    private int lotSize = 1;
    @Builder.Default
    private OrderBook orderBook = new OrderBook();
    @Builder.Default
    private StopLimitOrderBook inactiveOrderBook = new StopLimitOrderBook();
    @Builder.Default
    private int marketPrice = 0;
    @Builder.Default
    private MatchingState matchingState = MatchingState.CONTINUOUS;
    @Builder.Default
    private int openingPrice = 0;

    public MatchResult newOrder(EnterOrderRq enterOrderRq, Broker broker, Shareholder shareholder, Matcher matcher) {
        //TODO : Here is a bug : if the seller does not have enough credit and want to save a stopLimitOrder . it will failed (Play with OrderStatus)
        if (enterOrderRq.getSide() == Side.SELL &&
                !shareholder.hasEnoughPositionsOn(this, orderBook.totalSellQuantityByShareholder(shareholder) + inactiveOrderBook.totalSellQuantityByShareholder(shareholder) + enterOrderRq.getQuantity())
        )
            return MatchResult.notEnoughPositions();

        Order order;
        if (enterOrderRq.getPeakSize() == 0 && enterOrderRq.getStopPrice() == 0)
            order = new Order(enterOrderRq.getOrderId(), this, enterOrderRq.getSide(),
                    enterOrderRq.getQuantity(), enterOrderRq.getPrice(), broker, shareholder, enterOrderRq.getEntryTime(), OrderStatus.NEW, enterOrderRq.getMinimumExecutionQuantity());
        else if (enterOrderRq.getRequestType() == OrderEntryType.ACTIVATED_ORDER)
            order = new Order(enterOrderRq.getOrderId(), this, enterOrderRq.getSide(),
                    enterOrderRq.getQuantity(), enterOrderRq.getPrice(), broker, shareholder, enterOrderRq.getEntryTime(), OrderStatus.ACTIVATED);
        else if ((enterOrderRq.getSide() == Side.SELL && enterOrderRq.getStopPrice() >= this.marketPrice)
                    || (enterOrderRq.getSide() == Side.BUY && enterOrderRq.getStopPrice() <= this.marketPrice))
            order = new Order(enterOrderRq.getOrderId(), this, enterOrderRq.getSide(),
                    enterOrderRq.getQuantity(), enterOrderRq.getPrice(), broker, shareholder, enterOrderRq.getEntryTime(), OrderStatus.NEW);

        else if ((enterOrderRq.getStopPrice() != 0)) {
            inactiveOrderBook.enqueue(new StopLimitOrder(enterOrderRq.getOrderId(), this, enterOrderRq.getSide(),
                    enterOrderRq.getQuantity(), enterOrderRq.getPrice(), broker, shareholder, enterOrderRq.getEntryTime(), enterOrderRq.getStopPrice()));
            return MatchResult.notEnoughMarketPrice();
        }
        else
            order = new IcebergOrder(enterOrderRq.getOrderId(), this, enterOrderRq.getSide(),
                    enterOrderRq.getQuantity(), enterOrderRq.getPrice(), broker, shareholder,
                    enterOrderRq.getEntryTime(), enterOrderRq.getPeakSize());

        return matcher.execute(order);
    }

    public void deleteOrder(DeleteOrderRq deleteOrderRq) throws InvalidRequestException {
        Order order = orderBook.findByOrderId(deleteOrderRq.getSide(), deleteOrderRq.getOrderId());
        if (order == null)
            order = inactiveOrderBook.findByOrderId(deleteOrderRq.getSide(), deleteOrderRq.getOrderId());
        if (order == null)
            throw new InvalidRequestException(Message.ORDER_ID_NOT_FOUND);
        
        if (order.getSide() == Side.BUY)
            order.getBroker().increaseCreditBy(order.getValue());

        if (order instanceof StopLimitOrder)
            inactiveOrderBook.removeByOrderId(deleteOrderRq.getSide(), deleteOrderRq.getOrderId());
        else
            orderBook.removeByOrderId(deleteOrderRq.getSide(), deleteOrderRq.getOrderId());
    }

    public MatchResult updateOrder(EnterOrderRq updateOrderRq, Matcher matcher) throws InvalidRequestException {
        Order order = orderBook.findByOrderId(updateOrderRq.getSide(), updateOrderRq.getOrderId());
        if (order == null)
            order = inactiveOrderBook.findByOrderId(updateOrderRq.getSide(), updateOrderRq.getOrderId());
        if (order == null)
            throw new InvalidRequestException(Message.ORDER_ID_NOT_FOUND);
        if ((order instanceof IcebergOrder) && updateOrderRq.getPeakSize() == 0)
            throw new InvalidRequestException(Message.INVALID_PEAK_SIZE);
        if (!(order instanceof IcebergOrder) && updateOrderRq.getPeakSize() != 0)
            throw new InvalidRequestException(Message.CANNOT_SPECIFY_PEAK_SIZE_FOR_A_NON_ICEBERG_ORDER);
        if (!(order instanceof StopLimitOrder) && updateOrderRq.getStopPrice() != 0)
            throw new InvalidRequestException(Message.CANNOT_SPECIFY_STOP_PRICE_FOR_A_NON_STOP_LIMIT_ORDER);

        if (updateOrderRq.getSide() == Side.SELL &&
                !order.getShareholder().hasEnoughPositionsOn(this,
                orderBook.totalSellQuantityByShareholder(order.getShareholder()) - order.getQuantity() + updateOrderRq.getQuantity()))
            return MatchResult.notEnoughPositions();

        boolean losesPriority = order.isQuantityIncreased(updateOrderRq.getQuantity())
                || updateOrderRq.getPrice() != order.getPrice()
                || ((order instanceof IcebergOrder icebergOrder) && (icebergOrder.getPeakSize() < updateOrderRq.getPeakSize()));

        if (updateOrderRq.getSide() == Side.BUY) {
            order.getBroker().increaseCreditBy(order.getValue());
        }
        Order originalOrder = order.snapshot();
        order.updateFromRequest(updateOrderRq);
        if (!losesPriority && !(order instanceof StopLimitOrder)) {
            if (updateOrderRq.getSide() == Side.BUY) {
                order.getBroker().decreaseCreditBy(order.getValue());
            }
            return MatchResult.executed(null, List.of());
        }
        else if (order instanceof StopLimitOrder stopLimitOrder) {
            if (stopLimitOrder.getSide() == Side.BUY && !stopLimitOrder.getBroker().hasEnoughCredit(stopLimitOrder.getValue())){
                inactiveOrderBook.removeByOrderId(order.getSide() ,order.getOrderId());
                inactiveOrderBook.enqueue(originalOrder);
                return MatchResult.notEnoughCredit();
            }
            if ((stopLimitOrder.getSide() == Side.SELL && stopLimitOrder.getStopPrice() >= this.marketPrice)
                    || (stopLimitOrder.getSide() == Side.BUY && stopLimitOrder.getStopPrice() <= this.marketPrice)){
                stopLimitOrder.getBroker().decreaseCreditBy(stopLimitOrder.getValue());
                inactiveOrderBook.removeByOrderId(order.getSide(), order.getOrderId());
                Order executableOrder = new Order(stopLimitOrder.getOrderId(), stopLimitOrder.getSecurity(), stopLimitOrder.getSide(), stopLimitOrder.getQuantity(), stopLimitOrder.getPrice(), stopLimitOrder.getBroker(), stopLimitOrder.getShareholder(), stopLimitOrder.getEntryTime(), OrderStatus.ACTIVATED);
                return matcher.execute(executableOrder);
            }
            else {
                inactiveOrderBook.enqueue(stopLimitOrder);
                return MatchResult.notEnoughMarketPrice();
            }
        }
        else
            order.markAsNew();

        orderBook.removeByOrderId(updateOrderRq.getSide(), updateOrderRq.getOrderId());
        MatchResult matchResult = matcher.execute(order);
        if (matchResult.outcome() != MatchingOutcome.EXECUTED) {
            orderBook.enqueue(originalOrder);
            if (updateOrderRq.getSide() == Side.BUY) {
                originalOrder.getBroker().decreaseCreditBy(originalOrder.getValue());
            }
        }
        return matchResult;
    }

    public void updateMarketPrice(int newPrice) { this.marketPrice = newPrice; }

    //TODO : This function does 2 jobs at the same time, any alternatives?
    public LinkedList<Trade> changeMatchingState(MatchingState newMatchingState, Matcher matcher) {
        this.matchingState = newMatchingState;
        //TODO : match all the order in the orderBook
        return new LinkedList<>();
    }

    public void updateOpeningPrice(Order newOrder){
        //TODO : implement opening price calculations
    }

    public int getTradableQuantity() {
        //TODO : calculate tradable quantity based on OpeningPrice and orderBook
        return 0 ;
    }

}
