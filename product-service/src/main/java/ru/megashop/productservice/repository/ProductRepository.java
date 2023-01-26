package ru.megashop.productservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.megashop.productservice.model.Product;

public interface ProductRepository extends MongoRepository<Product, String> {
}