package ir.ramtung.tinyme.messaging;

public class Message {
    public static final String INVALID_ORDER_ID = "Invalid order ID";
    public static final String ORDER_QUANTITY_NOT_POSITIVE = "Order quantity is not-positive";
    public static final String ORDER_PRICE_NOT_POSITIVE = "Order price is not-positive";
    public static final String UNKNOWN_SECURITY_ISIN = "Unknown security ISIN";
    public static final String ORDER_ID_NOT_FOUND = "Order ID not found in the order book";
    public static final String INVALID_PEAK_SIZE = "Iceberg order peak size is out of range";
    public static final String CANNOT_SPECIFY_PEAK_SIZE_FOR_A_NON_ICEBERG_ORDER = "Cannot specify peak size for a non-iceberg order";
    public static final String UNKNOWN_BROKER_ID = "Unknown broker ID";
    public static final String UNKNOWN_SHAREHOLDER_ID = "Unknown shareholder ID";
    public static final String BUYER_HAS_NOT_ENOUGH_CREDIT = "Buyer has not enough credit";
    public static final String QUANTITY_NOT_MULTIPLE_OF_LOT_SIZE = "Quantity is not a multiple of security lot size";
    public static final String PRICE_NOT_MULTIPLE_OF_TICK_SIZE = "Price is not a multiple of security tick size";
    public static final String SELLER_HAS_NOT_ENOUGH_POSITIONS = "Seller has not enough positions";
    public static final String ORDER_MEQ_IS_NOT_POSITIVE = "Order Minimum Execution Quantity is not-positive";
    public static final String ORDER_MEQ_IS_BIGGER_THAN_QUANTITY = "Order Minimum Execution Quantity is not valid (Bigger than Order Quantity)";
    public static final String ORDER_UPDATE_MEQ_NOT_ZERO = "Order Update has a non-zero MEQ value";
    public static final String ORDER_FAILED_TO_REACH_MEQ = "Order failed to reach minimum execution quantity";
    public static final String STOP_LIMIT_ORDER_MEQ_NOT_ZERO = "Stop Limit Order has a non-zero MEQ value";
    public static final String STOP_PRICE_NOT_POSITIVE = "Stop price is not-positive";
    public static final String STOP_LIMIT_ORDER_PEAK_SIZE_NOT_ZERO = "Stop Limit Order has a non-zero peak size value";
    public static final String CANNOT_SPECIFY_STOP_PRICE_FOR_A_NON_STOP_LIMIT_ORDER = "Cannot specify stop price for a non-stop limit order";
    public static final String CANNOT_USE_AUCTION_MATCHING_WITH_STOP_PRICE = "Cannot use Auction Maching with Stop Price";
    public static final String CANNOT_USE_AUCTION_MATCHING_WITH_MEQ = "Cannot use Auction Matching with Minimum Execution Quantity" ;

}
