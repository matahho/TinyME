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

    public MatchingOutcome canStartMatching(Order order){
        for (MatchingControl control : controlList){
            MatchingOutcome outcome = control.canStartMatching(order);
            if(outcome != MatchingOutcome.EXECUTED)
                return outcome;
        }
        return MatchingOutcome.EXECUTED;
    }

    public MatchingOutcome canTrade(Order newOrder, Trade trade) { return MatchingOutcome.EXECUTED; }
}
