package com.deloitte.order_service.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    private Long id;
    private Long productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

}
