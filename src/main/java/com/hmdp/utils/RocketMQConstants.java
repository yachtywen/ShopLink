package com.hmdp.utils;

public class RocketMQConstants {

    public static final String SECKILL_ORDER_TOPIC = "seckill-order-topic";
    public static final String SECKILL_ORDER_CONSUMER_GROUP = "hmdp-seckill-order-consumer";
    public static final String SECKILL_ORDER_DLQ_TOPIC = "%DLQ%" + SECKILL_ORDER_CONSUMER_GROUP;
    public static final String SECKILL_ORDER_DLQ_CONSUMER_GROUP = "hmdp-seckill-order-dlq-recorder";

    public static final String ORDER_TIMEOUT_TOPIC = "voucher-order-timeout-topic";
    public static final String ORDER_TIMEOUT_CONSUMER_GROUP = "hmdp-order-timeout-consumer";
    public static final String ORDER_TIMEOUT_DLQ_TOPIC = "%DLQ%" + ORDER_TIMEOUT_CONSUMER_GROUP;
    public static final String ORDER_TIMEOUT_DLQ_CONSUMER_GROUP = "hmdp-order-timeout-dlq-recorder";

    public static final int ORDER_TIMEOUT_DELAY_LEVEL = 9;
    public static final long SEND_TIMEOUT_MILLIS = 3000L;
    public static final int CONSUMER_MAX_RECONSUME_TIMES = 16;

    private RocketMQConstants() {
    }
}
