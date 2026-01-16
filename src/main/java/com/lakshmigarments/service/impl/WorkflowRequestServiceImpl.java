package com.lakshmigarments.service.impl;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakshmigarments.configuration.ModelMapperConfig;
import com.lakshmigarments.configuration.ObjectMapperConfig;
import com.lakshmigarments.context.UserContext;
import com.lakshmigarments.context.UserInfo;
import com.lakshmigarments.controller.UserController;
import com.lakshmigarments.dto.WorkflowResponseDTO;
import com.lakshmigarments.dto.request.CreateJobworkReceiptRequest;
import com.lakshmigarments.exception.JobworkNotFoundException;
import com.lakshmigarments.exception.UserNotFoundException;
import com.lakshmigarments.exception.WorkflowRequestNotFoundException;
import com.lakshmigarments.model.Jobwork;
import com.lakshmigarments.model.JobworkStatus;
import com.lakshmigarments.model.User;
import com.lakshmigarments.model.WorkflowRequest;
import com.lakshmigarments.model.WorkflowRequestDTO;
import com.lakshmigarments.model.WorkflowRequestStatus;
import com.lakshmigarments.model.WorkflowRequestType;
import com.lakshmigarments.repository.JobworkRepository;
import com.lakshmigarments.repository.UserRepository;
import com.lakshmigarments.repository.WorkflowRequestRepository;
import com.lakshmigarments.repository.specification.WorfklowRequestSpecification;
import com.lakshmigarments.service.WorkflowRequestService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkflowRequestServiceImpl implements WorkflowRequestService {

    private final UserController userController;


	private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowRequestServiceImpl.class);
	private final WorkflowRequestRepository requestRepository;
	private final ObjectMapper objectMapper;
	private final UserRepository userRepository;
	private final JobworkRepository jobworkRepository;
	private final ModelMapper modelMapper;

	@Override
	public WorkflowRequest createWorkflowRequest(WorkflowRequestDTO workflowRequestDTO) {

//		try {
//			JobworkReceiptDTO jobworkReceiptDTO = objectMapper.readValue(
//					workflowRequestDTO.getPayload(), JobworkReceiptDTO.class);
//			System.out.println(jobworkReceiptDTO.getJobworkReceiptItems().get(0).getItemName());
//		} catch (JsonMappingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (JsonProcessingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
		UserInfo userInfo = UserContext.get();
		User user = userRepository.findById(Long.valueOf(1)).orElse(null);
		
		if (workflowRequestDTO.getRequestType() == WorkflowRequestType.JOBWORK_RECEIPT.toString()) {
			try {
				JsonNode root = objectMapper.readTree(workflowRequestDTO.getPayload());
				JsonNode firstItem = root.get(0);

				String jobworkNumber = firstItem.get("jobworkNumber").asText();
				System.out.println(jobworkNumber);
				Jobwork jobwork = jobworkRepository.findByJobworkNumber(jobworkNumber).orElse(null);
				if (jobwork == null) {
					throw new JobworkNotFoundException("No Jobwork found with ID " + jobworkNumber);
				}
				jobwork.setJobworkStatus(JobworkStatus.AWAITING_APPROVAL);
				jobworkRepository.save(jobwork);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		WorkflowRequest workflowRequest = new WorkflowRequest();
		workflowRequest.setPayload(workflowRequestDTO.getPayload());
		workflowRequest.setRemarks(workflowRequestDTO.getRemarks());
		workflowRequest.setRequestedBy(null);
		workflowRequest.setSystemComments(workflowRequestDTO.getSystemComments());
		workflowRequest.setWorkflowRequestType(WorkflowRequestType.valueOf(workflowRequestDTO.getRequestType()));
		workflowRequest.setWorkflowRequestStatus(WorkflowRequestStatus.PENDING);
		workflowRequest.setRequestedBy(user);
		return requestRepository.save(workflowRequest);
//		return null;
	}

	@Override
	public Page<WorkflowResponseDTO> getAllWorkflowRequests(Integer pageNo, Integer pageSize, String sortBy, String sortDir,List<String> requestedByNames 
			) {
		
		if (pageNo == null) {
			pageNo = 0;
		}
		if (pageSize == null || pageSize == 0) {
			pageSize = 10;
		}
		
		Sort sort = sortDir.equals("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
		
		Specification<WorkflowRequest> specification = Specification
				.where(WorfklowRequestSpecification.filterByRequestedNames(requestedByNames));
		Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
		
		Page<WorkflowRequest> workflowRequestPage = requestRepository.findAll(specification,pageable);
		
		return workflowRequestPage.map(this::convertToWorkflowResponseDTO);
	}
	
	private WorkflowResponseDTO convertToWorkflowResponseDTO(WorkflowRequest entity) {
	    return modelMapper.map(entity, WorkflowResponseDTO.class);
	}

	@Override
	public WorkflowRequest updateWorkflowRequest(Long id, WorkflowRequestDTO workflowRequestDTO) {
		
		UserInfo userInfo = UserContext.get();
		Long userId = Long.valueOf(userInfo.getUserId());
		User user = userRepository.findById(userId).orElseThrow(() -> {
			LOGGER.error("User with ID {} not found", userId);
			return new UserNotFoundException("User not found with ID " + userId);
		});
		
		WorkflowRequest workflowRequest = requestRepository.findById(id).orElseThrow(() -> {
			LOGGER.error("Workflow Request with ID {} not found", id);
			return new WorkflowRequestNotFoundException("Workflow Request not found with ID " + id);
		});

		if (workflowRequestDTO.getRequestStatus() != null) {
			workflowRequest.setWorkflowRequestStatus(WorkflowRequestStatus.valueOf(
					workflowRequestDTO.getRequestStatus()));
		}
		
		workflowRequest.setApprovedBy(user);
		
		return requestRepository.save(workflowRequest);
	}

	@Override
	public WorkflowResponseDTO getWorkflowRequest(Long id) {
		
		WorkflowRequest workflowRequest = requestRepository.findById(id).orElseThrow(() -> {
			LOGGER.error("Workflow Request with ID {} not found", id);
			return new WorkflowRequestNotFoundException("Workflow Request not found with ID " + id);
		});
		
		WorkflowResponseDTO dto = new WorkflowResponseDTO();
		dto.setId(id);
		dto.setRequestedAt(workflowRequest.getRequestedAt());
		dto.setRequestedBy(workflowRequest.getRequestedBy().getName());
		dto.setRequestStatus(workflowRequest.getWorkflowRequestStatus());
		dto.setRequestType(workflowRequest.getWorkflowRequestType());
		
		try {
            dto.setPayload(objectMapper.readTree(workflowRequest.getPayload()));
        } catch (Exception e) {
            // fallback if payload is invalid JSON
            dto.setPayload(null);
        }
		
		return dto;
	}


}
