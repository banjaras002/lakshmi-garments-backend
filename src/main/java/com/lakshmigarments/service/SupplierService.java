package com.lakshmigarments.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.lakshmigarments.dto.SupplierCreateRequest;
import com.lakshmigarments.dto.SupplierResponse;

@Service
public interface SupplierService {

    SupplierResponse createSupplier(SupplierCreateRequest supplierRequestDTO);

    SupplierResponse updateSupplier(Long id, SupplierCreateRequest supplierRequestDTO);

    List<SupplierResponse> getSuppliers(String search);

}
