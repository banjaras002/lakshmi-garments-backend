package com.lakshmigarments.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lakshmigarments.dto.response.BatchItemResponse;
import com.lakshmigarments.exception.BatchItemNotFoundException;
import com.lakshmigarments.model.BatchItem;
import com.lakshmigarments.model.DamageType;
import com.lakshmigarments.model.JobworkReceipt;
import com.lakshmigarments.model.JobworkType;
import com.lakshmigarments.service.BatchItemService;
import com.lakshmigarments.repository.BatchItemRepository;
import com.lakshmigarments.repository.DamageRepository;
import com.lakshmigarments.repository.JobworkReceiptRepository;
import com.lakshmigarments.repository.JobworkRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BatchItemServiceImpl implements BatchItemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchItemServiceImpl.class);
    private final BatchItemRepository batchItemRepository;
    private final JobworkReceiptRepository receiptRepository;
    private final JobworkRepository jobworkRepository;
    private final DamageRepository damageRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<BatchItemResponse> getBatchItemsByBatchSerial(String serialCode, String jobworkType) {
        LOGGER.debug("Fetching items by batch serial: {}", serialCode);
        List<BatchItem> batchItems = batchItemRepository.findByBatchSerialCode(serialCode);
        
        for (BatchItem batchItem : batchItems) {
			Long totalBatchItemQuantity = batchItem.getQuantity();
			Long totalAssignedQuantity = jobworkRepository.getAssignedQuantities(serialCode, 
					jobworkType, batchItem.getItem().getName());
			Long totalRepairableDamagesForItem = damageRepository.getDamagedQuantity(serialCode, 
					DamageType.REPAIRABLE.name(), jobworkType, batchItem.getItem().getName());
			LOGGER.debug("Batch item {} {} quantities : original quantity {}, repairable damages {}, assigned quantity {}", 
					serialCode, batchItem.getItem().getName(), totalBatchItemQuantity, totalRepairableDamagesForItem, totalAssignedQuantity);
		}
        
        List<JobworkReceipt> receipts = receiptRepository
        		.findByJobworkBatchSerialCodeAndJobworkJobworkType(serialCode, JobworkType.valueOf(jobworkType));
        LOGGER.debug("Fetched {} jobworks for batch {} and jobwork type {}", receipts.size(),serialCode, jobworkType);
        
        return batchItems.stream().map(batchItem -> modelMapper.map(batchItem, BatchItemResponse.class))
                .collect(Collectors.toList());
    }
    
    private BatchItem getBatchItemOrThrow(String serialCode, String itemName) {
		return batchItemRepository.findByBatchSerialCodeAndItemName(serialCode, itemName).orElseThrow(() -> {
			LOGGER.error("Batch item not found: {} {}", serialCode, itemName);
			return new BatchItemNotFoundException("Batch item not found: " + serialCode + " " + itemName);
		});
	}

}
