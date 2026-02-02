package com.lakshmigarments.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lakshmigarments.dto.request.SupplierRequest;
import com.lakshmigarments.dto.response.SupplierResponse;
import com.lakshmigarments.exception.DuplicateSupplierException;
import com.lakshmigarments.exception.SupplierNotFoundException;
import com.lakshmigarments.model.Supplier;
import com.lakshmigarments.repository.SupplierRepository;
import com.lakshmigarments.repository.specification.SupplierSpecification;
import com.lakshmigarments.service.SupplierService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SupplierServiceImpl.class);

	private final SupplierRepository supplierRepository;
	private final ModelMapper modelMapper;

	@Override
	@Transactional(readOnly = true)
	public List<SupplierResponse> getSuppliers(String search) {
		LOGGER.debug("Fetching all suppliers matching: {}", search);
		Specification<Supplier> spec = SupplierSpecification.filterByName(search);
		List<Supplier> suppliers = supplierRepository.findAll(spec);

		LOGGER.debug("Found {} supplier(s)", suppliers.size());
		return suppliers.stream()
				.map(supplier -> modelMapper.map(supplier, SupplierResponse.class))
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public SupplierResponse createSupplier(SupplierRequest supplierRequest) {
		LOGGER.debug("Creating supplier: {}", supplierRequest.getName());
		String supplierName = supplierRequest.getName().trim();
		String supplierLocation = supplierRequest.getLocation().trim();

		validateSupplierUniqueness(supplierName, null);

		Supplier supplier = new Supplier();
		supplier.setName(supplierName);
		supplier.setLocation(supplierLocation);

		Supplier savedSupplier = supplierRepository.save(supplier);
		LOGGER.info("Supplier created successfully with ID: {}", savedSupplier.getId());
		return modelMapper.map(savedSupplier, SupplierResponse.class);
	}

	@Override
	@Transactional
	public SupplierResponse updateSupplier(Long id, SupplierRequest supplierRequest) {
		LOGGER.debug("Updating supplier with ID: {}", id);
		
		Supplier supplier = this.getSupplierOrThrow(id);
		
		String supplierName = supplierRequest.getName().trim();
		String supplierLocation = supplierRequest.getLocation().trim();

		validateSupplierUniqueness(supplierName, id);

		supplier.setName(supplierName);
		supplier.setLocation(supplierLocation);

		Supplier savedSupplier = supplierRepository.save(supplier);
		LOGGER.info("Supplier updated successfully with ID: {}", savedSupplier.getId());
		return modelMapper.map(savedSupplier, SupplierResponse.class);
	}

	private void validateSupplierUniqueness(String name, Long id) {
		if (id == null) {
			if (supplierRepository.existsByNameIgnoreCase(name)) {
				LOGGER.error("Supplier name already exists: {}", name);
				throw new DuplicateSupplierException("Supplier already exists with name: " + name);
			}
		} else {
			if (supplierRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
				LOGGER.error("Supplier name already exists for another ID: {}", name);
				throw new DuplicateSupplierException("Supplier already exists with name: " + name);
			}
		}
	}

	private Supplier getSupplierOrThrow(Long id) {
		return supplierRepository.findById(id).orElseThrow(() -> {
			LOGGER.error("Supplier not found with ID: {}", id);
			return new SupplierNotFoundException("Supplier not found with ID: " + id);
		});
	}

}
