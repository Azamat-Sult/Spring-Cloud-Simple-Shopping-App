package ru.megashop.orderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.megashop.orderservice.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}