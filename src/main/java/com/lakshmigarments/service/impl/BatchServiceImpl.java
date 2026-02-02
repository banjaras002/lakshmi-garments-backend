package com.lakshmigarments.service.impl;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Date;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.lakshmigarments.context.UserContext;
import com.lakshmigarments.context.UserInfo;
import com.lakshmigarments.controller.BatchController;
import com.lakshmigarments.dto.BatchDetailDTO;
import com.lakshmigarments.dto.BatchRequestDTO;
import com.lakshmigarments.dto.BatchUpdateDTO;
import com.lakshmigarments.dto.BatchSubCategoryRequestDTO;
import com.lakshmigarments.dto.BatchTimeline;
import com.lakshmigarments.dto.BatchResponseDTO;
import com.lakshmigarments.dto.BatchSerialDTO;
import com.lakshmigarments.dto.BatchTimelineDTO;
import com.lakshmigarments.dto.BatchTimelineDetail;
import com.lakshmigarments.dto.BatchResponseDTO.BatchSubCategoryResponseDTO;
import com.lakshmigarments.interceptor.UserContextInterceptor;
import com.lakshmigarments.model.Batch;
import com.lakshmigarments.model.BatchItem;
import com.lakshmigarments.model.BatchStatus;
import com.lakshmigarments.model.BatchSubCategory;
import com.lakshmigarments.model.Category;
import com.lakshmigarments.model.Damage;
import com.lakshmigarments.model.DamageType;
import com.lakshmigarments.model.Employee;
import com.lakshmigarments.model.Inventory;
import com.lakshmigarments.model.Jobwork;
import com.lakshmigarments.model.JobworkItem;
import com.lakshmigarments.model.JobworkReceipt;
import com.lakshmigarments.model.JobworkReceiptItem;
import com.lakshmigarments.model.JobworkStatus;
import com.lakshmigarments.model.JobworkType;
import com.lakshmigarments.model.LedgerDirection;
import com.lakshmigarments.model.MaterialInventoryLedger;
import com.lakshmigarments.model.MovementType;
import com.lakshmigarments.model.ReferenceType;
import com.lakshmigarments.model.SubCategory;
import com.lakshmigarments.model.User;
import com.lakshmigarments.exception.BatchNotFoundException;
import com.lakshmigarments.exception.BatchStatusNotFoundException;
import com.lakshmigarments.exception.CategoryNotFoundException;
import com.lakshmigarments.exception.EmployeeNotFoundException;
import com.lakshmigarments.exception.InsufficientInventoryException;
import com.lakshmigarments.exception.InventoryNotFoundException;
import com.lakshmigarments.exception.SubCategoryNotFoundException;
import com.lakshmigarments.exception.UserNotFoundException;
import com.lakshmigarments.repository.BatchRepository;
import com.lakshmigarments.repository.CategoryRepository;
import com.lakshmigarments.repository.BatchSubCategoryRepository;
import com.lakshmigarments.repository.DamageRepository;
import com.lakshmigarments.repository.InventoryRepository;
import com.lakshmigarments.repository.JobworkReceiptRepository;
import com.lakshmigarments.repository.JobworkRepository;
import com.lakshmigarments.repository.MaterialLedgerRepository;
import com.lakshmigarments.repository.SubCategoryRepository;
import com.lakshmigarments.repository.BatchItemRepository;
import com.lakshmigarments.repository.UserRepository;
import com.lakshmigarments.repository.specification.BatchSpecification;
import com.lakshmigarments.service.BatchService;
import com.lakshmigarments.service.EmployeeService;
import com.lakshmigarments.service.PdfGenerator;
import com.lakshmigarments.utility.DateUtil;
import com.lakshmigarments.utility.TimeDifferenceUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BatchServiceImpl implements BatchService {

	private final PdfGenerator pdfGenerator;

//	private final EmployeeService employeeService;

	private final Logger LOGGER = LoggerFactory.getLogger(BatchServiceImpl.class);
	private final BatchRepository batchRepository;
	private final JobworkRepository jobworkRepository;
	private final JobworkReceiptRepository receiptRepository;
	private final BatchSubCategoryRepository batchSubCategoryRepository;
	private final DamageRepository damageRepository;
	private final CategoryRepository categoryRepository;
	private final SubCategoryRepository subCategoryRepository;
	private final InventoryRepository inventoryRepository;
	private final UserRepository userRepository;
	private final ModelMapper modelMapper;
	private final MaterialLedgerRepository ledgerRepository;
	private final BatchItemRepository batchItemRepository;

	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy, hh:mm a");

	@Override
	@Transactional
	public void createBatch(BatchRequestDTO batchRequestDTO) {

		Category category = categoryRepository.findByName(batchRequestDTO.getCategoryName()).orElseThrow(() -> {
			LOGGER.error("Category not found with name {}", batchRequestDTO.getCategoryName());
			return new CategoryNotFoundException("Category not found with name " + batchRequestDTO.getCategoryName());
		});

//		User user = userRepository.findById(batchRequestDTO.getCreatedByID()).orElseThrow(() -> {
//			LOGGER.error("User with ID {} not found", batchRequestDTO.getCreatedByID());
//			return new UserNotFoundException("User not found with ID " + batchRequestDTO.getCreatedByID());
//		});

		BatchStatus batchStatus = batchRequestDTO.getBatchStatus() != null ? batchRequestDTO.getBatchStatus()
				: BatchStatus.CREATED;

		List<BatchSubCategory> batchSubCategories = validateBatchSubCategories(batchRequestDTO.getSubCategories());

		Batch batch = new Batch();
		batch.setCategory(category);
		batch.setBatchStatus(batchStatus);
		batch.setSerialCode(batchRequestDTO.getSerialCode());
		batch.setIsUrgent(batchRequestDTO.getIsUrgent());
		batch.setRemarks(batchRequestDTO.getRemarks());
		batch.setAvailableQuantity(batchRequestDTO.getTotalQuantity());

		Batch createdBatch = batchRepository.save(batch);

		for (BatchSubCategory batchSubCategory : batchSubCategories) {
			batchSubCategory.setBatch(batch);
			batchSubCategory.setAvailableQuantity(batchSubCategory.getQuantity());
			batchSubCategoryRepository.save(batchSubCategory);

			// detect the quantities from inventory
			Inventory cachedInventory = inventoryRepository.findBySubCategoryNameAndCategoryName(
					batchSubCategory.getSubCategory().getName(), category.getName()).orElse(null);
			if (cachedInventory.getCount() < batchSubCategory.getQuantity()) {
				throw new InsufficientInventoryException("Stock not available");
			} else {
				cachedInventory.setCount(cachedInventory.getCount() - batchSubCategory.getQuantity());
				inventoryRepository.save(cachedInventory);
			}

			MaterialInventoryLedger inventory;
			inventory = new MaterialInventoryLedger();
			inventory.setDirection(LedgerDirection.OUT);
			inventory.setMovementType(MovementType.BATCH_CREATION);
			inventory.setReferenceType(ReferenceType.BATCH);
			inventory.setReference_id(createdBatch.getId());
			inventory.setUnit("piece(s)");
			inventory.setQuantity(batchSubCategory.getQuantity());
			inventory.setSubCategory(batchSubCategory.getSubCategory());
			inventory.setCategory(batch.getCategory());

			ledgerRepository.save(inventory);
//				inventory.setCount(inventory.getCount() - batchSubCategory.getQuantity());
//				inventoryRepository.save(inventory);

		}

		return;
	}

	@Override
	public Page<BatchResponseDTO> getAllBatches(Integer pageNo, Integer pageSize, String sortBy, String sortOrder,
			String search, List<String> batchStatusNames, List<String> categoryNames, List<Boolean> isUrgents,
			Date startDate, Date endDate) {

		if (pageNo == null) {
			pageNo = 0;
		}
		if (pageSize == null || pageSize == 0) {
			pageSize = 10;
		}
		System.out.println(sortBy + " " + sortOrder);
		Sort sort = sortOrder.equals("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

		Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

		Specification<Batch> specification = Specification
				.where(BatchSpecification.filterByBatchStatusName(batchStatusNames))
				.and(BatchSpecification.filterByCategoryName(categoryNames))
				.and(BatchSpecification.filterByIsUrgent(isUrgents));

		if (search != null && !search.isEmpty()) {
			Specification<Batch> searchSpecification = Specification.where(null);
			searchSpecification = searchSpecification.or(BatchSpecification.filterBySerialCode(search))
					.or(BatchSpecification.filterByRemarks(search));
			specification = specification.and(searchSpecification);
		}

		if (startDate != null || endDate != null) {
			specification = specification.and(BatchSpecification.filterByDateRange(startDate, endDate));
		}

		Page<Batch> batches = batchRepository.findAll(specification, pageable);

		return batches.map(this::convertToBatchResponseDTO);
	}

	@Override
	public List<BatchSerialDTO> getUnpackagedBatches() {
		LOGGER.info("Fetching unpackaged batches");
		List<Batch> unpackagedBatches = batchRepository.findAllExceptPackagedWithoutRepairableDamages();
		List<BatchSerialDTO> batchSerialDTOs = unpackagedBatches.stream()
				.map(batch -> modelMapper.map(batch, BatchSerialDTO.class)).collect(Collectors.toList());
		LOGGER.info("Found {} unpackaged batches", batchSerialDTOs.size());
		return batchSerialDTOs;
	}

	@Override
	public BatchTimeline getBatchTimeline(Long batchId) {

		Batch batch = batchRepository.findById(batchId).orElseThrow(() -> {
			LOGGER.error("Batch not found with id {}", batchId);
			return new BatchNotFoundException("Batch not found with id " + batchId);
		});
		BatchResponseDTO batchResponseDTO = convertToBatchResponseDTO(batch);
		BatchTimeline batchTimeline = new BatchTimeline();
		batchTimeline.setBatchDetails(batchResponseDTO);

		List<BatchTimelineDetail> timelineDetails = new ArrayList<>();

		// GET details of batch if discarded
		if (batch.getBatchStatus() == BatchStatus.DISCARDED) {
			BatchTimelineDetail timelineDetail = new BatchTimelineDetail();
			timelineDetail.setPerformedAt(batch.getLastModifiedAt());
//			timelineDetail.setPerformedBy(batch.getUpdatedBy().getName());
			timelineDetail.setMessage("Batch discarded by " + batch.getLastModifiedBy() + " at "
					+ batch.getLastModifiedAt().format(formatter));

			String timeTaken = TimeDifferenceUtil.formatDuration(batch.getCreatedAt(), batch.getLastModifiedAt());
			timelineDetail.setTimeTakenFromPrevious(timeTaken);
			timelineDetail.setStage(BatchStatus.DISCARDED.toString());
			batchTimeline.setTimelineDetail(Arrays.asList(timelineDetail));
			return batchTimeline;
		}

		// GET the JOBWORKS and JOBWORK RECEIPTS IF ANY
		List<Jobwork> jobworks = jobworkRepository.findByBatchSerialCode(batch.getSerialCode());
		List<JobworkReceipt> jobworkReceipts = receiptRepository.findByJobworkBatchSerialCode(batch.getSerialCode());

		jobworks.sort(Comparator.comparing(Jobwork::getCreatedAt));
		jobworkReceipts.sort(Comparator.comparing(JobworkReceipt::getCreatedAt));
		System.out.println(jobworks.size());
		int i = 0, j = 0;
		while (i < jobworks.size() && j < jobworkReceipts.size()) {
			Jobwork jw = jobworks.get(i);
			JobworkReceipt jwr = jobworkReceipts.get(j);

			BatchTimelineDetail timelineDetail = new BatchTimelineDetail();
			if (jw.getCreatedAt().isBefore(jwr.getCreatedAt())) {
				timelineDetail = processJobworkToDetail(jw, timelineDetails, batch);
				i++;
			} else {
				timelineDetail = processJobworkReceiptToDetail(jwr, timelineDetails, batch);
				j++;
			}
			timelineDetails.add(timelineDetail);
		}

		batchTimeline.setTimelineDetail(timelineDetails);

		// Process remaining items if any
		while (i < jobworks.size()) {
			Jobwork jw = jobworks.get(i);
			BatchTimelineDetail timelineDetail = processJobworkToDetail(jw, timelineDetails, batch);
			timelineDetails.add(timelineDetail);
			i++;
		}
		while (j < jobworkReceipts.size()) {
			JobworkReceipt jwr = jobworkReceipts.get(j);
			BatchTimelineDetail timelineDetail = processJobworkReceiptToDetail(jwr, timelineDetails, batch);
			timelineDetails.add(timelineDetail);
			j++;
		}

		return batchTimeline;
	}

	protected BatchTimelineDetail processJobworkToDetail(Jobwork jw, List<BatchTimelineDetail> timelineDetails,
			Batch batch) {
		BatchTimelineDetail timelineDetail = new BatchTimelineDetail();
//		timelineDetail.setAssignedBy(jw.getAssignedBy().getName());
//        long totalQuantity = jw.getJobworkItems().stream().mapToLong(JobworkItem::getQuantity).sum();
		String message = "Batch assigned to " + jw.getAssignedTo().getName() + " at "
				+ jw.getCreatedAt().format(formatter) + " by " + jw.getCreatedBy();
		timelineDetail.setMessage(message);
		timelineDetail.setPerformedAt(jw.getCreatedAt());
//        timelineDetail.setPerformedBy(jw.getEmployee().getName());
		timelineDetail.setStage(jw.getJobworkStatus().toString());

		String timeTaken;
		if (timelineDetails.isEmpty()) {
			timeTaken = TimeDifferenceUtil.formatDuration(batch.getCreatedAt(), jw.getCreatedAt());
			timelineDetail.setTimeTakenFromPrevious(timeTaken);
		} else {
			BatchTimelineDetail previousTimelineDetail = timelineDetails.get(timelineDetails.size() - 1);
			timeTaken = TimeDifferenceUtil.formatDuration(previousTimelineDetail.getPerformedAt(), jw.getCreatedAt());
			timelineDetail.setTimeTakenFromPrevious(timeTaken);
		}
		timelineDetail.setTimeTakenFromPrevious(timeTaken);
		return timelineDetail;
	}

	protected BatchTimelineDetail processJobworkReceiptToDetail(JobworkReceipt jwr,
			List<BatchTimelineDetail> timelineDetails, Batch batch) {
		BatchTimelineDetail timelineDetail = new BatchTimelineDetail();
		String message = "Batch returned by " + " at " + jwr.getCreatedAt().format(formatter) + " to "
				+ jwr.getCreatedBy();
		timelineDetail.setMessage(message);
		timelineDetail.setStage("SUBMITTED");
		timelineDetail.setPerformedAt(jwr.getCreatedAt());
		String timeTaken;
		if (timelineDetails.isEmpty()) {
			timeTaken = TimeDifferenceUtil.formatDuration(batch.getCreatedAt(), jwr.getCreatedAt());
			timelineDetail.setTimeTakenFromPrevious(timeTaken);
		} else {
			BatchTimelineDetail previousTimelineDetail = timelineDetails.get(timelineDetails.size() - 1);
			timeTaken = TimeDifferenceUtil.formatDuration(previousTimelineDetail.getPerformedAt(), jwr.getCreatedAt());
			timelineDetail.setTimeTakenFromPrevious(timeTaken);
		}
		return timelineDetail;
	}

//	@Override
//	public Long getBatchCount(Long batchId) {
//		List<BatchSubCategory> batchSubCategories = batchSubCategoryRepository.findByBatchId(batchId);
//		System.out.println(batchSubCategories.size());
//
//		List<Damage> damages = damageRepository.findAllByBatchId(batchId);
//		return batchSubCategories.stream().mapToLong(BatchSubCategory::getQuantity).sum()
//				- damages.stream().mapToLong(Damage::getQuantity).sum();
//	}

	private List<BatchSubCategory> validateBatchSubCategories(List<BatchSubCategoryRequestDTO> batchSubCategories) {
		List<BatchSubCategory> validatedBatchSubCategories = new ArrayList<>();
		for (BatchSubCategoryRequestDTO batchSubCategoryRequestDTO : batchSubCategories) {
			SubCategory subCategory = subCategoryRepository.findByName(batchSubCategoryRequestDTO.getSubCategoryName())
					.orElseThrow(() -> {
						LOGGER.error("Sub category not found with id {}",
								batchSubCategoryRequestDTO.getSubCategoryName());
						return new SubCategoryNotFoundException(
								"Sub category not found with name " + batchSubCategoryRequestDTO.getSubCategoryName());
					});
			BatchSubCategory batchSubCategory = new BatchSubCategory();
			batchSubCategory.setSubCategory(subCategory);
			batchSubCategory.setQuantity(batchSubCategoryRequestDTO.getQuantity());
			validatedBatchSubCategories.add(batchSubCategory);
		}
		return validatedBatchSubCategories;
	}

	// map the subcategories to the batch response dto
	private BatchResponseDTO convertToBatchResponseDTO(Batch batch) {
		BatchResponseDTO batchResponseDTO = modelMapper.map(batch, BatchResponseDTO.class);
		List<BatchSubCategory> batchSubCategories = batchSubCategoryRepository.findByBatchId(batch.getId());
		List<BatchSubCategoryResponseDTO> batchSubCategoryResponseDTOs = batchSubCategories.stream()
				.map(batchSubCategory -> modelMapper.map(batchSubCategory, BatchSubCategoryResponseDTO.class))
				.collect(Collectors.toList());
		batchResponseDTO.setSubCategories(batchSubCategoryResponseDTOs);

		List<BatchItem> batchItems = batchItemRepository.findByBatchId(batch.getId());
		List<BatchResponseDTO.BatchItemResponse> itemResponseDTOs = batchItems.stream().map(batchItem -> {
			BatchResponseDTO.BatchItemResponse itemResponse = new BatchResponseDTO.BatchItemResponse();
			itemResponse.setId(batchItem.getId());
			itemResponse.setItemName(batchItem.getItem() != null ? batchItem.getItem().getName() : null);
			itemResponse.setQuantity(batchItem.getQuantity());
			return itemResponse;
		}).collect(Collectors.toList());
		batchResponseDTO.setItems(itemResponseDTOs);

		return batchResponseDTO;
	}

	@Override
	@Transactional
	public void updateBatch(Long batchId, BatchUpdateDTO batchUpdateDTO) {
		Batch batch = batchRepository.findById(batchId).orElseThrow(() -> {
			LOGGER.error("Batch not found with id {}", batchId);
			return new BatchNotFoundException("Batch not found with id " + batchId);
		});

		if (batchUpdateDTO.getSerialCode() != null) {
			batch.setSerialCode(batchUpdateDTO.getSerialCode());
		}
		if (batchUpdateDTO.getCategoryName() != null) {
			Category category = categoryRepository.findByName(batchUpdateDTO.getCategoryName()).orElseThrow(() -> {
				LOGGER.error("Category not found with name {}", batchUpdateDTO.getCategoryName());
				return new CategoryNotFoundException(
						"Category not found with name " + batchUpdateDTO.getCategoryName());
			});
			batch.setCategory(category);
		}
		if (batchUpdateDTO.getIsUrgent() != null) {
			batch.setIsUrgent(batchUpdateDTO.getIsUrgent());
		}
		if (batchUpdateDTO.getRemarks() != null) {
			batch.setRemarks(batchUpdateDTO.getRemarks());
		}
		if (batchUpdateDTO.getSubCategories() != null) {
			List<BatchSubCategory> batchSubCategories = validateBatchSubCategories(batchUpdateDTO.getSubCategories());
			for (BatchSubCategory batchSubCategory : batchSubCategories) {
				batchSubCategory.setBatch(batch);
				batchSubCategoryRepository.save(batchSubCategory);
			}
		}
		if (batchUpdateDTO.getBatchStatusName() != null) {
			try {
				BatchStatus batchStatus = BatchStatus.valueOf(batchUpdateDTO.getBatchStatusName().toUpperCase());

				if (batchStatus == BatchStatus.DISCARDED) {
					batch.setBatchStatus(batchStatus);
				}

			} catch (IllegalArgumentException ex) {
				LOGGER.error("Invalid batch status {}", batchUpdateDTO.getBatchStatusName());
				throw new BatchStatusNotFoundException(
						"Batch status not found with name " + batchUpdateDTO.getBatchStatusName());
			}
		}

		batchRepository.save(batch);
	}

	// TODO
	@Override
	public List<JobworkType> getAllowedJobworkTypes(String batchSerialCode) {
		List<JobworkType> allowedJobworkTypes = new ArrayList<>();
		Batch batch = this.getBatchOrThrow(batchSerialCode);

		// logic to include CUTTING
		LOGGER.debug("Fetching available quantities for cutting for batch {}", batchSerialCode);
		Long availableQuantity = this.getAvailableQuantitiesForCutting(batchSerialCode);
		if (availableQuantity > 0) {
			LOGGER.debug("Cutting allowed for batch {}", batchSerialCode);
			allowedJobworkTypes.add(JobworkType.CUTTING);
		}

		// conditions for adding stitching
		List<JobworkReceipt> cuttingJobworkReceipts = receiptRepository
				.findByJobworkBatchSerialCodeAndJobworkJobworkType(batchSerialCode, JobworkType.CUTTING);
		LOGGER.debug("Fetched {} jobwork receipts for CUTTING of batch {}", cuttingJobworkReceipts.size(),
				batchSerialCode);

		Long totalAcceptedQuantity = !cuttingJobworkReceipts.isEmpty() ? cuttingJobworkReceipts.stream()
				.flatMap(receipt -> receipt.getJobworkReceiptItems().stream())
				.map(JobworkReceiptItem::getAcceptedQuantity).filter(Objects::nonNull).mapToLong(Long::longValue).sum()
				: 0L;
		LOGGER.debug("Accepted quantities received for batch {} from CUTTING jobs : {}", batchSerialCode,
				totalAcceptedQuantity);

		// conditions for removing stitching
		Long assignedStitchingQuantities = jobworkRepository.getAssignedQuantities(batchSerialCode,
				JobworkType.STITCHING.name());
		Long damagedRepairableStitchingQuantities = damageRepository.getDamagedQuantity(batchSerialCode,
				DamageType.REPAIRABLE.name(), JobworkType.STITCHING.name());
		long availableForStitching = totalAcceptedQuantity - assignedStitchingQuantities
				+ damagedRepairableStitchingQuantities;

		if (availableForStitching > 0) {
			allowedJobworkTypes.add(JobworkType.STITCHING);
		}

		// conditions for adding stitching
		List<JobworkReceipt> stitchingJobworkReceipts = receiptRepository
				.findByJobworkBatchSerialCodeAndJobworkJobworkType(batchSerialCode, JobworkType.STITCHING);
		LOGGER.debug("Fetched {} jobwork receipts for STITCHING of batch {}", stitchingJobworkReceipts.size(),
				batchSerialCode);

		Long totalAcceptedQuantityForStitching = !stitchingJobworkReceipts.isEmpty() ? stitchingJobworkReceipts.stream()
				.flatMap(receipt -> receipt.getJobworkReceiptItems().stream())
				.map(JobworkReceiptItem::getAcceptedQuantity).filter(Objects::nonNull).mapToLong(Long::longValue).sum()
				: 0L;
		LOGGER.debug("Accepted quantities received for batch {} from STITCHING jobs : {}", batchSerialCode,
				totalAcceptedQuantityForStitching);

		if (totalAcceptedQuantityForStitching > 0) {
			allowedJobworkTypes.add(JobworkType.PACKAGING);
		}

		return allowedJobworkTypes;
	}

	// mark the batch as discarded in batch status and refill inventory
	@Override
	public void recycleBatch(Long batchId) {

		UserInfo userInfo = UserContext.get();

//		User user = userRepository.findById(Long.valueOf(userInfo.getUserId())).orElseThrow(() -> {
//			LOGGER.error("User with ID {} not found", userInfo.getUserId());
//			return new UserNotFoundException("User not found with ID " + userInfo.getUserId());
//		});
//		
		Batch batch = batchRepository.findById(batchId).orElseThrow(() -> {
			LOGGER.error("Batch not found with id {}", batchId);
			return new BatchNotFoundException("Batch not found with id " + batchId);
		});

		if (batch.getBatchStatus() == BatchStatus.DISCARDED) {
			LOGGER.info("Batch already recyles");
			return;
		}

		List<BatchSubCategory> batchSubCategories = batchSubCategoryRepository.findByBatchId(batchId);

		long categoryId = batch.getCategory().getId();

		List<Inventory> validInventories = new ArrayList<>();
		for (BatchSubCategory batchSubCategory : batchSubCategories) {
			batchSubCategory.setAvailableQuantity(0L);
			boolean isInventoryValid = inventoryRepository.existsByCategoryIdAndSubCategoryId(categoryId,
					batchSubCategory.getSubCategory().getId());
			if (isInventoryValid) {
				Inventory inventory = inventoryRepository
						.findByCategoryIdAndSubCategoryId(categoryId, batchSubCategory.getSubCategory().getId())
						.orElseThrow(() -> {
							LOGGER.error("Inventory not found with category ID {}", categoryId);
							return new InventoryNotFoundException("Inventory not found with category id " + categoryId);
						});
				long countAfterRecycle = inventory.getCount() + batchSubCategory.getQuantity();
				inventory.setCount(countAfterRecycle);
				validInventories.add(inventory);
			}

			MaterialInventoryLedger inventory;
			inventory = new MaterialInventoryLedger();
			inventory.setDirection(LedgerDirection.IN);
			inventory.setMovementType(MovementType.BATCH_RECYLE);
			inventory.setReferenceType(ReferenceType.BATCH);
			inventory.setReference_id(batch.getId());
			inventory.setUnit("piece(s)");
			inventory.setQuantity(batchSubCategory.getQuantity());
			inventory.setSubCategory(batchSubCategory.getSubCategory());
			inventory.setCategory(batch.getCategory());

			ledgerRepository.save(inventory);
		}
		for (Inventory inventory : validInventories) {
			inventoryRepository.save(inventory);
		}
		batch.setBatchStatus(BatchStatus.DISCARDED);
		batch.setAvailableQuantity(0L);
		batchRepository.save(batch);
	}

	@Override
	public List<BatchDetailDTO> getBatchDetails(Long batchId) {

		Batch batch = batchRepository.findById(batchId).orElseThrow(() -> {
			LOGGER.error("Batch not found with id {}", batchId);
			return new BatchNotFoundException("Batch not found with id " + batchId);
		});

		BatchDetailDTO batchDetailDTO = new BatchDetailDTO();
		batchDetailDTO.setBatchSerialCode(batch.getSerialCode());

		// FETCH all batch damages

		return null;
	}

	@Override
	public Long getAvailableQuantities(String serialCode, String jobworkType) {

		Batch batch = batchRepository.findBySerialCode(serialCode).orElseThrow(() -> {
			LOGGER.error("Batch not found with serial {}", serialCode);
			return new BatchNotFoundException("Batch not found with serial " + serialCode);
		});

		long totalQuantities = batchRepository.findQuantityBySerialCode(serialCode);

		// fetch jobwork receipts for the batch logic for CUTTING
		List<JobworkReceipt> receipts = receiptRepository.findByJobworkBatchSerialCodeAndJobworkJobworkType(serialCode,
				JobworkType.valueOf(jobworkType));
		List<Jobwork> jobworks = jobworkRepository.findByBatchSerialCodeAndJobworkStatusIn(serialCode,
				Arrays.asList(JobworkStatus.IN_PROGRESS, JobworkStatus.REASSIGNED));

		// subtract quantities from ongoing jobworks yet to be submitted
		for (Jobwork jobwork : jobworks) {
			List<JobworkItem> jobworkItems = jobwork.getJobworkItems();
			for (JobworkItem jobworkItem : jobworkItems) {
				totalQuantities -= jobworkItem.getQuantity();
			}

		}

		// subtract quantities from submitted jobworks
		for (JobworkReceipt jobworkReceipt : receipts) {
			List<JobworkReceiptItem> receiptItems = jobworkReceipt.getJobworkReceiptItems();
			for (JobworkReceiptItem receiptItem : receiptItems) {
				totalQuantities -= (receiptItem.getAcceptedQuantity() + receiptItem.getSalesQuantity());

			}

			long totalDamages = jobworkReceipt.getJobworkReceiptItems().stream()
					.flatMap(item -> item.getDamages().stream()) // flatten all damage lists
					.filter(damage -> damage.getDamageType() != DamageType.REPAIRABLE).mapToLong(Damage::getQuantity)
					.sum();

			totalQuantities -= totalDamages;

		}

		return totalQuantities;
	}

	@Override
	public Long getAvailableQuantitiesForCutting(String serialCode) {
		LOGGER.debug("Fetching available quantities for CUTTING for batch {}", serialCode);

		Batch batch = this.getBatchOrThrow(serialCode);

		Long assignedJobworksQuantity = jobworkRepository.getAssignedQuantities(serialCode, JobworkType.CUTTING.name());
		Long damagedQuantity = damageRepository.getDamagedQuantity(serialCode, DamageType.REPAIRABLE.name(),
				JobworkType.CUTTING.name());
		Long batchQuantity = batchRepository.findQuantityBySerialCode(batch.getSerialCode());
		LOGGER.debug("Assigned Quantities {}, Repairable quantities {}, Total batch quantity {}",
				assignedJobworksQuantity, damagedQuantity, batchQuantity);

		Long availableQuantitiesForCutting = batchQuantity - assignedJobworksQuantity + damagedQuantity;
		LOGGER.debug("Available quantities for cutting work for batch {} is {}", serialCode,
				availableQuantitiesForCutting);
		if (availableQuantitiesForCutting < 0) {
			return 0L;
		}

		return availableQuantitiesForCutting;
	}

	@Override
	public List<String> getBatchSerialCodesForJobwork() {
		LOGGER.debug("Fetching batch serial codes that are available for jobworks");
		List<String> batchSerialCodes = batchRepository.findAllBatchSerialCodesForJobwork();
		return batchSerialCodes;
	}

	public void recalculateBatchStatus(Batch batch) {

		List<Jobwork> jobworks = jobworkRepository.findByBatch(batch);

		if (jobworks.isEmpty()) {
			batch.setBatchStatus(BatchStatus.CREATED);
			LOGGER.debug("Marked the status of batch {} as {}", batch.getSerialCode(), BatchStatus.CREATED);
		} else if (jobworks.stream().anyMatch(jw -> jw.getJobworkStatus() == JobworkStatus.IN_PROGRESS
				|| jw.getJobworkStatus() == JobworkStatus.AWAITING_CLOSE)) {
			batch.setBatchStatus(BatchStatus.ASSIGNED);
			LOGGER.debug("Marked the status of batch {} as {}", batch.getSerialCode(), BatchStatus.ASSIGNED);
		} else if (jobworks.stream().allMatch(jw -> jw.getJobworkStatus() == JobworkStatus.CLOSED
				|| jw.getJobworkStatus() == JobworkStatus.REASSIGNED)) {
			batch.setBatchStatus(BatchStatus.COMPLETED);
			LOGGER.debug("Marked the status of batch {} as {}", batch.getSerialCode(), BatchStatus.COMPLETED);
		}

		batchRepository.save(batch);
	}

//	private Employee getEmployeeOrThrow(String name) {
//		return employeeRepository.findByName(name).orElseThrow(() -> {
//			LOGGER.error("Employee not found: {}", name);
//			return new EmployeeNotFoundException("Employee not found: " + name);
//		});
//	}

	private Batch getBatchOrThrow(String serialCode) {
		return batchRepository.findBySerialCode(serialCode).orElseThrow(() -> {
			LOGGER.error("Batch not found: {}", serialCode);
			return new BatchNotFoundException("Batch not found: " + serialCode);
		});
	}

	@Override
	public List<String> getAllBatchSerialCode() {
		LOGGER.debug("Fetchging all the batch serial codes");
		return batchRepository.getAllBatchSerialCodes();
	}

}
