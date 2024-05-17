package ir.ramtung.tinyme.domain;

import ir.ramtung.tinyme.domain.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderBookTest {
    private Security security;
    private List<Order> orders;
    private Broker broker;
    private Shareholder shareholder;
    @BeforeEach
    void setupOrderBook() {
        security = Security.builder().build();
        broker = Broker.builder().build();
        shareholder = Shareholder.builder().build();
        shareholder.incPosition(security, 100_000);
        orders = Arrays.asList(
                new Order(1, security, Side.BUY, 304, 15700, broker, shareholder),
                new Order(2, security, Side.BUY, 43, 15500, broker, shareholder),
                new Order(3, security, Side.BUY, 445, 15450, broker, shareholder),
                new Order(4, security, Side.BUY, 526, 15450, broker, shareholder),
                new Order(5, security, Side.BUY, 1000, 15400, broker, shareholder),
                new Order(6, security, Side.SELL, 350, 15800, broker, shareholder),
                new Order(7, security, Side.SELL, 285, 15810, broker, shareholder),
                new Order(8, security, Side.SELL, 800, 15810, broker, shareholder),
                new Order(9, security, Side.SELL, 340, 15820, broker, shareholder),
                new Order(10, security, Side.SELL, 65, 15820, broker, shareholder)
        );
        orders.forEach(order -> security.getOrderBook().enqueue(order));
    }

    @Test
    void finds_the_first_order_by_id() {
        assertThat(security.getOrderBook().findByOrderId(Side.BUY, 1))
                .isEqualTo(orders.get(0));
    }

    @Test
    void fails_to_find_the_first_order_by_id_in_the_wrong_queue() {
        assertThat(security.getOrderBook().findByOrderId(Side.SELL, 1)).isNull();
    }

    @Test
    void finds_some_order_in_the_middle_by_id() {
        assertThat(security.getOrderBook().findByOrderId(Side.BUY, 3))
                .isEqualTo(orders.get(2));
    }

    @Test
    void finds_the_last_order_by_id() {
        assertThat(security.getOrderBook().findByOrderId(Side.SELL, 10))
                .isEqualTo(orders.get(9));
    }

    @Test
    void removes_the_first_order_by_id() {
        OrderBook orderBook = security.getOrderBook();
        orderBook.removeByOrderId(Side.BUY, 1);
        assertThat(orderBook.getBuyQueue()).isEqualTo(orders.subList(1, 5));
    }

    @Test
    void fails_to_remove_the_first_order_by_id_in_the_wrong_queue() {
        OrderBook orderBook = security.getOrderBook();
        orderBook.removeByOrderId(Side.SELL, 1);
        assertThat(orderBook.getBuyQueue()).isEqualTo(orders.subList(0, 5));
    }

    @Test
    void removes_the_last_order_by_id() {
        OrderBook orderBook = security.getOrderBook();
        orderBook.removeByOrderId(Side.SELL, 10);
        assertThat(orderBook.getSellQueue()).isEqualTo(orders.subList(5, 9));
    }

    @Test
    void someOrdersAreExist_calculateOpeningPriceCalled_returnCorrectPrice(){
        security.getOrderBook().enqueue(new Order(1, security, Side.BUY, 1500, 15425, broker, shareholder));
        security.getOrderBook().enqueue(new Order(1, security, Side.SELL, 2000, 15420, broker, shareholder));

        assertThat(security.getOrderBook().calculateOpeningPrice(100000000)).isEqualTo(15425);
    }

    @Test
    void someOrdersAreExistSameQuantity_calculateOpeningPriceCalled_returnCorrectPrice(){
        Broker broker = Broker.builder().build();
        Shareholder shareholder = Shareholder.builder().build();
        shareholder.incPosition(security, 100_000);
        orders = Arrays.asList(
                new Order(1, security, Side.BUY, 10, 4, broker, shareholder),
                new Order(2, security, Side.BUY, 10, 3, broker, shareholder),
                new Order(3, security, Side.BUY, 10, 1, broker, shareholder),

                new Order(8, security, Side.SELL, 10, 6, broker, shareholder),
                new Order(9, security, Side.SELL, 10, 5, broker, shareholder),
                new Order(10, security, Side.SELL, 10, 4, broker, shareholder)
        );
        OrderBook mockedOrderBook = new OrderBook();
        orders.forEach(mockedOrderBook::enqueue);

        assertThat(mockedOrderBook.calculateOpeningPrice(3)).isEqualTo(4);
    }
    @Test
    void weAreInAuctionMode_TwoOrderHasSameTradeAbleQuantity_nearestToLastTradePriceIsFinalOpeningPrice(){
        security.getOrderBook().getBuyQueue().clear();
        security.getOrderBook().getSellQueue().clear();

        orders = Arrays.asList(
                new Order(1, security, Side.BUY, 100, 1000, broker, shareholder),
                new Order(2, security, Side.SELL, 100, 2000, broker, shareholder)
        );
        orders.forEach(order -> security.getOrderBook().enqueue(order));

        assertThat(security.getOrderBook().calculateOpeningPrice(1500)).isEqualTo(1500);

    }

}