package com.naraekn.cicd.services;

import com.naraekn.cicd.domain.Product;
import com.naraekn.cicd.domain.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    public Product createProduct(String name) {
        return productRepository.save(new Product(name));
    }
}
