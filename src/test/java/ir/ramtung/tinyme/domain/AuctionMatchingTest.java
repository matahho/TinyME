package ir.ramtung.tinyme.domain;

import ir.ramtung.tinyme.config.MockedJMSTestConfig;
import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.domain.service.Matcher;
import ir.ramtung.tinyme.domain.service.OrderHandler;
import ir.ramtung.tinyme.messaging.EventPublisher;
import ir.ramtung.tinyme.messaging.Message;
import ir.ramtung.tinyme.messaging.TradeDTO;
import ir.ramtung.tinyme.messaging.event.*;
import ir.ramtung.tinyme.messaging.request.DeleteOrderRq;
import ir.ramtung.tinyme.messaging.request.EnterOrderRq;
import ir.ramtung.tinyme.repository.BrokerRepository;
import ir.ramtung.tinyme.repository.SecurityRepository;
import ir.ramtung.tinyme.repository.ShareholderRepository;
import ir.ramtung.tinyme.messaging.request.ChangeMatchingStateRq;
import org.springframework.beans.factory.annotation.Autowired;
import ir.ramtung.tinyme.messaging.request.MatchingState;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import static ir.ramtung.tinyme.domain.entity.Side.BUY;
import static ir.ramtung.tinyme.domain.entity.Side.SELL;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(MockedJMSTestConfig.class)
public class AuctionMatchingTest {

    @Autowired
    OrderHandler orderHandler;
    @Autowired
    EventPublisher eventPublisher;
    @Autowired
    SecurityRepository securityRepository;
    @Autowired
    BrokerRepository brokerRepository;
    @Autowired
    ShareholderRepository shareholderRepository;
    private Security security;
    private Shareholder shareholder;
    private Broker broker1;
    private Broker broker2;
    private Broker broker3;



    @Test
    void calculateAndAnnounceOpeningPriceTest() {
        //  orders in the queue
        Order matchingBuyOrder1 = new Order(100, security, Side.BUY, 300, 15500, broker1, shareholder);
        Order matchingBuyOrder2 = new Order(110, security, Side.BUY, 300, 15500, broker1, shareholder);
        Order incomingSellOrder = new Order(200, security, Side.SELL, 1000, 15450, broker2, shareholder);
        security.getOrderBook().enqueue(matchingBuyOrder1);
        security.getOrderBook().enqueue(matchingBuyOrder2);

        // send request to change matching state to AUCTION
        ChangeMatchingStateRq changeMatchingStateRq = new ChangeMatchingStateRq("ABC", MatchingState.AUCTION);
        orderHandler.handleChangeMatchingStateRq(changeMatchingStateRq);

        // verify that  opening price(bazgoshayi) is calculated and announced
        ArgumentCaptor<OpeningPriceEvent> openingPriceEventCaptor = ArgumentCaptor.forClass(OpeningPriceEvent.class);
        verify(eventPublisher).publish(openingPriceEventCaptor.capture());
        OpeningPriceEvent openingPriceEvent = openingPriceEventCaptor.getValue();
        assertThat(openingPriceEvent.getSecurityIsin()).isEqualTo("ABC");
        // add assert for the calculated opening price and tradable quantity

    }

    @Test
    void performReopeningOperationWithTradesTest() {
        //  orders in the queue
        Order matchingBuyOrder = new Order(100, security, Side.BUY, 1000, 15500, broker1, shareholder);
        Order incomingSellOrder = new Order(200, security, Side.SELL, 300, 15450, broker2, shareholder);
        security.getOrderBook().enqueue(matchingBuyOrder);

        // send request to change matching state to AUCTION
        ChangeMatchingStateRq changeMatchingStateRq = new ChangeMatchingStateRq("ABC", MatchingState.AUCTION);
        orderHandler.handleChangeMatchingStateRq(changeMatchingStateRq);

        // perform reopening operation
        orderHandler.handleReopeningOperation();

        // to verify that trades are executed during reopening (OrderExecutedEvent)
        ArgumentCaptor<TradeEvent> tradeEventCaptor = ArgumentCaptor.forClass(TradeEvent.class);
        verify(eventPublisher).publish(tradeEventCaptor.capture());
        List<TradeEvent> tradeEvents = tradeEventCaptor.getAllValues();
        // assert to verify the trades
    }




}
