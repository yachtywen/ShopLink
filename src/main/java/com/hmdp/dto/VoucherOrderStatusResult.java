package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherOrderStatusResult {

    private Long orderId;

    private Integer statusCode;

    private String statusText;

    private LocalDateTime payTime;

    private LocalDateTime createTime;
}
