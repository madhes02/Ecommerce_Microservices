package com.deloitte.order_service;

import com.deloitte.order_service.dto.request.OrderItemRequest;
import com.deloitte.order_service.dto.request.OrderRequestDTO;
import com.deloitte.order_service.dto.response.OrderResponseDTO;
import com.deloitte.order_service.entity.Order;
import com.deloitte.order_service.feign.ProductFeignClient;
import com.deloitte.order_service.feign.ProductResponse;
import com.deloitte.order_service.repository.OrderRepository;
import com.deloitte.order_service.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceApplicationTests {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductFeignClient productFeignClient;

    @InjectMocks
    private OrderService orderService;

    @Test
    void placeOrder_success() {
        // ARRANGE
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(2);

        OrderRequestDTO request = new OrderRequestDTO();
        request.setItems(List.of(item));

        // fake product-service response
        ProductResponse product = new ProductResponse();
        product.setId(1L);
        product.setName("Laptop");
        product.setPrice(new BigDecimal("75000.00"));
        product.setStock(10);

        when(productFeignClient.getProductById(1L)).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        // ACT
        OrderResponseDTO result = orderService.placeOrder(request, "madhes");

        // ASSERT
        assertNotNull(result);
        assertEquals("madhes", result.getUsername());
        assertEquals(new BigDecimal("150000.00"), result.getTotalPrice());
        assertEquals("PENDING", result.getStatus());
        verify(productFeignClient, times(1)).reduceStock(1L, 2);
    }

    @Test
    void placeOrder_throws_when_insufficient_stock() {
        // ARRANGE
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(20); // requesting 20

        OrderRequestDTO request = new OrderRequestDTO();
        request.setItems(List.of(item));

        ProductResponse product = new ProductResponse();
        product.setId(1L);
        product.setName("Laptop");
        product.setPrice(new BigDecimal("75000.00"));
        product.setStock(5); // only 5 available

        when(productFeignClient.getProductById(1L)).thenReturn(product);

        // ACT + ASSERT
        assertThrows(IllegalArgumentException.class,
                () -> orderService.placeOrder(request, "madhes"));

        // order must NOT be saved if stock check fails
        verify(orderRepository, never()).save(any(Order.class));
    }
}