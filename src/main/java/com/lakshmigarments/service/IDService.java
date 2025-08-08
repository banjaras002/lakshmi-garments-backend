package com.lakshmigarments.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lakshmigarments.repository.BatchRepository;
import com.lakshmigarments.repository.CategoryRepository;
import com.lakshmigarments.repository.LorryReceiptRepository;

@Service
public class IDService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(IDService.class);
	private final LorryReceiptRepository lorryReceiptRepository;
	private final BatchRepository batchRepository;
	private final CategoryRepository categoryRepository;
	
	public IDService(LorryReceiptRepository lorryReceiptRepository, BatchRepository batchRepository,
			CategoryRepository categoryRepository) {
		this.lorryReceiptRepository = lorryReceiptRepository;
		this.batchRepository = batchRepository;
		this.categoryRepository =categoryRepository;
	}
	
	public Long getNextLRID() {
		Long lrCount = lorryReceiptRepository.count();
		return lrCount + 1;
	}
	
	public String getSerialCode(String categoryName) {
	    String latestSerialCode = batchRepository.findLatestSerialCodeByCategoryName(categoryName).orElse(null);
	    String categoryCode = categoryRepository.findCodeByName(categoryName).orElse(null);
	    
	    if (latestSerialCode == null) {
	        return categoryCode + "0001";
	    }

	    // Strip any parenthetical suffix like (U)
	    String cleanedSerialCode = latestSerialCode.contains("(")
	            ? latestSerialCode.substring(0, latestSerialCode.indexOf('(')).trim()
	            : latestSerialCode;

	    Integer numericPart = Integer.parseInt(cleanedSerialCode.substring(categoryCode.length()));
	    numericPart++;

	    String incrementedNumericPart = String.format("%04d", numericPart);
	    return categoryCode + incrementedNumericPart;
	}


}
