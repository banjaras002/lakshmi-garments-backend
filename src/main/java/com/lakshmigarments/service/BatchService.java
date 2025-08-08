package com.lakshmigarments.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.lakshmigarments.dto.BatchDTO;
import com.lakshmigarments.dto.BatchSubCategoryDTO;
import com.lakshmigarments.exception.CategoryNotFoundException;
import com.lakshmigarments.exception.SubCategoryNotFoundException;
import com.lakshmigarments.model.Batch;
import com.lakshmigarments.model.BatchStatus;
import com.lakshmigarments.model.BatchSubCategory;
import com.lakshmigarments.model.Category;
import com.lakshmigarments.model.Inventory;
import com.lakshmigarments.model.SubCategory;
import com.lakshmigarments.repository.BatchRepository;
import com.lakshmigarments.repository.BatchStatusRepository;
import com.lakshmigarments.repository.BatchSubCategoryRepository;
import com.lakshmigarments.repository.CategoryRepository;
import com.lakshmigarments.repository.SubCategoryRepository;
import com.lakshmigarments.repository.InventoryRepository;

@Service
public class BatchService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BatchService.class);
	private final BatchRepository batchRepository ;
	private final BatchSubCategoryRepository batchSubCategoryRepository;
	private final CategoryRepository categoryRepository;
	private final SubCategoryRepository subCategoryRepository;
	private final InventoryRepository warehouseRepository;
	private final BatchStatusRepository batchStatusRepository;
	
	public BatchService(BatchRepository batchRepository, BatchSubCategoryRepository batchSubCategoryRepository, 
			CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository,
			InventoryRepository warehouseRepository, BatchStatusRepository batchStatusRepository) {
		this.batchRepository = batchRepository;
		this.batchSubCategoryRepository = batchSubCategoryRepository;
		this.categoryRepository = categoryRepository;
		this.subCategoryRepository = subCategoryRepository;
		this.warehouseRepository = warehouseRepository;
		this.batchStatusRepository = batchStatusRepository;
	}
	
	public BatchDTO createBatch(BatchDTO batchDTO) {
	    // Extract category name
	    String categoryName = batchDTO.getCategory();

	    Category category = categoryRepository.findByName(batchDTO.getCategory()).orElseThrow(() -> {
	        LOGGER.error("Category not found with name {}", categoryName);
	        return new CategoryNotFoundException("Category not found with name " + categoryName);
	    });

	    for (BatchSubCategoryDTO subCategoryDTO : batchDTO.getSubCategories()) {
	        String subCategoryName = subCategoryDTO.getSubCategory();
	        
	        subCategoryRepository.findByName(subCategoryName).orElseThrow(() -> {
	            LOGGER.error("SubCategory not found with name {}", subCategoryName);
	            return new SubCategoryNotFoundException("SubCategory not found with name " + subCategoryName);
	        });
	    }
	    
	    BatchStatus batchStatus = batchStatusRepository.findByName("CUTTING").orElse(null);
	    
	    Batch batch = new Batch();
	    batch.setSerialCode(batchDTO.getSerialCode());
	    batch.setCategory(category);
	    batch.setRemarks(batchDTO.getRemarks());
	    batch.setIsUrgent(batchDTO.getIsUrgent());
	    batch.setBatchStatus(batchStatus);
	    if (batchDTO.getIsUrgent()) {
			batch.setSerialCode(batchDTO.getSerialCode() + " (U)");
		}
	    Batch createdBatch = batchRepository.save(batch);
	    
	    for (BatchSubCategoryDTO batchSubCategoryDTO : batchDTO.getSubCategories()) {
	    	
	    	String subCategoryName = batchSubCategoryDTO.getSubCategory();
	    	
	    	SubCategory subCategory = subCategoryRepository.findByName(subCategoryName).orElseThrow(() -> {
	            LOGGER.error("SubCategory not found with name {}", subCategoryName);
	            return new SubCategoryNotFoundException("SubCategory not found with name " + subCategoryName);
	        });
	    	// Reduce the count from inventory
	    	Inventory inventory = warehouseRepository.findBySubCategoryName(subCategoryName).orElse(null);
	    	inventory.setCount(inventory.getCount() - batchSubCategoryDTO.getQuantity());
	    	warehouseRepository.save(inventory);
	    	
			BatchSubCategory batchSubCategory = new BatchSubCategory();
			batchSubCategory.setBatch(createdBatch);
			batchSubCategory.setQuantity(batchSubCategoryDTO.getQuantity());
			batchSubCategory.setSubCategory(subCategory);
			batchSubCategoryRepository.save(batchSubCategory);
		}

	    return batchDTO;
	}

	public List<BatchDTO> getBatches(String search) {
		
		List<Batch> batches;
		if (search != null && !search.isEmpty()) {
	        batches = batchRepository.findBySerialCodeContaining(search);
	    } else {
	        batches = batchRepository.findAll(Sort.by(Sort.Order.desc("createdAt")));
	    }
	    
	    List<BatchDTO> batchDTOs = batches.stream().map(batch -> {
	        List<BatchSubCategoryDTO> batchSubCategoryDTOs = batchSubCategoryRepository.findByBatch(batch)
	                .stream()
	                .map(batchSubCategory -> new BatchSubCategoryDTO(
	                        batchSubCategory.getId(),
	                        batchSubCategory.getSubCategory().getName(),
	                        batchSubCategory.getQuantity()
	                ))
	                .collect(Collectors.toList()); // Collect the mapped results into a list
	        
	        

	        return new BatchDTO(batch.getId(), batch.getCategory().getName(), 
	        		batch.getSerialCode(), batch.getCreatedAt(), batch.getBatchStatus().getName(), 
	        		batch.getIsUrgent(), batch.getRemarks(), batchSubCategoryDTOs) ;
	    }).collect(Collectors.toList()); // Collect into a list

	    return batchDTOs;
	}

	
}
