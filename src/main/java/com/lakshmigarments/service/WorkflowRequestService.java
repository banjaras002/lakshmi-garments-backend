package com.lakshmigarments.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.lakshmigarments.dto.WorkflowResponseDTO;
import com.lakshmigarments.model.WorkflowRequest;
import com.lakshmigarments.model.WorkflowRequestDTO;

@Service
public interface WorkflowRequestService {

	WorkflowRequest createWorkflowRequest(WorkflowRequestDTO workflowRequestDTO);

	Page<WorkflowResponseDTO> getAllWorkflowRequests(Integer pageNo, Integer pageSize, String sortBy, String sortDir,
			List<String> requestedByNames);
	
	WorkflowRequest updateWorkflowRequest(Long id, WorkflowRequestDTO workflowRequestDTO);
	
	WorkflowResponseDTO getWorkflowRequest(Long id);

}
