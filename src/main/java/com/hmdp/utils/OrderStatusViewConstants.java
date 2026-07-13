package com.hmdp.utils;

public class OrderStatusViewConstants {

    public static final int VIEW_STATUS_QUEUING = 10;
    public static final int VIEW_STATUS_PROCESSING = 11;
    public static final int VIEW_STATUS_PENDING_PAY = 12;
    public static final int VIEW_STATUS_PAID = 13;
    public static final int VIEW_STATUS_CANCELLED = 14;
    public static final int VIEW_STATUS_FAILED = 15;

    public static final String VIEW_TEXT_QUEUING = "排队中";
    public static final String VIEW_TEXT_PROCESSING = "处理中";
    public static final String VIEW_TEXT_PENDING_PAY = "待支付";
    public static final String VIEW_TEXT_PAID = "支付成功";
    public static final String VIEW_TEXT_CANCELLED = "已取消";
    public static final String VIEW_TEXT_FAILED = "处理失败";

    private OrderStatusViewConstants() {
    }
}
