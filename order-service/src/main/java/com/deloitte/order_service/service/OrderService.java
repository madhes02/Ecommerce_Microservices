package com.deloitte.order_service.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.deloitte.order_service.dto.request.OrderItemRequest;
import com.deloitte.order_service.dto.request.OrderRequestDTO;
import com.deloitte.order_service.dto.response.OrderItemResponse;
import com.deloitte.order_service.dto.response.OrderResponseDTO;
import com.deloitte.order_service.entity.Order;
import com.deloitte.order_service.entity.OrderItem;
import com.deloitte.order_service.entity.OrderStatus;
import com.deloitte.order_service.exception.ResourceNotFoundException;
import com.deloitte.order_service.feign.ProductResponse;
import com.deloitte.order_service.repository.OrderRepository;

import io.micrometer.common.lang.NonNull;

import com.deloitte.order_service.feign.ProductFeignClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

        private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductFeignClient productFeignClient;

    public OrderResponseDTO placeOrder(OrderRequestDTO requestDTO, String username) {

        logger.info("Placing order for user: {}", username);

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
        logger.info("Order saved. ID: {}, Total: {}", saved.getId(), totalPrice);

        for (OrderItemRequest itemRequest : requestDTO.getItems()) {
            productFeignClient.reduceStock(
                    itemRequest.getProductId(),
                    itemRequest.getQuantity());
        }

        return mapToDTO(saved);
    }

    public List<OrderResponseDTO> getMyOrders(String username) {
        return orderRepository.findByUsername(username).stream()
        .map(this::mapToDTO)
        .collect(Collectors.toList());
    }

    public OrderResponseDTO getOrderById(@NonNull Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + id
                ));
        return mapToDTO(order);          
    }


    public OrderResponseDTO updateOrderStatus(@NonNull Long id, String status) {
        Order order = orderRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(
                "Order not found with id: " + id
        ));

        order.setStatus(OrderStatus.valueOf(status.toUpperCase()));
        Order updated = orderRepository.save(order);
        logger.info("Order {} status updated to: {}", id, status);
        return mapToDTO(order);
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
