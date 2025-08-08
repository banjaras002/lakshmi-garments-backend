package com.lakshmigarments.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lakshmigarments.dto.CreateSupplierDTO;
import com.lakshmigarments.model.Supplier;
import com.lakshmigarments.service.SupplierService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/suppliers")
@CrossOrigin(origins = "*")
public class SupplierController {

	private static final Logger LOGGER = LoggerFactory.getLogger(SupplierController.class);
	private final SupplierService supplierService;
	
	public SupplierController(SupplierService supplierService) {
		this.supplierService = supplierService;
	}
	
	@PostMapping
	public ResponseEntity<Supplier> createSupplier(@RequestBody @Validated CreateSupplierDTO createSupplierDTO) {
		LOGGER.info("Create a new supplier");
		return new ResponseEntity<>(supplierService.createSupplier(createSupplierDTO), HttpStatus.CREATED);
	}
	
	@GetMapping
	public ResponseEntity<Page<Supplier>> getSuppliers(
			@RequestParam(defaultValue = "0", required = false) Integer pageNo,
			@RequestParam(required = false) Integer pageSize,
			@RequestParam(defaultValue = "id", required = false) String sortBy,
			@RequestParam(defaultValue = "asc", required = false) String sortDir) {
		Page<Supplier> supplierPage = supplierService.getSuppliers(pageNo, pageSize, sortBy, sortDir);
		LOGGER.info("Retrieve suppliers");
		return new ResponseEntity<Page<Supplier>>(supplierPage, HttpStatus.OK);
	}
	

}
