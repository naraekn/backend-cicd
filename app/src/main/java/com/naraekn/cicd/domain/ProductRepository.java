package com.naraekn.cicd.domain;

import java.util.List;

public interface ProductRepository {
    List<Product> findAll();

    Product save(Product product);
}
