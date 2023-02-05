package ru.megashop.orderservice.service;

import brave.Span;
import brave.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import ru.megashop.orderservice.dto.InventoryResponse;
import ru.megashop.orderservice.dto.OrderLineItemsDto;
import ru.megashop.orderservice.dto.OrderRequest;
import ru.megashop.orderservice.event.OrderPlacedEvent;
import ru.megashop.orderservice.model.Order;
import ru.megashop.orderservice.model.OrderLineItems;
import ru.megashop.orderservice.repository.OrderRepository;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final Tracer tracer;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public String placeOrder(OrderRequest orderRequest) {

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList().stream()
                .map(this::mapToDto)
                .toList();

        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        Span inventoryServiceLookup  = tracer.nextSpan().name("InventoryServiceLookup");

        try (Tracer.SpanInScope isLookup = tracer.withSpanInScope(inventoryServiceLookup.start())) {

            inventoryServiceLookup.tag("call", "inventory-service");

            InventoryResponse[] inventoryResponseArray = webClientBuilder
                    .clientConnector(new ReactorClientHttpConnector(HttpClient
                            .create()
                            .responseTimeout(Duration.ofMillis(4000))))
                    .build()
                    .get()
                    .uri("http://inventory-service/api/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();

            boolean allProductsInStock = Arrays.stream(inventoryResponseArray)
                    .allMatch(InventoryResponse::isInStock);

            if (allProductsInStock) {
                orderRepository.save(order);
                kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
                return "Order placed successfully";
            } else {
                throw new IllegalArgumentException("Product is not in stock, please try again later");
            }

        } finally {
            inventoryServiceLookup.flush();
        }

    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        return OrderLineItems.builder()
                .price(orderLineItemsDto.getPrice())
                .quantity(orderLineItemsDto.getQuantity())
                .skuCode(orderLineItemsDto.getSkuCode())
                .build();
    }

}