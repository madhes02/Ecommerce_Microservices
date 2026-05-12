package com.deloitte.order_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderItemRequest {

    @NotNull(message="Product id is required")
    private Long productId;

    @NotNull(message="Quantity must be atleast 1")
    private Integer quantity;


}
