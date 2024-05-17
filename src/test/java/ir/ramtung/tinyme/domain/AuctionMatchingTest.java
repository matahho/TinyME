package ir.ramtung.tinyme.domain;

import ir.ramtung.tinyme.config.MockedJMSTestConfig;
import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.domain.service.Matcher;
import ir.ramtung.tinyme.domain.service.OrderHandler;
import ir.ramtung.tinyme.messaging.EventPublisher;
import ir.ramtung.tinyme.messaging.Message;
import ir.ramtung.tinyme.messaging.TradeDTO;
import ir.ramtung.tinyme.messaging.event.*;
import ir.ramtung.tinyme.messaging.request.*;
import ir.ramtung.tinyme.repository.BrokerRepository;
import ir.ramtung.tinyme.repository.SecurityRepository;
import ir.ramtung.tinyme.repository.ShareholderRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.platform.engine.support.discovery.SelectorResolver;
import org.springframework.beans.factory.annotation.Autowired;
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


    @BeforeEach
    void setupOrderBook() {
        securityRepository.clear();
        brokerRepository.clear();
        shareholderRepository.clear();

        security = Security.builder().isin("ABC").matchingState(MatchingState.AUCTION).build();
        securityRepository.addSecurity(security);

        shareholder = Shareholder.builder().shareholderId(1).build();
        shareholder.incPosition(security, 100_000);
        shareholderRepository.addShareholder(shareholder);

        broker1 = Broker.builder().brokerId(0).credit(100_000_000L).build();
        broker2 = Broker.builder().brokerId(1).credit(100_000_000L).build();
        brokerRepository.addBroker(broker1);
        brokerRepository.addBroker(broker2);


        List<Order> orders = Arrays.asList(
                new Order(1, security, Side.BUY, 304, 15700, broker1, shareholder),
                new Order(2, security, Side.BUY, 43, 15500, broker1, shareholder),
                new Order(3, security, Side.BUY, 445, 15450, broker1, shareholder),
                new Order(4, security, Side.BUY, 526, 15450, broker1, shareholder),
                new Order(5, security, Side.BUY, 1000, 15400, broker1, shareholder),

                new Order(6, security, Side.SELL, 350, 15800, broker2, shareholder),
                new Order(7, security, Side.SELL, 285, 15810, broker2, shareholder),
                new Order(8, security, Side.SELL, 800, 15810, broker2, shareholder),
                new Order(9, security, Side.SELL, 340, 15820, broker2, shareholder),
                new Order(10, security, Side.SELL, 65, 15820, broker2, shareholder)
        );
        orders.forEach(order -> security.getOrderBook().enqueue(order));

        security.updateMarketPrice(16000);
        security.updateOpeningPrice();
    }

    @Test
    void changeMatcherMode_SwitchBetweenContinuousAndAuction_ModeChanges() {
        ChangeMatchingStateRq changeMatchingStateRq = new ChangeMatchingStateRq("ABC", MatchingState.AUCTION);
        orderHandler.handleChangeMatchingStateRq(changeMatchingStateRq);

        assertThat(security.getMatchingState()).isEqualTo(MatchingState.AUCTION);
    }


    @Test
    void pricePriority_EnqueueOrdersWithDifferentPrices_HighestPriceOrderIsMatched() {
        Order lowPriceBuyOrder = new Order(100, security, Side.BUY, 100, 15000, broker1, shareholder);
        Order highPriceBuyOrder = new Order(101, security, Side.BUY, 100, 16000, broker1, shareholder);
        security.getOrderBook().enqueue(lowPriceBuyOrder);
        security.getOrderBook().enqueue(highPriceBuyOrder);

        // sell order that can match with higher price buy order
        Order incomingSellOrder = new Order(200, security, Side.SELL, 100, 15000, broker2, shareholder);
        security.getOrderBook().enqueue(incomingSellOrder);

        ChangeMatchingStateRq changeMatchingStateRq = new ChangeMatchingStateRq("ABC", MatchingState.CONTINUOUS);
        orderHandler.handleChangeMatchingStateRq(changeMatchingStateRq);


        ArgumentCaptor<TradeEvent> tradeEventCaptor = ArgumentCaptor.forClass(TradeEvent.class);
        verify(eventPublisher).publish(tradeEventCaptor.capture());
        List<TradeEvent> tradeEvents = tradeEventCaptor.getAllValues();
        assertThat(tradeEvents).hasSize(1);

        TradeEvent tradeEvent = tradeEvents.get(0);
        assertThat(tradeEvent.getSecurityIsin()).isEqualTo("ABC");
        assertThat(tradeEvent.getBuyId()).isEqualTo(101); // higher price buy order should be matched
        assertThat(tradeEvent.getSellId()).isEqualTo(200);
        assertThat(tradeEvent.getQuantity()).isEqualTo(100);
        assertThat(tradeEvent.getPrice()).isEqualTo(15820); //sell order price
    }

    @Disabled
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
    @Disabled
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
        //orderHandler.handleReopeningOperation();

        // to verify that trades are executed during reopening (OrderExecutedEvent)
        ArgumentCaptor<TradeEvent> tradeEventCaptor = ArgumentCaptor.forClass(TradeEvent.class);
        verify(eventPublisher).publish(tradeEventCaptor.capture());
        List<TradeEvent> tradeEvents = tradeEventCaptor.getAllValues();
        // assert to verify the trades
    }

    @Test
    void opening_price_causes_no_auction_matching(){
        orderHandler.handleChangeMatchingStateRq(new ChangeMatchingStateRq("ABC", MatchingState.AUCTION));

        ArgumentCaptor<Event> tradeEventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisher).publish(tradeEventCaptor.capture());
        List<Event> tradeEvents = tradeEventCaptor.getAllValues();
        assertThat(tradeEvents).size().isEqualTo(1);
    }

    @Test
    void check_opening_price_and_tradable_quantity(){
        Order matchingBuyOrder = new Order(100, security, Side.BUY, 1000, 15500, broker1, shareholder);
        Order incomingSellOrder = new Order(200, security, Side.SELL, 300, 15450, broker2, shareholder);
        orderHandler.handleEnterOrder(new EnterOrderRq(OrderEntryType.NEW_ORDER, matchingBuyOrder));
        orderHandler.handleEnterOrder(new EnterOrderRq(OrderEntryType.NEW_ORDER, incomingSellOrder));

        assertThat(security.getOpeningPrice()).isEqualTo(15700);
        assertThat(security.getTradableQuantity()).isEqualTo(300);
    }

    @Test
    void auctionModeActivated_MEQOrderEntered_OrderShouldReject() {
        int minimumOrderQuantity = 100;

        // Given
        ChangeMatchingStateRq changeMatchingStateRq = new ChangeMatchingStateRq("ABC", MatchingState.AUCTION);
        orderHandler.handleChangeMatchingStateRq(changeMatchingStateRq);

        // When : MEQ order entered
        Order matchingBuyOrder = new Order(170, security, Side.BUY, 48, 15700, broker1, shareholder); // less than min amount
        EnterOrderRq enterOrderRq = EnterOrderRq.createNewOrderRq(1,
                matchingBuyOrder.getSecurity().getIsin(),
                matchingBuyOrder.getOrderId(),
                matchingBuyOrder.getEntryTime(),
                matchingBuyOrder.getSide(),
                matchingBuyOrder.getTotalQuantity(),
                matchingBuyOrder.getPrice(),
                matchingBuyOrder.getBroker().getBrokerId(),
                matchingBuyOrder.getShareholder().getShareholderId(),
                0,
                5);

        orderHandler.handleEnterOrder(enterOrderRq);

        // MEQ can not entered to an auction mode
        ArgumentCaptor<OrderRejectedEvent> orderRejectedCaptor = ArgumentCaptor.forClass(OrderRejectedEvent.class);
        verify(eventPublisher).publish(orderRejectedCaptor.capture());
        OrderRejectedEvent outputEvent = orderRejectedCaptor.getValue();

        assertThat(outputEvent.getOrderId()).isEqualTo(matchingBuyOrder.getOrderId());
        assertThat(outputEvent.getErrors()).containsOnly( Message.CANNOT_USE_AUCTION_MATCHING_WITH_MEQ);
        assertThat(security.getOrderBook().getBuyQueue()).doesNotContain(matchingBuyOrder);
    }
}
