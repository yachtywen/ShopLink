package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_mq_message")
public class MqMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String messageId;

    private Long orderId;

    private Long userId;

    private Long voucherId;

    private String topic;

    private String messageBody;

    private Integer status;

    private Integer retryCount;

    private Integer maxRetryCount;

    private LocalDateTime nextRetryTime;

    private String lastError;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
