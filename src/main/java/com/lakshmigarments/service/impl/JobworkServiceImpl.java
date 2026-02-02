package com.lakshmigarments.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.lakshmigarments.context.UserContext;
import com.lakshmigarments.context.UserInfo;
import com.lakshmigarments.controller.AuthController;
import com.lakshmigarments.controller.JobworkController;
import com.lakshmigarments.controller.WorkflowRequestController;
import com.lakshmigarments.dto.JobworkItemDTO;
import com.lakshmigarments.dto.JobworkRequestDTO;
import com.lakshmigarments.dto.JobworkResponseDTO;
import com.lakshmigarments.dto.request.CreateCuttingJobworkRequest;
import com.lakshmigarments.dto.request.CreateJobworkRequest;
import com.lakshmigarments.dto.request.CreateDamageRequest;
import com.lakshmigarments.dto.request.CreateItemBasedJobworkRequest;
import com.lakshmigarments.dto.request.CreateJobworkReceiptItemRequest;
import com.lakshmigarments.dto.response.EmployeeJobworkResponse;
import com.lakshmigarments.dto.response.ItemResponse;
import com.lakshmigarments.dto.response.DetailedEmployeeJobworkResponse;
import com.lakshmigarments.dto.response.EmployeeJobworkReportResponse;
import com.lakshmigarments.dto.response.JobworkDetailDTO;
import com.lakshmigarments.dto.response.JobworkItemResponse;
import com.lakshmigarments.dto.response.JobworkResponse;
import com.lakshmigarments.model.Batch;
import com.lakshmigarments.model.BatchItem;
import com.lakshmigarments.model.BatchStatus;
import com.lakshmigarments.model.Damage;
import com.lakshmigarments.model.DamageType;
import com.lakshmigarments.model.Employee;
import com.lakshmigarments.model.Item;
import com.lakshmigarments.model.Jobwork;
import com.lakshmigarments.model.JobworkItem;
import com.lakshmigarments.model.JobworkOrigin;
import com.lakshmigarments.model.JobworkReceipt;
import com.lakshmigarments.model.JobworkReceiptItem;
import com.lakshmigarments.model.JobworkStatus;
import com.lakshmigarments.model.JobworkItemStatus;
import com.lakshmigarments.model.JobworkType;
import com.lakshmigarments.model.User;
import com.lakshmigarments.exception.BatchItemNotFoundException;
import com.lakshmigarments.exception.BatchNotFoundException;
import com.lakshmigarments.exception.EmployeeNotFoundException;
import com.lakshmigarments.exception.ItemNotFoundException;
import com.lakshmigarments.exception.JobworkNotFoundException;
import com.lakshmigarments.exception.JobworkTypeNotFoundException;
import com.lakshmigarments.exception.UserNotFoundException;
import com.lakshmigarments.repository.BatchItemRepository;
import com.lakshmigarments.repository.BatchRepository;
import com.lakshmigarments.repository.DamageRepository;
import com.lakshmigarments.repository.EmployeeRepository;
import com.lakshmigarments.repository.ItemRepository;
import com.lakshmigarments.repository.JobworkItemRepository;
import com.lakshmigarments.repository.JobworkReceiptRepository;
import com.lakshmigarments.repository.JobworkRepository;
import com.lakshmigarments.repository.UserRepository;
import com.lakshmigarments.repository.specification.JobworkSpecification;
import com.lakshmigarments.service.BatchService;
import com.lakshmigarments.service.JobworkService;
import com.lakshmigarments.service.validation.JobworkCreationValidator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobworkServiceImpl implements JobworkService<CreateJobworkRequest> {

	private static final Logger LOGGER = LoggerFactory.getLogger(JobworkServiceImpl.class);
	private final JobworkRepository jobworkRepository;
	private final EmployeeRepository employeeRepository;
	private final ItemRepository itemRepository;
	private final ModelMapper modelMapper;
	private final BatchRepository batchRepository;
	private final UserRepository userRepository;
	private final JobworkItemRepository jobworkItemRepository;
	private final JobworkReceiptRepository jobworkReceiptRepository;
	private final BatchItemRepository batchItemRepository;
	private final DamageRepository damageRepository;
	private final JobworkCreationValidator jobworkCreationValidator;
	private final BatchService batchService;

	@Override
	public Page<JobworkResponseDTO> getAllJobworks(Pageable pageable, String search, List<String> assignedToNames,
			List<JobworkStatus> statuses, List<JobworkType> types, List<String> batchSerialCodes,
			LocalDateTime startDate, LocalDateTime endDate) {

		// 1. Build Dynamic Specification
		Specification<Jobwork> spec = Specification.where(null);

		// Global Search (Case-insensitive Jobwork Number)
		if (search != null && !search.trim().isEmpty()) {
			spec = spec.and(JobworkSpecification.filterUniqueByJobworkNumber(search.trim()));
		}

		// Filter by Multiple Employee Names
		if (assignedToNames != null && !assignedToNames.isEmpty()) {
			spec = spec.and(JobworkSpecification.assignedToNamesIn(assignedToNames));
		}

		// Filter by Multiple Statuses
		if (statuses != null && !statuses.isEmpty()) {
			spec = spec.and(JobworkSpecification.hasStatuses(statuses));
		}

		// Filter by Multiple Jobwork Types
		if (types != null && !types.isEmpty()) {
			spec = spec.and(JobworkSpecification.hasJobworkTypes(types));
		}

		// Filter by Multiple Batch Serial Codes
		if (batchSerialCodes != null && !batchSerialCodes.isEmpty()) {
			spec = spec.and(JobworkSpecification.batchSerialCodesIn(batchSerialCodes));
		}

		// Date Range Filter
		if (startDate != null || endDate != null) {
			spec = spec.and(JobworkSpecification.assignedBetween(startDate, endDate));
		}

		// 2. Fetch Jobworks using Specification
		Page<Jobwork> jobworks = jobworkRepository.findAll(spec, pageable);

		// 3. Optimized Receipt Fetching (Batching to prevent N+1)
		List<String> jobworkNumbers = jobworks.getContent().stream().map(Jobwork::getJobworkNumber).toList();

		Map<String, List<JobworkReceipt>> receiptsByJobworkNumber = new HashMap<>();
		if (!jobworkNumbers.isEmpty()) {
			List<JobworkReceipt> receipts = jobworkReceiptRepository.findByJobworkJobworkNumberIn(jobworkNumbers);
			receiptsByJobworkNumber = receipts.stream()
					.collect(Collectors.groupingBy(r -> r.getJobwork().getJobworkNumber()));
		}

		// 4. Convert to DTO
		List<JobworkResponseDTO> jobworkResponseDTOs = convertToJobworkResponseDTO(jobworks.getContent(),
				receiptsByJobworkNumber);

		LOGGER.info("Fetched {} filtered jobworks on page {}", jobworkResponseDTOs.size(), pageable.getPageNumber());
		return new PageImpl<>(jobworkResponseDTOs, pageable, jobworks.getTotalElements());
	}

//	@Override
//	@Transactional
//	public Jobwork createJobwork(JobworkRequestDTO jobworkRequestDTO) {
//		
////		UserInfo userInfo = UserContext.get();
////		Long userId = Long.valueOf(userInfo.getUserId());
////		
////		User user = userRepository.findById(userId).orElseThrow(() -> {
////			LOGGER.error("User with ID {} not found", userId);
////			return new UserNotFoundException("User not found with ID " + userId);
////		});
//
//		Employee employee = employeeRepository.findById(jobworkRequestDTO.getEmployeeId()).orElseThrow(() -> {
//			LOGGER.error("Employee with ID {} not found", jobworkRequestDTO.getEmployeeId());
//			return new EmployeeNotFoundException("Employee not found with ID " + jobworkRequestDTO.getEmployeeId());
//		});
//
////		User user = userRepository.findById(jobworkRequestDTO.getAssignedBy()).orElseThrow(() -> {
////			LOGGER.error("User with ID {} not found", jobworkRequestDTO.getAssignedBy());
////			return new UserNotFoundException("User not found with ID " + jobworkRequestDTO.getAssignedBy());
////		});
//
//		Batch batch = batchRepository.findBySerialCode(jobworkRequestDTO.getBatchSerialCode()).orElseThrow(() -> {
//			LOGGER.error("Batch with serial code {} not found", jobworkRequestDTO.getBatchSerialCode());
//			return new BatchNotFoundException(
//					"Batch not found with serial code " + jobworkRequestDTO.getBatchSerialCode());
//		});
//
//		// CUTTING
//		if (jobworkRequestDTO.getJobworkType() == JobworkType.CUTTING) {
//
//			// TODO to return correct object
//			if (jobworkRequestDTO.getQuantities().size() <= 0) {
//				return null;
//			}
//			Long quantity = jobworkRequestDTO.getQuantities().get(0);
//
//			batch.setAvailableQuantity(batch.getAvailableQuantity() - quantity);
//			batch.setBatchStatus(BatchStatus.ASSIGNED);
//
//			Jobwork jobwork = new Jobwork();
////			jobwork.setAssignedBy(user);
//			jobwork.setAssignedTo(employee);
//			jobwork.setBatch(batch);
//			jobwork.setJobworkType(jobworkRequestDTO.getJobworkType());
//			jobwork.setJobworkNumber(jobworkRequestDTO.getJobworkNumber());
//			jobwork.setJobworkOrigin(JobworkOrigin.ORIGINAL);
//			jobwork.setJobworkStatus(JobworkStatus.IN_PROGRESS);
//			jobwork.setRemarks(jobworkRequestDTO.getRemarks());
//			jobwork.setAssignedTo(employee);
//			Jobwork createdJobwork = jobworkRepository.save(jobwork);
//
//			JobworkItem jobworkItem = new JobworkItem();
//			jobworkItem.setJobwork(createdJobwork);
//			jobworkItem.setQuantity(quantity);
//			jobworkItem.setJobworkStatus(JobworkItemStatus.IN_PROGRESS);
//			jobworkItemRepository.save(jobworkItem);
//
//			return createdJobwork;
//		}
//
////        Item item = null;
////        if (jobworkRequestDTO.getItemId() != null) {
////            item = itemRepository.findById(jobworkRequestDTO.getItemId()).orElseThrow(() -> {
////                LOGGER.error("Item with ID {} not found", jobworkRequestDTO.getItemId());
////                return new ItemNotFoundException("Item not found with ID " + jobworkRequestDTO.getItemId());
////            });
////        }
//		
//		Jobwork jobwork = new Jobwork();
////		jobwork.setAssignedBy(user);
//		jobwork.setAssignedTo(employee);
//		jobwork.setBatch(batch);
//		jobwork.setJobworkType(jobworkRequestDTO.getJobworkType());
//		jobwork.setJobworkNumber(jobworkRequestDTO.getJobworkNumber());
//		jobwork.setJobworkOrigin(JobworkOrigin.ORIGINAL);
//		jobwork.setJobworkStatus(JobworkStatus.IN_PROGRESS);
//		jobwork.setRemarks(jobworkRequestDTO.getRemarks());
//		Jobwork createdJobwork = jobworkRepository.save(jobwork);
//		
//		Long totalQuantity = 0L;
//		for (Long quantity : jobworkRequestDTO.getQuantities()) {
//			totalQuantity += quantity;
//		}
//		
//		batch.setAvailableQuantity(batch.getAvailableQuantity() - totalQuantity);
//		batch.setBatchStatus(BatchStatus.ASSIGNED);
//		
//		int i = 0; 
//		for (String itemName : jobworkRequestDTO.getItemNames()) {
//			
//			Item existingItem = itemRepository.findByName(itemName).orElseThrow(() -> {
//				LOGGER.error("Item not found with name {}", itemName);
//				return new ItemNotFoundException("Item not found with name " + itemName);
//			});
//			
//			JobworkItem jobworkItem = new JobworkItem();
//			jobworkItem.setJobwork(createdJobwork);
//			jobworkItem.setItem(existingItem);
//			jobworkItem.setQuantity(jobworkRequestDTO.getQuantities().get(i));
//			jobworkItem.setJobworkStatus(JobworkItemStatus.IN_PROGRESS);
//			jobworkItemRepository.save(jobworkItem);
//						
//			i += 1;
//		}
//
//	
//		return createdJobwork;
//	}

	@Override
	public List<String> getJobworkNumbers(String search) {
		LOGGER.debug("Fetching jobwork numbers with search: {}", search);
		return jobworkRepository.findUniqueJobworksByJobworkNumber().stream().map(Jobwork::getJobworkNumber)
				.collect(Collectors.toList());
	}

	@Override
	public JobworkDetailDTO getJobworkDetail(String jobworkNumber) {

		LOGGER.debug("Fetching jobwork detail for jobwork number: {}", jobworkNumber);

		Jobwork jobwork = jobworkRepository.findByJobworkNumber(jobworkNumber).orElseThrow(() -> {
			LOGGER.error("Jobwork with number {} not found", jobworkNumber);
			return new JobworkNotFoundException("Jobwork with number " + jobworkNumber + " not found");
		});

		List<JobworkItem> jobworkItems = jobworkItemRepository.findAllByJobwork(jobwork);

		// ✅ Convert entities → DTOs using ModelMapper
		List<JobworkItemDTO> jobworkItemDTOs = jobworkItems.stream().map(this::jwTojwDTO).toList();

		List<JobworkReceipt> receipts = jobworkReceiptRepository
				.findByJobworkJobworkNumberIn(Arrays.asList(jobworkNumber));
		List<JobworkReceiptItem> receiptItems = new ArrayList<>();

		System.out.println(receipts.size());

		long returnedQuantity = 0;
		for (JobworkReceipt jobworkReceipt : receipts) {
			List<JobworkReceiptItem> itemReceiptItems = jobworkReceipt.getJobworkReceiptItems();
			for (JobworkReceiptItem jobworkReceiptItem : itemReceiptItems) {
				receiptItems.add(jobworkReceiptItem);
			}

		}

		JobworkDetailDTO dto = new JobworkDetailDTO();
		dto.setStartedAt(jobwork.getCreatedAt());
		dto.setAssignedBy(jobwork.getCreatedBy());
		dto.setAssignedTo(jobwork.getAssignedTo().getName());
		dto.setBatchSerialCode(jobwork.getBatch().getSerialCode());
		dto.setJobworkNumber(jobworkNumber);
		dto.setJobworkOrigin(jobwork.getJobworkOrigin().name());
		dto.setJobworkType(jobwork.getJobworkType().name());
		dto.setRemarks(jobwork.getRemarks());
		dto.setJobworkItems(jobworkItemDTOs);

		List<JobworkItemResponse> receiptItemDTOs = receipts.stream().flatMap(r -> r.getJobworkReceiptItems().stream())
				.map(this::toReceiptItemDTO).toList();

		dto.setJobworkReceiptItems(receiptItemDTOs);
		dto.setJobworkStatus(jobwork.getJobworkStatus().toString());

		return dto;
	}

	private JobworkItemResponse toReceiptItemDTO(JobworkReceiptItem item) {
		JobworkItemResponse jobworkItemResponse = new JobworkItemResponse();

		jobworkItemResponse.setItemName(item.getItem().getName());
		jobworkItemResponse.setAcceptedQuantity(item.getAcceptedQuantity());
		jobworkItemResponse.setSalesQuantity(item.getSalesQuantity());
		jobworkItemResponse.setSalesPrice(item.getSalesPrice());
		jobworkItemResponse.setWagePerItem(item.getWagePerItem());
		jobworkItemResponse.setDamagedQuantity(item.getDamagedQuantity());
		return jobworkItemResponse;
	}

	public String getNextJobworkNumber() {

		// Format: yyyyMMdd (e.g. 20251228)
		String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

		Jobwork lastJobwork = jobworkRepository.findTop1ByOrderByJobworkNumberDesc().orElse(null);

		int nextSequence = 1;

		if (lastJobwork != null) {
			String lastNumber = lastJobwork.getJobworkNumber();

			// Expected format: JW-yyyyMMdd-XXX
			String[] parts = lastNumber.split("-");

			if (parts.length == 3 && parts[1].equals(today)) {
				nextSequence = Integer.parseInt(parts[2]) + 1;
			}
		}

		return "JW-" + today + "-" + String.format("%03d", nextSequence);
	}

////    private JobworkDetailDTO convertToJobworkDetailDTO(List<Jobwork> jobworks) {
////
////        List<String> items = new ArrayList<>();
////        List<Long> quantity = new ArrayList<>();
////
////        for (Jobwork jobwork : jobworks) {
////            if (jobwork.getItem() != null) {
////                items.add(jobwork.getItem().getName());
////            }
////            if (jobwork.getQuantity() != null) {
////                quantity.add(jobwork.getQuantity());
////            }
////        }
////
////        JobworkDetailDTO jobworkDetailDTO = new JobworkDetailDTO();
////        jobworkDetailDTO.setJobworkNumber(jobworks.get(0).getJobworkNumber());
////        jobworkDetailDTO.setStartedAt(jobworks.get(0).getStartedAt());
////        jobworkDetailDTO.setJobworkType(jobworks.get(0).getJobworkType());
////        jobworkDetailDTO.setBatchSerialCode(jobworks.get(0).getBatch().getSerialCode());
////        jobworkDetailDTO.setAssignedTo(jobworks.get(0).getEmployee().getName());
////        jobworkDetailDTO.setItems(items);
////        jobworkDetailDTO.setQuantity(quantity);
////        return jobworkDetailDTO;
////
////    }
//
	private List<JobworkResponseDTO> convertToJobworkResponseDTO(List<Jobwork> jobworks,
			Map<String, List<JobworkReceipt>> receiptsByJobworkNumber) {

		return jobworks.stream().map(jobwork -> mapToDTO(jobwork,
				receiptsByJobworkNumber.getOrDefault(jobwork.getJobworkNumber(), List.of()))).toList();
	}

	private JobworkResponseDTO mapToDTO(Jobwork jobwork, List<JobworkReceipt> receipts) {
		JobworkResponseDTO jobworkResponseDTO = new JobworkResponseDTO();
		jobworkResponseDTO.setId(jobwork.getId());
		jobworkResponseDTO.setAssignedTo(jobwork.getAssignedTo().getName());
		jobworkResponseDTO.setBatchSerial(jobwork.getBatch().getSerialCode());
		jobworkResponseDTO.setJobworkType(jobwork.getJobworkType().toString());
		jobworkResponseDTO.setJobworkNumber(jobwork.getJobworkNumber());
		jobworkResponseDTO.setStartedAt(jobwork.getCreatedAt());

		List<JobworkItem> jobworkItems = jobwork.getJobworkItems();
		long totalIssuedQty = jobwork.getJobworkItems().stream().mapToLong(JobworkItem::getQuantity).sum();

		JobworkStatus status = JobworkStatus.CLOSED;
		int completedCount = 0, inProgressCount = 0;
		for (JobworkItem jobworkItem : jobworkItems) {
			if (jobworkItem.getJobworkItemStatus() == JobworkItemStatus.IN_PROGRESS) {
				inProgressCount += 1;
			} else if (jobworkItem.getJobworkItemStatus() == JobworkItemStatus.CLOSED) {
				completedCount += 1;
			}
		}

		// deduct the submitted quantities for the jobwork
		long returnedQuantity = 0;
		for (JobworkReceipt jobworkReceipt : receipts) {
			List<JobworkReceiptItem> receiptItems = jobworkReceipt.getJobworkReceiptItems();
			for (JobworkReceiptItem jobworkReceiptItem : receiptItems) {
				returnedQuantity += jobworkReceiptItem.getDamagedQuantity() + jobworkReceiptItem.getSalesQuantity()
						+ jobworkReceiptItem.getAcceptedQuantity();
			}

		}

		// evaluate jobwork status
		if (completedCount == jobworkItems.size()) {
			status = JobworkStatus.CLOSED;
		} else if (completedCount < jobworkItems.size() && completedCount != 0) {
			status = JobworkStatus.PENDING_RETURN;
		} else if (completedCount == 0) {
			status = JobworkStatus.IN_PROGRESS;
		}

		jobworkResponseDTO.setTotalQuantitesIssued(totalIssuedQty);
		jobworkResponseDTO.setStatus(jobwork.getJobworkStatus().toString());
		jobworkResponseDTO.setPendingQuantity(totalIssuedQty - returnedQuantity);

		return jobworkResponseDTO;
	}

	private JobworkItemDTO jwTojwDTO(JobworkItem item) {

		JobworkItemDTO dto = new JobworkItemDTO();
		dto.setId(item.getId());
		dto.setQuantity(item.getQuantity());
		dto.setStatus(item.getJobworkItemStatus().name());

		if (item.getItem() != null) {
			dto.setItemName(item.getItem().getName());
		}

		if (item.getJobwork() != null) {
			dto.setJobworkNumber(item.getJobwork().getJobworkNumber());
		}

		return dto;
	}

	@Override
	@Transactional
	public Jobwork reAssignJobwork(String jobworkNumber, String employeeName) {
		LOGGER.debug("Reassigning jobwork {}", jobworkNumber);
		Jobwork oldJobwork = this.getJobworkOrThrow(jobworkNumber);
		Employee employee = this.getEmployeeOrThrow(employeeName);

		Jobwork newJobwork = new Jobwork();
		newJobwork.setBatch(oldJobwork.getBatch());
		newJobwork.setAssignedTo(employee);
		newJobwork.setJobworkNumber(this.getNextJobworkNumber());
		newJobwork.setJobworkOrigin(JobworkOrigin.REASSIGNED);
		newJobwork.setJobworkStatus(JobworkStatus.IN_PROGRESS);
		newJobwork.setJobworkType(oldJobwork.getJobworkType());
		newJobwork.setRemarks(oldJobwork.getRemarks());
		newJobwork.setParentJobwork(oldJobwork);

		// 🔹 CLONE jobwork items
		List<JobworkItem> clonedItems = oldJobwork.getJobworkItems().stream().map(oldItem -> {
			JobworkItem item = new JobworkItem();
			item.setItem(oldItem.getItem());
			item.setQuantity(oldItem.getQuantity());
			item.setJobworkItemStatus(oldItem.getJobworkItemStatus());
			item.setJobwork(newJobwork); // parent set
			return item;
		}).toList();

		newJobwork.setJobworkItems(clonedItems);

		Jobwork savedJobwork = jobworkRepository.save(newJobwork);

		// 🔹 Mark old jobwork as reassigned
		oldJobwork.setJobworkStatus(JobworkStatus.REASSIGNED);
		jobworkRepository.save(oldJobwork);
		LOGGER.debug("Marked the reassigned jobwork {} to {}", jobworkNumber, JobworkStatus.REASSIGNED);

		return savedJobwork;
	}

	// to create a jobwork
	@Override
	@Transactional
	public JobworkResponse createJobwork(CreateJobworkRequest request) {
		LOGGER.debug("Creating a new jobwork");

		Employee employee = getEmployeeOrThrow(request.getAssignedTo());
		Batch batch = getBatchOrThrow(request.getBatchSerialCode());

		if (request instanceof CreateCuttingJobworkRequest cuttingRequest) {

			Long assignedJobworksQuantity = jobworkRepository.getAssignedQuantities(request.getBatchSerialCode(),
					JobworkType.CUTTING.name());

			Long damagedQuantity = damageRepository.getDamagedQuantity(request.getBatchSerialCode(),
					DamageType.REPAIRABLE.name(), JobworkType.CUTTING.name());

			Long batchQuantity = batchRepository.findQuantityBySerialCode(batch.getSerialCode());

			LOGGER.debug("Cutting jobwork creation request received");

			jobworkCreationValidator.validateCuttingQuantityAvailability(batchQuantity, cuttingRequest,
					assignedJobworksQuantity, damagedQuantity);

			LOGGER.debug("Quantities for batch {} validated", cuttingRequest.getBatchSerialCode());

			return this.createCuttingJobwork(cuttingRequest, employee, batch);

		} else if (request instanceof CreateItemBasedJobworkRequest itemJobworkRequest) {

			LOGGER.debug("{} jobwork creation request received", itemJobworkRequest.getJobworkType());

			for (int i = 0; i < itemJobworkRequest.getItemNames().size(); i++) {

				String itemName = itemJobworkRequest.getItemNames().get(i);
				Long requestedQuantity = itemJobworkRequest.getQuantities().get(i);

				BatchItem batchItem = this.getBatchItemOrThrow(itemJobworkRequest.getBatchSerialCode(), itemName);

				Long batchItemQuantity = batchItem.getQuantity();

				Long assignedQuantity = jobworkRepository.getAssignedQuantities(itemJobworkRequest.getBatchSerialCode(),
						itemJobworkRequest.getJobworkType().name(), itemName);

				Long repairableDamages = damageRepository.getDamagedQuantity(itemJobworkRequest.getBatchSerialCode(),
						DamageType.REPAIRABLE.name(), itemJobworkRequest.getJobworkType().name(), itemName);

				jobworkCreationValidator.validateItemQuantityAvailability(batchItemQuantity, assignedQuantity,
						repairableDamages, requestedQuantity, itemName, itemJobworkRequest.getJobworkType());

				LOGGER.debug("Validated item {} successfully", itemName);
			}

			return this.createItemBasedJobwork(itemJobworkRequest, employee, batch);
		}

		LOGGER.error("Unsupported jobwork type {}", request.getJobworkType());
		throw new JobworkTypeNotFoundException("Unsupported Jobwork Type");
	}

	private JobworkResponse createCuttingJobwork(CreateCuttingJobworkRequest request, Employee employee, Batch batch) {

		Jobwork jobwork = new Jobwork();
		jobwork.setJobworkNumber(request.getJobworkNumber());
		jobwork.setJobworkType(request.getJobworkType());
		jobwork.setAssignedTo(employee);
		jobwork.setBatch(batch);
		jobwork.setRemarks(request.getRemarks());
		jobwork.setJobworkOrigin(JobworkOrigin.ORIGINAL);
		jobwork.setJobworkStatus(JobworkStatus.IN_PROGRESS);
		Jobwork createdJobwork = jobworkRepository.save(jobwork);
		LOGGER.debug("Created new jobwork {}", createdJobwork.getJobworkNumber());

		JobworkItem jobworkItem = new JobworkItem();
		jobworkItem.setJobwork(createdJobwork);
		jobworkItem.setQuantity(request.getQuantity());
		jobworkItem.setJobworkItemStatus(JobworkItemStatus.IN_PROGRESS);
		jobworkItemRepository.save(jobworkItem);
		LOGGER.debug("Created new jobwork item for cutting");

		batch.setBatchStatus(BatchStatus.ASSIGNED);
		batchRepository.save(batch);
		LOGGER.debug("Batch {} status changed to ASSIGNED", batch.getSerialCode());

		JobworkResponse mappedResponse = modelMapper.map(createdJobwork, JobworkResponse.class);
		mappedResponse.setAssignedTo(createdJobwork.getAssignedTo().getName());
		mappedResponse.setBatchSerialCode(createdJobwork.getBatch().getSerialCode());
		return mappedResponse;
	}

	private JobworkResponse createItemBasedJobwork(CreateItemBasedJobworkRequest request, Employee employee,
			Batch batch) {

		Jobwork jobwork = new Jobwork();
		jobwork.setJobworkNumber(request.getJobworkNumber());
		jobwork.setJobworkType(request.getJobworkType());
		jobwork.setAssignedTo(employee);
		jobwork.setBatch(batch);
		jobwork.setRemarks(request.getRemarks());
		jobwork.setJobworkOrigin(JobworkOrigin.ORIGINAL);
		jobwork.setJobworkStatus(JobworkStatus.IN_PROGRESS);
		Jobwork createdJobwork = jobworkRepository.save(jobwork);
		LOGGER.debug("Created new jobwork {}", createdJobwork.getJobworkNumber());

		int i = 0;
		for (String itemName : request.getItemNames()) {
			Item item = this.getItemOrThrow(itemName);
			JobworkItem jobworkItem = new JobworkItem();
			jobworkItem.setJobwork(createdJobwork);
			jobworkItem.setItem(item);
			jobworkItem.setQuantity(request.getQuantities().get(i));
			jobworkItem.setJobworkItemStatus(JobworkItemStatus.IN_PROGRESS);
			jobworkItemRepository.save(jobworkItem);

			LOGGER.debug("Created new jobwork item for jobwork {}, item {}", createdJobwork.getJobworkNumber(),
					itemName);
			i += 1;
		}

		batch.setBatchStatus(BatchStatus.ASSIGNED);
		batchRepository.save(batch);
		LOGGER.debug("Batch {} status changed to ASSIGNED", batch.getSerialCode());

		JobworkResponse mappedResponse = modelMapper.map(createdJobwork, JobworkResponse.class);
		mappedResponse.setAssignedTo(createdJobwork.getAssignedTo().getName());
		mappedResponse.setBatchSerialCode(createdJobwork.getBatch().getSerialCode());
		return mappedResponse;
	}

	@Override
	public JobworkResponse closeJobwork(String jobworkNumber) {
		LOGGER.debug("Closing jobwork {}", jobworkNumber);

		Jobwork jobwork = this.getJobworkOrThrow(jobworkNumber);
		jobwork.setJobworkStatus(JobworkStatus.CLOSED);
		Jobwork savedJobwork = jobworkRepository.save(jobwork);
		LOGGER.debug("Closed jobwork {}", jobworkNumber);

		jobwork.getJobworkItems().forEach(jobworkItem -> jobworkItem.setJobworkItemStatus(JobworkItemStatus.CLOSED));

		batchService.recalculateBatchStatus(jobwork.getBatch());

		JobworkResponse mappedJobworkResponse = modelMapper.map(savedJobwork, JobworkResponse.class);
		mappedJobworkResponse.setBatchSerialCode(savedJobwork.getBatch().getSerialCode());
		mappedJobworkResponse.setAssignedTo(savedJobwork.getAssignedTo().getName());
		return mappedJobworkResponse;

	}

	@Override
	public JobworkResponse reopenJobwork(String jobworkNumber) {
		LOGGER.debug("Reopening jobwork {}", jobworkNumber);

		Jobwork jobwork = this.getJobworkOrThrow(jobworkNumber);
		jobwork.setJobworkStatus(JobworkStatus.AWAITING_CLOSE);
		Jobwork savedJobwork = jobworkRepository.save(jobwork);
		LOGGER.debug("Reopened jobwork {}", jobworkNumber);

		jobwork.getJobworkItems()
				.forEach(jobworkItem -> jobworkItem.setJobworkItemStatus(JobworkItemStatus.AWAITING_CLOSE));

		batchService.recalculateBatchStatus(jobwork.getBatch());

		JobworkResponse mappedJobworkResponse = modelMapper.map(savedJobwork, JobworkResponse.class);
		mappedJobworkResponse.setBatchSerialCode(savedJobwork.getBatch().getSerialCode());
		mappedJobworkResponse.setAssignedTo(savedJobwork.getAssignedTo().getName());
		return mappedJobworkResponse;

	}

	@Override
	public List<ItemResponse> getItemsForJobwork(String jobworkNumber) {

		Jobwork jobwork = this.getJobworkOrThrow(jobworkNumber);
		List<JobworkItem> jobworkItems = jobwork.getJobworkItems();

		List<ItemResponse> itemResponses = new ArrayList<>();

		long i = 1;
		for (JobworkItem jobworkItem : jobworkItems) {
			ItemResponse itemResponse = new ItemResponse();
			itemResponse.setId(i);
			itemResponse.setName(jobworkItem.getItem().getName());
			i += 1;
			itemResponses.add(itemResponse);
		}

		return itemResponses;
	}

	private Jobwork getJobworkOrThrow(String jobworkNumber) {
		return jobworkRepository.findByJobworkNumber(jobworkNumber).orElseThrow(() -> {
			LOGGER.error("Jobwork not found: {}", jobworkNumber);
			return new JobworkNotFoundException("Jobwork not found: " + jobworkNumber);
		});
	}

	private Employee getEmployeeOrThrow(String name) {
		return employeeRepository.findByName(name).orElseThrow(() -> {
			LOGGER.error("Employee not found: {}", name);
			return new EmployeeNotFoundException("Employee not found: " + name);
		});
	}

	private Batch getBatchOrThrow(String serialCode) {
		return batchRepository.findBySerialCode(serialCode).orElseThrow(() -> {
			LOGGER.error("Batch not found: {}", serialCode);
			return new BatchNotFoundException("Batch not found: " + serialCode);
		});
	}

	private Item getItemOrThrow(String itemName) {
		return itemRepository.findByName(itemName).orElseThrow(() -> {
			LOGGER.error("Item not found: {}", itemName);
			return new ItemNotFoundException("Item not found: " + itemName);
		});
	}

	private BatchItem getBatchItemOrThrow(String serialCode, String itemName) {
		return batchItemRepository.findByBatchSerialCodeAndItemName(serialCode, itemName).orElseThrow(() -> {
			LOGGER.error("Batch item not found: {} {}", serialCode, itemName);
			return new BatchItemNotFoundException("Batch item not found: " + serialCode + " " + itemName);
		});
	}

	@Override
	public EmployeeJobworkReportResponse getDetailedJobworksByEmployee(String employeeName, LocalDateTime startDate,
			LocalDateTime endDate) {
		LOGGER.info("Generating detailed jobwork report for employee: {} from {} to {}", employeeName, startDate, endDate);

		// 1. Build Specification for filtering
		Specification<Jobwork> spec = Specification.where(JobworkSpecification.assignedToNamesIn(List.of(employeeName)));
		if (startDate != null || endDate != null) {
			spec = spec.and(JobworkSpecification.assignedBetween(startDate, endDate));
		}

		// 2. Fetch Jobworks
		List<Jobwork> jobworks = jobworkRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
		LOGGER.debug("Found {} jobworks for filtering criteria", jobworks.size());

		if (jobworks.isEmpty()) {
			return EmployeeJobworkReportResponse.builder()
					.jobworks(new ArrayList<>())
					.stats(EmployeeJobworkReportResponse.OverallStats.builder()
							.totalJobworks(0L)
							.totalIssuedQuantity(0L)
							.totalAcceptedQuantity(0L)
							.totalDamagedQuantity(0L)
							.totalSalesQuantity(0L)
							.damageBreakdown(new HashMap<>())
							.build())
					.build();
		}

		// 3. Optimized Receipt & Damage Fetching
		List<String> jobworkNumbers = jobworks.stream().map(Jobwork::getJobworkNumber).toList();
		List<JobworkReceipt> receipts = jobworkReceiptRepository.findByJobworkJobworkNumberIn(jobworkNumbers);
		
		Map<String, List<JobworkReceiptItem>> receiptItemsByJobworkNumber = receipts.stream()
				.flatMap(r -> r.getJobworkReceiptItems().stream())
				.collect(Collectors.groupingBy(ri -> ri.getJobworkReceipt().getJobwork().getJobworkNumber()));

		// 4. Transform to Detailed Responses and Calculate Stats
		List<DetailedEmployeeJobworkResponse> detailedResponses = new ArrayList<>();
		
		long overallIssued = 0, overallAccepted = 0, overallDamaged = 0, overallSales = 0;
		Map<String, Long> overallDamageBreakdown = new HashMap<>();

		for (Jobwork jw : jobworks) {
			String jwNum = jw.getJobworkNumber();
			List<JobworkReceiptItem> riList = receiptItemsByJobworkNumber.getOrDefault(jwNum, List.of());
			
			// Group receipt items by item name for easier consolidation
			Map<String, List<JobworkReceiptItem>> riByItem = riList.stream()
					.collect(Collectors.groupingBy(ri -> ri.getItem() != null ? ri.getItem().getName() : jw.getJobworkType().name()));

			List<DetailedEmployeeJobworkResponse.ItemDetail> itemDetails = new ArrayList<>();
			
			for (JobworkItem jwi : jw.getJobworkItems()) {
				String itemName = jwi.getItem() != null ? jwi.getItem().getName() : jw.getJobworkType().name();
				List<JobworkReceiptItem> itemReceipts = riByItem.getOrDefault(itemName, List.of());
				
				long issued = jwi.getQuantity();
				long accepted = itemReceipts.stream().mapToLong(JobworkReceiptItem::getAcceptedQuantity).sum();
				long damaged = itemReceipts.stream().mapToLong(JobworkReceiptItem::getDamagedQuantity).sum();
				long sales = itemReceipts.stream().mapToLong(JobworkReceiptItem::getSalesQuantity).sum();
				
				Map<String, Long> itemDamageBreakdown = new HashMap<>();
				itemReceipts.stream()
						.flatMap(ri -> ri.getDamages().stream())
						.forEach(d -> {
							String type = d.getDamageType().name();
							itemDamageBreakdown.put(type, itemDamageBreakdown.getOrDefault(type, 0L) + d.getQuantity());
							overallDamageBreakdown.put(type, overallDamageBreakdown.getOrDefault(type, 0L) + d.getQuantity());
						});

				itemDetails.add(DetailedEmployeeJobworkResponse.ItemDetail.builder()
						.itemName(itemName)
						.issuedQuantity(issued)
						.acceptedQuantity(accepted)
						.damagedQuantity(damaged)
						.salesQuantity(sales)
						.salesPrice(itemReceipts.isEmpty() ? 0.0 : itemReceipts.get(0).getSalesPrice())
						.wagePerItem(itemReceipts.isEmpty() ? 0.0 : itemReceipts.get(0).getWagePerItem())
						.status(jwi.getJobworkItemStatus().name())
						.damageBreakdown(itemDamageBreakdown)
						.build());
				
				overallIssued += issued;
				overallAccepted += accepted;
				overallDamaged += damaged;
				overallSales += sales;
			}

			detailedResponses.add(DetailedEmployeeJobworkResponse.builder()
					.jobworkNumber(jwNum)
					.jobworkType(jw.getJobworkType().name())
					.jobworkStatus(jw.getJobworkStatus().name())
					.batchSerialCode(jw.getBatch().getSerialCode())
					.startedAt(jw.getCreatedAt())
					.lastUpdatedAt(jw.getLastModifiedAt())
					.remarks(jw.getRemarks())
					.items(itemDetails)
					.build());
		}

		EmployeeJobworkReportResponse response = EmployeeJobworkReportResponse.builder()
				.jobworks(detailedResponses)
				.stats(EmployeeJobworkReportResponse.OverallStats.builder()
						.totalJobworks((long) jobworks.size())
						.totalIssuedQuantity(overallIssued)
						.totalAcceptedQuantity(overallAccepted)
						.totalDamagedQuantity(overallDamaged)
						.totalSalesQuantity(overallSales)
						.damageBreakdown(overallDamageBreakdown)
						.build())
				.build();

		LOGGER.info("Successfully generated report with {} jobworks and {} total pieces issued", jobworks.size(), overallIssued);
		return response;
	}

	@Override
	public List<EmployeeJobworkResponse> getJobworksByEmployeeName(String employeeName) {
		LOGGER.info("Fetching all jobworks for employee: {}", employeeName);

		// Fetch all jobworks assigned to the employee
		List<Jobwork> jobworks = jobworkRepository.findByAssignedToNameOrderByCreatedAtDesc(employeeName);
		LOGGER.debug("Found {} jobworks for employee: {}", jobworks.size(), employeeName);

		if (jobworks.isEmpty()) {
			LOGGER.warn("No jobworks found for employee: {}", employeeName);
			return new ArrayList<>();
		}

		// Convert to response DTO
		List<EmployeeJobworkResponse> responses = new ArrayList<>();

		for (Jobwork jobwork : jobworks) {
			EmployeeJobworkResponse response = new EmployeeJobworkResponse();
			response.setJobworkNumber(jobwork.getJobworkNumber());
			response.setJobworkType(jobwork.getJobworkType() != null ? jobwork.getJobworkType().name() : null);
			response.setJobworkStatus(jobwork.getJobworkStatus() != null ? jobwork.getJobworkStatus().name() : null);
			response.setBatchSerialCode(jobwork.getBatch() != null ? jobwork.getBatch().getSerialCode() : null);
			response.setStartedAt(jobwork.getCreatedAt());
			response.setUpdatedAt(jobwork.getLastModifiedAt());
			response.setRemarks(jobwork.getRemarks());

			// Fetch jobwork items (pieces/items issued to this jobwork)
			List<JobworkItem> jobworkItems = jobwork.getJobworkItems();
			List<EmployeeJobworkResponse.JobworkItemDetail> itemDetails = new ArrayList<>();

			for (JobworkItem jobworkItem : jobworkItems) {
				EmployeeJobworkResponse.JobworkItemDetail itemDetail = new EmployeeJobworkResponse.JobworkItemDetail();
				itemDetail.setItemName(jobworkItem.getItem() != null ? jobworkItem.getItem().getName() : null);
				itemDetail.setQuantity(jobworkItem.getQuantity());
				itemDetails.add(itemDetail);
			}

			response.setItems(itemDetails);
			responses.add(response);

			LOGGER.debug("Processed jobwork {} with {} items", jobwork.getJobworkNumber(), itemDetails.size());
		}

		LOGGER.info("Successfully fetched {} jobworks for employee: {}", responses.size(), employeeName);
		return responses;
	}
}
