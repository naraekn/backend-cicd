package com.naraekn.cicd.domain;

import lombok.Getter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Product {
    @Id
    @GeneratedValue
    private Long id;
    @Getter
    private String name;

    public Product() {}

    public Product(String name) {
        this.name = name;
    }
}
