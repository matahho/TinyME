package ir.ramtung.tinyme.domain.entity;

import lombok.Getter;

import java.io.InvalidObjectException;
import java.util.LinkedList;

@Getter
public class InactiveOrderBook extends OrderBook{
    public InactiveOrderBook(){
        super();
    }

    @Override
    public void enqueue(Order order) {
        if (order instanceof StopLimitOrder)
            super.enqueue(order);
        else
            throw new InvalidObjectException("Invalid object type. Expected StopLimitOrder.");

    }
}
