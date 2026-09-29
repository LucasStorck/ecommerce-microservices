package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.ProductRequest;
import com.ecommerce.productservice.dto.ProductResponse;
import com.ecommerce.productservice.exception.ProductNotFoundException;
import com.ecommerce.productservice.mapper.ProductMapper;
import com.ecommerce.productservice.model.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

  private final ProductRepository productRepository;
  private final ProductMapper productMapper;

  public ProductServiceImpl(ProductRepository productRepository, ProductMapper productMapper) {
    this.productRepository = productRepository;
    this.productMapper = productMapper;
  }

  @Override
  public ProductResponse createProduct(ProductRequest productRequest) {
    Product product = productMapper.toEntity(productRequest);
    return productMapper.toResponse(productRepository.save(product));
  }

  @Override
  public List<ProductResponse> getAllProducts() {
    return productRepository.findAll().stream()
        .map(productMapper::toResponse)
        .toList();
  }

  @Override
  @Cacheable(value = "products", key = "#id")
  public ProductResponse getProductById(String id) {
    return productMapper.toResponse(findOrThrow(id));
  }

  @Override
  @CachePut(value = "products", key = "#id")
  public ProductResponse updateProduct(String id, ProductRequest productRequest) {
    Product product = findOrThrow(id);
    productMapper.updateEntity(productRequest, product);
    return productMapper.toResponse(productRepository.save(product));
  }

  @Override
  @CacheEvict(value = "products", key = "#id")
  public void deleteProduct(String id) {
    if (!productRepository.existsById(id)) {
      throw new ProductNotFoundException(id);
    }
    productRepository.deleteById(id);
  }

  private Product findOrThrow(String id) {
    return productRepository.findById(id)
        .orElseThrow(() -> new ProductNotFoundException(id));
  }
}
