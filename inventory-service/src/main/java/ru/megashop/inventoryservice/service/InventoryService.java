package ru.megashop.inventoryservice.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.megashop.inventoryservice.dto.InventoryResponse;
import ru.megashop.inventoryservice.repository.InventoryRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @SneakyThrows // <- BAD PRACTICE!!!
    @Transactional(readOnly = true)
    public List<InventoryResponse> isInStock(List<String> skuCode) {

//        log.info("Wait started");
//        Thread.sleep(10000);
//        log.info("Wait ended");

        return inventoryRepository.findBySkuCodeIn(skuCode).stream()
                .map(inventory -> InventoryResponse.builder()
                        .skuCode(inventory.getSkuCode())
                        .isInStock(inventory.getQuantity() > 0)
                        .build())
                .toList();
    }

}