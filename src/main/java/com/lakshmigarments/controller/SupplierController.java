package com.lakshmigarments.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lakshmigarments.dto.SupplierCreateRequest;
import com.lakshmigarments.dto.SupplierResponse;
import com.lakshmigarments.service.SupplierService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/suppliers")
@AllArgsConstructor
public class SupplierController {

	private static final Logger LOGGER = LoggerFactory.getLogger(SupplierController.class);
	private final SupplierService supplierService;

	@PostMapping
	public ResponseEntity<SupplierResponse> createSupplier(
			@Valid @RequestBody SupplierCreateRequest supplierCreateRequest) {
		LOGGER.info("Creating a new supplier: {}", supplierCreateRequest.getName());
		SupplierResponse supplierResponse = supplierService.createSupplier(supplierCreateRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(supplierResponse);
	}

	@GetMapping
	public ResponseEntity<List<SupplierResponse>> getSuppliers(@RequestParam(required = false) String search) {
		LOGGER.info("Fetching all suppliers with search: {}", search);
		List<SupplierResponse> suppliers = supplierService.getSuppliers(search);
		return ResponseEntity.ok(suppliers);
	}

	@PutMapping("/{id}")
	public ResponseEntity<SupplierResponse> updateSupplier(
			@PathVariable Long id,
			@Valid @RequestBody SupplierCreateRequest supplierRequest) {
		LOGGER.info("Updating supplier with ID: {}", id);
		SupplierResponse savedSupplier = supplierService.updateSupplier(id, supplierRequest);
		return ResponseEntity.ok(savedSupplier);
	}

}
