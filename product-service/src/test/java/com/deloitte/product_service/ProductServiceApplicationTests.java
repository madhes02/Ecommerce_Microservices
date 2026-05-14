package com.deloitte.product_service;

import com.deloitte.product_service.dto.request.ProductRequestDTO;
import com.deloitte.product_service.dto.response.ProductResponseDTO;
import com.deloitte.product_service.entity.Product;
import com.deloitte.product_service.exception.ResourceNotFoundException;
import com.deloitte.product_service.repository.ProductRepository;
import com.deloitte.product_service.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceApplicationTests {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProduct_success() {
        // ARRANGE
        ProductRequestDTO request = new ProductRequestDTO();
        request.setName("Laptop");
        request.setDescription("Gaming Laptop");
        request.setPrice(new BigDecimal("75000.00"));
        request.setStock(10);
        request.setCategory("Electronics");

        Product savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setName("Laptop");
        savedProduct.setPrice(new BigDecimal("75000.00"));
        savedProduct.setStock(10);
        savedProduct.setCategory("Electronics");

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // ACT
        ProductResponseDTO result = productService.createProduct(request);

        // ASSERT
        assertNotNull(result);
        assertEquals("Laptop", result.getName());
        assertEquals(new BigDecimal("75000.00"), result.getPrice());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void getProductById_throws_when_not_found() {
        // ARRANGE
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(ResourceNotFoundException.class,
                () -> productService.getProductById(99L));
    }

    @Test
    void reduceStock_throws_when_insufficient() {
        // ARRANGE
        Product product = new Product();
        product.setId(1L);
        product.setStock(2); // only 2 in stock

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // ACT + ASSERT — try to order 5, only 2 available
        assertThrows(IllegalArgumentException.class,
                () -> productService.reduceStock(1L, 5));
    }
}