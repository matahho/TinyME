package ir.ramtung.tinyme.domain.service;

import ir.ramtung.tinyme.domain.entity.*;
import org.springframework.stereotype.Component;

@Component
public class CreditControl implements MatchingControl{

    @Override
    public MatchingOutcome canTrade(Order newOrder, Trade trade){
        if((newOrder.getSide() == Side.SELL) || (newOrder.getSide() == Side.BUY && trade.buyerHasEnoughCredit() && newOrder.getStatus() != OrderStatus.ACTIVATED))
            return MatchingOutcome.EXECUTED;
        else return MatchingOutcome.NOT_ENOUGH_CREDIT;
    }

    @Override
    public void tradeAccepted(Order newOrder, Trade trade) {
        if (newOrder.getSide() == Side.BUY)
            trade.decreaseBuyersCredit();
        trade.increaseSellersCredit();
    }
}
