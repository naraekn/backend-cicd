package com.naraekn.cicd.controllers;

import com.naraekn.cicd.domain.Product;
import com.naraekn.cicd.services.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<Product> getProduct() {
        return productService.getProducts();
    }

    @PostMapping
    public Product createProduct(@RequestBody String name) {
        return productService.createProduct(name);
    }
}
