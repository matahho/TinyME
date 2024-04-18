package ir.ramtung.tinyme.domain.entity;

import ir.ramtung.tinyme.messaging.request.EnterOrderRq;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StopLimitOrder extends Order{
    int stopPrice;

    public StopLimitOrder(long orderId, Security security, Side side, int quantity, int price, Broker broker, Shareholder shareholder, LocalDateTime entryTime, OrderStatus status, int stopPrice){
        super(orderId, security, side, quantity, price, broker, shareholder, entryTime, status);
        this.stopPrice = stopPrice;
    }

    public StopLimitOrder(long orderId, Security security, Side side, int quantity, int price, Broker broker, Shareholder shareholder, LocalDateTime entryTime, int stopPrice){
        super(orderId, security, side, quantity, price, broker, shareholder, entryTime);
        this.stopPrice = stopPrice;
        this.status = OrderStatus.INACTIVE;

    }

    @Override
    public Order snapshot(){
        return new StopLimitOrder(orderId, security, side, quantity, price, broker, shareholder, entryTime, status, stopPrice);
    }

    @Override
    public Order snapshotWithQuantity(int newQuantity){
        return new StopLimitOrder(orderId, security, side, newQuantity, price, broker, shareholder, entryTime, status, stopPrice);
    }

    @Override
    public boolean queuesBefore(Order order) {
        if (order.getSide() == Side.BUY) {
            return stopPrice > order.getPrice();
        } else {
            return stopPrice < order.getPrice();
        }
    }


    public Order activate() {
        return new Order(this.orderId, this.security, this.side, this.quantity, this.price, this.broker, this.shareholder);
    }

    @Override //TODO : Furthur consideration required
    public void updateFromRequest(EnterOrderRq updateOrderRq) {
        super.updateFromRequest(updateOrderRq);
        stopPrice = updateOrderRq.getStopPrice();
    }





}
