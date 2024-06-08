package ir.ramtung.tinyme.messaging.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.domain.entity.Side;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor(force = true)
public class EnterOrderRq {
    private OrderEntryType requestType;
    private long requestId;
    private String securityIsin;
    private long orderId;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime entryTime;
    private Side side;
    private int quantity;
    private int price;
    private long brokerId;
    private long shareholderId;
    private int peakSize;
    private final int minimumExecutionQuantity;
    private int stopPrice;

    private EnterOrderRq(OrderEntryType orderEntryType, long requestId, String securityIsin, long orderId, LocalDateTime entryTime, Side side, int quantity, int price, long brokerId, long shareholderId, int peakSize, int minimumExecutionQuantity) {
        this.requestType = orderEntryType;
        this.requestId = requestId;
        this.securityIsin = securityIsin;
        this.orderId = orderId;
        this.entryTime = entryTime;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.brokerId = brokerId;
        this.shareholderId = shareholderId;
        this.peakSize = peakSize;
        this.minimumExecutionQuantity = minimumExecutionQuantity;
        this.stopPrice = 0;
    }

    private EnterOrderRq(OrderEntryType orderEntryType, long requestId, String securityIsin, long orderId, LocalDateTime entryTime, Side side, int quantity, int price, long brokerId, long shareholderId, int peakSize, int minimumExecutionQuantity, int stopPrice) {
        this.requestType = orderEntryType;
        this.requestId = requestId;
        this.securityIsin = securityIsin;
        this.orderId = orderId;
        this.entryTime = entryTime;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.brokerId = brokerId;
        this.shareholderId = shareholderId;
        this.peakSize = peakSize;
        this.minimumExecutionQuantity = minimumExecutionQuantity;
        this.stopPrice = stopPrice;
    }
    public EnterOrderRq(OrderEntryType orderEntryType, Order order) {
        this.requestType = orderEntryType;
        this.securityIsin = order.getSecurity().getIsin();
        this.orderId = order.getOrderId();
        this.entryTime = order.getEntryTime();
        this.side = order.getSide();
        this.quantity = order.getQuantity();
        this.price = order.getPrice();
        this.brokerId = order.getBroker().getBrokerId();
        this.shareholderId = order.getShareholder().getShareholderId();
        this.minimumExecutionQuantity = 0;
        this.stopPrice = 0;
    }


    public static EnterOrderRq createNewOrderRq(long requestId, String securityIsin, long orderId, LocalDateTime entryTime, Side side, int quantity, int price, long brokerId, long shareholderId, int peakSize, int minimumExecutionQuantity) {
        return new EnterOrderRq(OrderEntryType.NEW_ORDER, requestId, securityIsin, orderId, entryTime, side, quantity, price, brokerId, shareholderId, peakSize, minimumExecutionQuantity);
    }

    public static EnterOrderRq createNewOrderRq(long requestId, String securityIsin, long orderId, LocalDateTime entryTime, Side side, int quantity, int price, long brokerId, long shareholderId, int peakSize, int minimumExecutionQuantity, int stopPrice) {
        return new EnterOrderRq(OrderEntryType.NEW_ORDER, requestId, securityIsin, orderId, entryTime, side, quantity, price, brokerId, shareholderId, peakSize, minimumExecutionQuantity, stopPrice);
    }



    public static EnterOrderRq createUpdateOrderRq(long requestId, String securityIsin, long orderId, LocalDateTime entryTime, Side side, int quantity, int price, long brokerId, long shareholderId, int peakSize, int minimumExecutionQuantity) {
        return new EnterOrderRq(OrderEntryType.UPDATE_ORDER, requestId, securityIsin, orderId, entryTime, side, quantity, price, brokerId, shareholderId, peakSize, minimumExecutionQuantity);
    }
    public static EnterOrderRq createUpdateOrderRq(long requestId, String securityIsin, long orderId, LocalDateTime entryTime, Side side, int quantity, int price, long brokerId, long shareholderId, int peakSize, int minimumExecutionQuantity, int stopPrice) {
        return new EnterOrderRq(OrderEntryType.UPDATE_ORDER, requestId, securityIsin, orderId, entryTime, side, quantity, price, brokerId, shareholderId, peakSize, minimumExecutionQuantity, stopPrice);
    }

}
