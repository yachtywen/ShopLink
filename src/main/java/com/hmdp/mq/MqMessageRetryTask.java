package com.hmdp.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class MqMessageRetryTask {

    @Resource
    private SeckillOrderMessageService seckillOrderMessageService;

    @Scheduled(fixedDelayString = "${hmdp.mq.retry-fixed-delay:5000}")
    public void retryPendingMessages() {
        try {
            seckillOrderMessageService.retryPendingMessages();
        } catch (Exception e) {
            log.error("扫描补偿秒杀订单消息失败", e);
        }
    }
}
