package ir.ramtung.tinyme.domain.service;

import ir.ramtung.tinyme.domain.entity.MatchingOutcome;
import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.domain.entity.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MatchingControlList {
    @Autowired
    private List<MatchingControl> controlList;

    public void tradeAccepted(Order newOrder, Trade trade) {
        for (MatchingControl control : controlList) {
            control.tradeAccepted(newOrder, trade);
        }
    }

    public MatchingOutcome canTrade(Order newOrder, Trade trade) { return MatchingOutcome.EXECUTED; }
}
