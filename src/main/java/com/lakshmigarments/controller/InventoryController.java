package com.lakshmigarments.controller;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lakshmigarments.dto.CategoryCountDTO;
import com.lakshmigarments.service.InventoryService;

@RestController
@RequestMapping("/inventories")
@CrossOrigin(origins = "*")
public class InventoryController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(InventoryController.class);
	private final InventoryService warehouseService;
	
	public InventoryController(InventoryService warehouseService) {
		this.warehouseService = warehouseService;
	}
	
	@GetMapping("/categoryCount")
	public List<CategoryCountDTO> getCategoryCount() {
		return warehouseService.getCategorySubCategoryCounts();
	}
	
	@GetMapping("/search")
	public Integer getCategorySubCategoryCount( @RequestParam String category,
            @RequestParam String subCategory) {
		return warehouseService.getCategorySubCategoryCount(category, subCategory).getCount();
	}

}
