package com.deloitte.order_service.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderRequestDTO {

    @NotNull
    @Size(min= 1, message = "At least one item is required")
    private List<OrderItemRequest> items;

}
