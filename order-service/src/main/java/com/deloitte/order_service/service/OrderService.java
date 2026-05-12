package com.deloitte.order_service.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.deloitte.order_service.dto.request.OrderItemRequest;
import com.deloitte.order_service.dto.request.OrderRequestDTO;
import com.deloitte.order_service.dto.response.OrderItemResponse;
import com.deloitte.order_service.dto.response.OrderResponseDTO;
import com.deloitte.order_service.entity.Order;
import com.deloitte.order_service.entity.OrderItem;
import com.deloitte.order_service.entity.OrderStatus;
import com.deloitte.order_service.feign.ProductResponse;
import com.deloitte.order_service.repository.OrderRepository;
import com.deloitte.order_service.feign.ProductFeignClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductFeignClient productFeignClient;

    public OrderResponseDTO placeOrder(OrderRequestDTO requestDTO, String username) {

        Order order = new Order();
        order.setUsername(username);
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalPrice = BigDecimal.ZERO;
        List<OrderItem> itemsToSave = new ArrayList<>();

        for (OrderItemRequest itemRequest : requestDTO.getItems()) {
            ProductResponse product = productFeignClient.getProductById(itemRequest.getProductId());

            if (product.getStock() < itemRequest.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product: " + product.getName()
                                + ". Avalaible: " + product.getStock()
                                + ", Requested" + itemRequest.getQuantity());
            }

            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(itemRequest.getProductId());
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(product.getPrice());
            item.setSubtotal(subtotal);

            itemsToSave.add(item);
            totalPrice = totalPrice.add(subtotal);
        }

        order.setItems(itemsToSave);
        order.setTotalPrice(totalPrice);

        Order saved = orderRepository.save(order);

        for (OrderItemRequest itemRequest : requestDTO.getItems()) {
            productFeignClient.reduceStock(
                    itemRequest.getProductId(),
                    itemRequest.getQuantity());
        }

        return mapToDTO(saved);
    }

    private OrderResponseDTO mapToDTO(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()))
                .collect(Collectors.toList());

        return new OrderResponseDTO(
                order.getId(),
                order.getUsername(),
                itemResponses,
                order.getTotalPrice(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

}
