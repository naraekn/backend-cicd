package com.naraekn.cicd.infra;

import com.naraekn.cicd.domain.Product;
import com.naraekn.cicd.domain.ProductRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface JpaProductRepository
        extends ProductRepository, CrudRepository<Product, Long> {
    List<Product> findAll();

    Product save(Product product);
}
