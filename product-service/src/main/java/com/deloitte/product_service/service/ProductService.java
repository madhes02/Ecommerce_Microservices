package com.deloitte.product_service.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.deloitte.product_service.dto.request.ProductRequestDTO;
import com.deloitte.product_service.dto.response.ProductResponseDTO;
import com.deloitte.product_service.entity.Product;
import com.deloitte.product_service.exception.ResourceNotFoundException;
import com.deloitte.product_service.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository productRepository;

    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());
        Product savedProduct = productRepository.save(product);
        logger.info("Created product with ID: {}", savedProduct.getId());
        return mapToDTO(savedProduct);
    }

    public ProductResponseDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        logger.info("Retrieved product with ID: {}", id);
        return mapToDTO(product);
    }

    public List<ProductResponseDTO> getAllProducts() {
         return productRepository.findAll().stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<ProductResponseDTO> getProductByCategory(String category) {
        return productRepository.findByCategory(category).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());
        Product updatedProduct = productRepository.save(product);
        logger.info("Updated product with ID: {}", id);
        return mapToDTO(updatedProduct);
    }

    public String deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        productRepository.delete(product);
        logger.info("Deleted product with ID: {}", id);
        return "Product deleted successfully";
    }

    public void reduceStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
            if (product.getStock() < quantity) {
                throw new RuntimeException("Insufficient stock for product ID: " + productId);
            }
            product.setStock(product.getStock() - quantity);
            productRepository.save(product);
            logger.info("Reduced stock for product ID: {} by quantity: {}", productId, quantity);
    }

    private ProductResponseDTO mapToDTO(Product product) {
        return new ProductResponseDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getCategory(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
