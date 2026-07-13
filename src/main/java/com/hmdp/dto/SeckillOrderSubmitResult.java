package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderSubmitResult {

    private Long orderId;

    private Integer statusCode;

    private String statusText;
}
