package ir.ramtung.tinyme.domain.service;

import ir.ramtung.tinyme.domain.entity.MatchingOutcome;
import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.domain.entity.Trade;

public interface MatchingControl {
    default MatchingOutcome canStartMatching(Order order){ return MatchingOutcome.EXECUTED; }
    default MatchingOutcome canTrade(Order newOrder, Trade trade) { return MatchingOutcome.EXECUTED; }

}
