package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.ProductRequest;
import com.ecommerce.productservice.dto.ProductResponse;

import java.util.List;

public interface ProductService {
  ProductResponse createProduct(ProductRequest productRequest);
  List<ProductResponse> getAllProducts();
  ProductResponse getProductById(String id);
  ProductResponse updateProduct(String id, ProductRequest productRequest);
  void deleteProduct(String id);
}
