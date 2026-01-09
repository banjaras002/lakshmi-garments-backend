package com.lakshmigarments.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.lakshmigarments.dto.EmployeeRequestDTO;
import com.lakshmigarments.dto.EmployeeResponseDTO;
import com.lakshmigarments.dto.EmployeeStatsDTO;

@Service
public interface EmployeeService {
	
	EmployeeResponseDTO createEmployee(EmployeeRequestDTO employeeRequestDTO);
	
	EmployeeResponseDTO updateEmployee(Long id, EmployeeRequestDTO employeeRequestDTO);

	public Page<EmployeeResponseDTO> getEmployees(
	        Integer pageNo,
	        Integer pageSize,
	        String sortBy,
	        String sortOrder,
	        List<String> employeeNames,
	        List<String> skillNames,
	        Boolean isActive,
	        String search);

	
	EmployeeStatsDTO getEmployeeStats(Long employeeId);

}
