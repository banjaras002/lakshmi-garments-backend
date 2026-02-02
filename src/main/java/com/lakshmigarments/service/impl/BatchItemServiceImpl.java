package com.lakshmigarments.service.impl;

import java.util.ArrayList;
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
import com.lakshmigarments.repository.JobworkReceiptItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BatchItemServiceImpl implements BatchItemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchItemServiceImpl.class);
    private final BatchItemRepository batchItemRepository;
    private final JobworkReceiptRepository receiptRepository;
    private final JobworkRepository jobworkRepository;
    private final DamageRepository damageRepository;
    private final JobworkReceiptItemRepository receiptItemRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<BatchItemResponse> getBatchItemsByBatchSerial(String serialCode, String jobworkType) {
        LOGGER.debug("Fetching items by batch serial: {}", serialCode);
        List<BatchItem> batchItems = batchItemRepository.findByBatchSerialCode(serialCode);
        
        List<BatchItemResponse> batchItemResponses = new ArrayList<>();
        long i = 1;
        for (BatchItem batchItem : batchItems) {
			Long totalBatchItemQuantity = batchItem.getQuantity();
			Long totalAssignedQuantity = jobworkRepository.getAssignedQuantities(serialCode, 
					jobworkType, batchItem.getItem().getName());
			Long totalRepairableDamagesForItem = damageRepository.getDamagedQuantity(serialCode, 
					DamageType.REPAIRABLE.name(), jobworkType, batchItem.getItem().getName());
			LOGGER.debug("Batch item {} {} quantities : original quantity {}, repairable damages {}, assigned quantity {}", 
					serialCode, batchItem.getItem().getName(), totalBatchItemQuantity, totalRepairableDamagesForItem, totalAssignedQuantity);
			
			Long availableQuantity = totalBatchItemQuantity - totalAssignedQuantity + totalRepairableDamagesForItem;
			BatchItemResponse batchItemResponse = new BatchItemResponse();
			batchItemResponse.setName(batchItem.getItem().getName());
			batchItemResponse.setAvailableQuantity(availableQuantity);
			batchItemResponse.setId(i);
			
			batchItemResponses.add(batchItemResponse);
			i += 1;
			
		}
        if (JobworkType.STITCHING.name().equals(jobworkType)) {
			return batchItemResponses;
		}
        
        // Logic for packaging - get items based on finished stitching items
        LOGGER.debug("Calculating available items for packaging for batch {}", serialCode);
        
        // Clear previous responses as we need to recalculate for packaging
        batchItemResponses.clear();
        i = 1;
        
        // For packaging, we need to check what items are available from stitching receipts
        for (BatchItem batchItem : batchItems) {
        	String itemName = batchItem.getItem().getName();
        	
        	// Get total accepted quantity from stitching receipts (finished items from stitching)
        	Long totalAcceptedFromStitching = receiptItemRepository
        			.getAcceptedQuantityByBatchAndJobworkTypeAndItem(
        					serialCode, 
        					JobworkType.STITCHING.name(), 
        					itemName);
        	LOGGER.debug("Total accepted quantity from STITCHING for item {} in batch {}: {}", 
        			itemName, serialCode, totalAcceptedFromStitching);
        	
        	// Get quantities already assigned to packaging jobworks
        	Long assignedToPackaging = jobworkRepository.getAssignedQuantities(
        			serialCode, 
        			JobworkType.PACKAGING.name(), 
        			itemName);
        	LOGGER.debug("Already assigned to PACKAGING for item {} in batch {}: {}", 
        			itemName, serialCode, assignedToPackaging);
        	
        	// Get repairable damages from packaging (these can be reassigned)
        	Long repairableDamagesFromPackaging = damageRepository.getDamagedQuantity(
        			serialCode, 
        			DamageType.REPAIRABLE.name(), 
        			JobworkType.PACKAGING.name(), 
        			itemName);
        	LOGGER.debug("Repairable damages from PACKAGING for item {} in batch {}: {}", 
        			itemName, serialCode, repairableDamagesFromPackaging);
        	
        	// Calculate available quantity for packaging
        	// Available = (Accepted from Stitching) - (Assigned to Packaging) + (Repairable Damages from Packaging)
        	Long availableForPackaging = totalAcceptedFromStitching - assignedToPackaging + repairableDamagesFromPackaging;
        	
        	LOGGER.debug("Available quantity for PACKAGING for item {} in batch {}: {}", 
        			itemName, serialCode, availableForPackaging);
        	
        	// Only add items that have available quantity
        	if (availableForPackaging > 0) {
        		BatchItemResponse batchItemResponse = new BatchItemResponse();
        		batchItemResponse.setName(itemName);
        		batchItemResponse.setAvailableQuantity(availableForPackaging);
        		batchItemResponse.setId(i);
        		batchItemResponses.add(batchItemResponse);
        		i += 1;
        	}
        }
        
        LOGGER.debug("Total items available for PACKAGING in batch {}: {}", serialCode, batchItemResponses.size());
        return batchItemResponses;
    }
    
    private BatchItem getBatchItemOrThrow(String serialCode, String itemName) {
		return batchItemRepository.findByBatchSerialCodeAndItemName(serialCode, itemName).orElseThrow(() -> {
			LOGGER.error("Batch item not found: {} {}", serialCode, itemName);
			return new BatchItemNotFoundException("Batch item not found: " + serialCode + " " + itemName);
		});
	}

}
