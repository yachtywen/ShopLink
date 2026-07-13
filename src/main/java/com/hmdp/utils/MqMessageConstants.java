package com.hmdp.utils;

public class MqMessageConstants {

    public static final int MQ_MESSAGE_STATUS_SENDING = 0;
    public static final int MQ_MESSAGE_STATUS_SENT = 1;
    public static final int MQ_MESSAGE_STATUS_FAILED = 2;

    public static final int MQ_MESSAGE_DEFAULT_MAX_RETRY_COUNT = 5;
    public static final long MQ_MESSAGE_INITIAL_RETRY_DELAY_SECONDS = 5L;

    private MqMessageConstants() {
    }
}
