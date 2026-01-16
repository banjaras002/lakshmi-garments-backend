package com.lakshmigarments.service;

import com.lakshmigarments.dto.ItemRequestDTO;
import com.lakshmigarments.dto.response.BatchItemResponse;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ItemService {

    @Transactional
    BatchItemResponse createItem(ItemRequestDTO item);

    @Transactional
    BatchItemResponse updateItem(Long id, ItemRequestDTO itemRequestDTO);

    @Transactional
    boolean deleteItem(Long id);

    List<BatchItemResponse> getAllItems(String search);
}
