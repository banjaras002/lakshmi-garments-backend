package com.lakshmigarments.service.impl;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.cors.CorsConfigurationSource;

import com.lakshmigarments.dto.request.CreateJobworkReceiptRequest;
import com.lakshmigarments.configuration.SecurityConfig;
import com.lakshmigarments.controller.BatchController;
import com.lakshmigarments.controller.StockController;
import com.lakshmigarments.dto.request.CreateDamageRequest;
import com.lakshmigarments.dto.request.CreateJobworkReceiptItemRequest;
import com.lakshmigarments.exception.BatchNotFoundException;
import com.lakshmigarments.exception.InvalidJobworkItemException;
import com.lakshmigarments.exception.ItemNotFoundException;
import com.lakshmigarments.exception.JobworkClosedException;
import com.lakshmigarments.exception.JobworkItemNotFoundException;
import com.lakshmigarments.exception.JobworkNotFoundException;
import com.lakshmigarments.exception.UserNotFoundException;
import com.lakshmigarments.model.Batch;
import com.lakshmigarments.model.BatchItem;
import com.lakshmigarments.model.BatchStatus;
import com.lakshmigarments.model.Damage;
import com.lakshmigarments.model.DamageType;
import com.lakshmigarments.model.Item;
import com.lakshmigarments.model.Jobwork;
import com.lakshmigarments.model.JobworkItem;
import com.lakshmigarments.model.JobworkItemStatus;
import com.lakshmigarments.model.JobworkReceipt;
import com.lakshmigarments.model.JobworkReceiptItem;
import com.lakshmigarments.model.JobworkStatus;
import com.lakshmigarments.model.JobworkType;
import com.lakshmigarments.model.User;
import com.lakshmigarments.repository.*;
import com.lakshmigarments.service.BatchService;
import com.lakshmigarments.service.JobworkReceiptService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobworkReceiptServiceImpl implements JobworkReceiptService {

    private final BatchController batchController;

    private final StockController stockController;

    private final SecurityConfig securityConfig;

	private final DamageRepository damageRepository;

	private final JobworkReceiptRepository jobworkReceiptRepository;

	private static final Logger LOGGER = LoggerFactory.getLogger(JobworkReceiptServiceImpl.class);
	private final JobworkRepository jobworkRepository;
	private final BatchRepository batchRepository;
	private final UserRepository userRepository;
	private final JobworkReceiptItemRepository receiptItemRepository;
	private final ItemRepository itemRepository;
	private final JobworkItemRepository jobworkItemRepository;
	private final BatchItemRepository batchItemRepository;
	private final BatchService batchService;
	
	@Override
	@Transactional
	public void createJobworkReceipt(CreateJobworkReceiptRequest jobworkReceiptRequest) {
		LOGGER.debug("Creating jobwork receipt");
		
		List<String> itemNames = jobworkItemRepository.findItemNamesByJobworkNumber(jobworkReceiptRequest.getJobworkNumber());
		LOGGER.debug("Items issued for this jobwork {}", itemNames);
		
		Jobwork jobwork = this.getJobworkOrThrow(jobworkReceiptRequest.getJobworkNumber());
		if (jobwork.getJobworkStatus() == JobworkStatus.CLOSED) {
			LOGGER.error("Jobwork already closed.");
			throw new JobworkClosedException("Jobwork already closed");
		}
//		Batch batch = this.getBatchOrThrow(jobwork.getBatch().getSerialCode());
		
		JobworkReceipt jobworkReceipt = new JobworkReceipt();
		jobworkReceipt.setJobwork(jobwork);
		JobworkReceipt createdJobworkReceipt = jobworkReceiptRepository.save(jobworkReceipt);
		LOGGER.debug("Created temporary jobwork receipt");
		
		long totalQuantitiesAccountedForJobwork = 0L;
		for (CreateJobworkReceiptItemRequest receiptItemRequest : jobworkReceiptRequest.getJobworkReceiptItems()) {
			
			if (jobwork.getJobworkType() != JobworkType.CUTTING && !itemNames.contains(receiptItemRequest.getItemName())) {
				LOGGER.error("Item {} not issued for this jobwork: {}", 
						receiptItemRequest.getItemName(), jobworkReceiptRequest.getJobworkNumber());
				throw new InvalidJobworkItemException("Item " + receiptItemRequest.getItemName() 
					+" not issued for this jobwork: " + jobworkReceiptRequest.getJobworkNumber());
			}
			
			LOGGER.debug("Processing the receipt request item {}", receiptItemRequest.getItemName());
			
			Item item = this.getItemOrThrow(receiptItemRequest.getItemName());
			
			JobworkReceiptItem jobworkReceiptItem = new JobworkReceiptItem();
			jobworkReceiptItem.setAcceptedQuantity(receiptItemRequest.getAcceptedQuantity());
			jobworkReceiptItem.setSalesQuantity(receiptItemRequest.getSalesQuantity());
			jobworkReceiptItem.setSalesPrice(receiptItemRequest.getSalesPrice());
			jobworkReceiptItem.setItem(item);
			jobworkReceiptItem.setWagePerItem(receiptItemRequest.getWagePerItem());
			jobworkReceiptItem.setJobworkReceipt(createdJobworkReceipt);
			totalQuantitiesAccountedForJobwork += receiptItemRequest.getAcceptedQuantity() +
					receiptItemRequest.getSalesQuantity();
			
			int totalDamagesForItem = 0;
			if (receiptItemRequest.getDamages() == null || receiptItemRequest.getDamages().isEmpty()) {
				LOGGER.debug("No damages for jobwork receipt item {}", receiptItemRequest.getItemName());
			} else {
				LOGGER.debug("Processing damages for jobwork receipt item {}", receiptItemRequest.getItemName());
				for (CreateDamageRequest damageRequest : receiptItemRequest.getDamages()) {
					boolean isDamagedForDamageType = damageRequest.getQuantity() == 0 ? false : true;
					if (isDamagedForDamageType) {
						Damage damage = new Damage();
						damage.setJobworkReceipt(createdJobworkReceipt);
						damage.setQuantity(damageRequest.getQuantity());
						damage.setDamageType(DamageType.valueOf(damageRequest.getType()));
						damageRepository.save(damage);
						
						totalDamagesForItem += damageRequest.getQuantity();
					}
				}
				LOGGER.debug("Total damages for receipt item {}", totalDamagesForItem);
			}
			totalQuantitiesAccountedForJobwork += totalDamagesForItem;
			jobworkReceiptItem.setDamagedQuantity(Long.valueOf(totalDamagesForItem));
			receiptItemRepository.save(jobworkReceiptItem);
			LOGGER.debug("Created jobwork receipt item {}", receiptItemRequest.getItemName());
			
			JobworkItem jobworkItem = jobworkItemRepository
					.findByItemNameAndJobworkJobworkNumber(receiptItemRequest.getItemName(), 
							jobworkReceiptRequest.getJobworkNumber()).orElse(null);
			if (jobwork.getJobworkType() != JobworkType.CUTTING) {
				LOGGER.debug("Computing current status of jobwork item {}", receiptItemRequest.getItemName());
				long totalQuantityAccountedForItem = receiptItemRequest.getAcceptedQuantity() + 
						receiptItemRequest.getSalesQuantity() + totalDamagesForItem;
				long totalIssuedQuantityForItem = jobworkItem.getQuantity();
				if (totalIssuedQuantityForItem == totalQuantityAccountedForItem) {	
					jobworkItem.setJobworkItemStatus(JobworkItemStatus.CLOSED);
					LOGGER.debug("Marked the status of jobwork item {} to COMPLETED", receiptItemRequest.getItemName());
				}
			} else {
				jobworkItem = jobwork.getJobworkItems().get(0);
				jobworkItem.setJobworkItemStatus(JobworkItemStatus.AWAITING_CLOSE);
				LOGGER.debug("Marked the status of jobwork item 'CUTTING' to AWAITING_CLOSE");
			}
			jobworkItemRepository.save(jobworkItem);
			
			// create batch items or update if CUTTING
			if (jobwork.getJobworkType() == JobworkType.CUTTING) {
				BatchItem batchItem = batchItemRepository.findByBatchIdAndItemName(
						jobwork.getBatch().getId(), receiptItemRequest.getItemName()).orElse(null);
				if (batchItem != null) {
					batchItem.setQuantity(batchItem.getQuantity() + receiptItemRequest.getAcceptedQuantity());
					LOGGER.debug("Updating batch item quantity {} for batch {} item {}", 
							batchItem.getQuantity() + receiptItemRequest.getAcceptedQuantity(), jobwork.getBatch().getSerialCode(), receiptItemRequest.getItemName());
				} else {
					batchItem = new BatchItem();
					batchItem.setBatch(jobwork.getBatch());
					batchItem.setItem(item);
					batchItem.setQuantity(receiptItemRequest.getAcceptedQuantity());
					LOGGER.debug("Created a new batch {} item {}", jobwork.getBatch().getSerialCode(), receiptItemRequest.getItemName());
				}
				batchItemRepository.save(batchItem);
			}
		}
		
		Long totalIssuedQuantityForJobwork = jobworkRepository.findTotalQuantities(jobwork.getJobworkNumber());
		if (totalIssuedQuantityForJobwork == totalQuantitiesAccountedForJobwork && 
				jobwork.getJobworkType() != JobworkType.CUTTING) {
			jobwork.setJobworkStatus(JobworkStatus.CLOSED);
			LOGGER.debug("Marked status of jobwork {} as CLOSED");
			
//			Batch batch = this.getBatchOrThrow(jobwork.getBatch().getSerialCode());
//			batch.setBatchStatus(BatchStatus.COMPLETED);
//			batchRepository.save(batch);
//			LOGGER.debug("Marked status of batch {} as COMPLETED", batch.getSerialCode());
			// TODO consider the case of closing the batch if type is packaging and last of its items
			// TODO check if any other jobs are dealing with this batch incomplete
			
		} else {
			jobwork.setJobworkStatus(JobworkStatus.PENDING_RETURN);
			LOGGER.debug("Marked the status of the jobwork {} to PENDING_RETURN");
			if (jobwork.getJobworkType() == JobworkType.CUTTING) {
				jobwork.setJobworkStatus(JobworkStatus.AWAITING_CLOSE);
				LOGGER.debug("Marked the status of the jobwork {} to AWAITING_CLOSE");
			}
			
		}
		Jobwork savedJobwork = jobworkRepository.save(jobwork);
		batchService.recalculateBatchStatus(savedJobwork.getBatch());
	}
	
	
		
	private Jobwork getJobworkOrThrow(String jobworkNumber) {
		return jobworkRepository.findByJobworkNumber(jobworkNumber).orElseThrow(() -> {
			LOGGER.error("Jobwork not found: {}", jobworkNumber);
			return new JobworkNotFoundException("Jobwork not found: " + jobworkNumber);
		});
	}
	
	private Item getItemOrThrow(String itemName) {
		return itemRepository.findByName(itemName).orElseThrow(() -> {
			LOGGER.error("Item not found: {}", itemName);
			return new ItemNotFoundException("Item not found: " + itemName);
		});
	}
	
	private Batch getBatchOrThrow(String serialCode) {
		return batchRepository.findBySerialCode(serialCode).orElseThrow(() -> {
			LOGGER.error("Batch not found: {}", serialCode);
			return new BatchNotFoundException("Batch not found: " + serialCode);
		});
	}
//	
//	private Batch getBatchItemOrThrow(String serialCode) {
//		return batchRepository.findBySerialCode(serialCode).orElseThrow(() -> {
//			LOGGER.error("Batch not found: {}", serialCode);
//			return new BatchNotFoundException("Batch not found: " + serialCode);
//		});
//	}

//	@Override
//	@Transactional
//	public void createJobworkReceipt(CreateJobworkReceiptRequest jobworkReceipt) {
//
////		String batchId = jobworkReceipt.getBatchSerialCode();
//		String jobworkNumber = jobworkReceipt.getJobworkNumber();
////		Long receivedById = jobworkReceipt.getReceivedById();
//		List<JobworkReceiptItemDTO> jobworkReceiptItemDTOs = jobworkReceipt.getJobworkReceiptItems();
//
//		Jobwork jobwork = jobworkRepository.findByJobworkNumber(jobworkNumber).orElseThrow(() -> {
//			LOGGER.error("Jobwork with number {} not found", jobworkNumber);
//			return new JobworkNotFoundException("Jobwork with number " + jobworkNumber + " not found");
//		});
//
////		User user = userRepository.findById(receivedById).orElseThrow(() -> {
////			LOGGER.error("User with ID {} not found", receivedById);
////			return new UserNotFoundException("User not found with ID " + receivedById);
////		});
//
//		Batch batch = batchRepository.findBySerialCode("2").orElseThrow(() -> {
//			LOGGER.error("Batch not found with id {}", batchId);
//			return new BatchNotFoundException("Batch not found with id " + batchId);
//		});
//
//		JobworkReceipt newJobworkReceipt = new JobworkReceipt();
//		newJobworkReceipt.setCompletedBy(jobwork.getAssignedTo());
//		newJobworkReceipt.setJobwork(jobwork);
////		newJobworkReceipt.setReceivedBy(user);
//		JobworkReceipt createdJobworkReceipt = jobworkReceiptRepository.save(newJobworkReceipt);
//
//		long itemCount = 0, totalReceivedQuantity = 0;
//		long totalQuantityForJobwork = jobworkRepository.findTotalQuantities(jobworkNumber);
//		
//		for (JobworkReceiptItemDTO jobworkReceiptItemDTO : jobworkReceiptItemDTOs) {
//			String itemName = jobworkReceiptItemDTO.getItemName();
//			Item existingItem = itemRepository.findByName(itemName).orElseThrow(() -> {
//				LOGGER.error("Item not found with name {}", itemName);
//				return new ItemNotFoundException("Item not found with name " + itemName);
//			});
//
//			itemCount += jobworkReceiptItemDTO.getReturnedQuantity();
//
//			JobworkItem jobworkItem;
//			if (jobwork.getJobworkType() == JobworkType.CUTTING) {
//				jobworkItem = jobworkItemRepository.findByJobworkJobworkNumber(jobworkNumber).orElseThrow(() -> {
//					LOGGER.error("JobworkItem with jobwork number {} not found", jobworkNumber);
//					return new JobworkItemNotFoundException(
//							"JobworkItem not found with jobwork number " + jobworkNumber);
//				});
//			} else {
//				jobworkItem = jobworkItemRepository.findByJobworkJobworkNumberAndItem(jobworkNumber, existingItem)
//						.orElseThrow(() -> {
//							LOGGER.error("JobworkItem with jobwork number {} and item {} not found", jobworkNumber,
//									existingItem.getName());
//							return new JobworkItemNotFoundException("JobworkItem not found with jobwork number "
//									+ jobworkNumber + " and item name " + existingItem.getName());
//						});
//			}
//
//			JobworkReceiptItem jobworkReceiptItem = new JobworkReceiptItem();
//			jobworkReceiptItem.setItem(existingItem);
//			jobworkReceiptItem.setAcceptedQuantity(jobworkReceiptItemDTO.getPurchasedQuantity());
//			jobworkReceiptItem.setSalesPrice(jobworkReceiptItemDTO.getPurchaseCost());
//			jobworkReceiptItem.setJobworkReceipt(createdJobworkReceipt);
//			jobworkReceiptItem.setAcceptedQuantity(jobworkReceiptItemDTO.getReturnedQuantity());
//			jobworkReceiptItem.setWagePerItem(jobworkReceiptItemDTO.getWage());
//
//			// create batch items if cutting - check if this batch item already there
//
//			if (jobwork.getJobworkType() == JobworkType.CUTTING) {
//				BatchItem existingBatchItem = batchItemRepository.findByBatchIdAndItem(batch.getId(), existingItem)
//						.orElse(null);
//				if (existingBatchItem == null) {
//					BatchItem batchItem = new BatchItem();
//					batchItem.setBatch(batch);
//					batchItem.setItem(existingItem);
//					batchItem.setQuantity(jobworkReceiptItemDTO.getReturnedQuantity());
//					batchItemRepository.save(batchItem);
//				} else {
//					existingBatchItem
//							.setQuantity(existingBatchItem.getQuantity() + jobworkReceiptItemDTO.getReturnedQuantity());
//				}
//			}
//
//			long totalDamagedQuantity = 0;
//			for (DamageDTO damageDTO : jobworkReceiptItemDTO.getDamages()) {
//				Damage damage = new Damage();
//				damage.setQuantity(damageDTO.getQuantity());
//				damage.setDamageType(DamageType.fromString(damageDTO.getType()));
//				damage.setJobworkItem(jobworkItem);
//				damage.setJobworkReceipt(createdJobworkReceipt);
//				damageRepository.save(damage);
//
//				if (DamageType.fromString(damageDTO.getType()) == DamageType.REPAIRABLE) {
//					itemCount += damageDTO.getQuantity() == null ? 0 : damageDTO.getQuantity();
//				}
//
//				totalDamagedQuantity += damageDTO.getQuantity();
//			}
//			// TODO to consider the quantity from received receipts
//			long totalQuantityForItem = jobworkReceiptItemDTO.getPurchasedQuantity()
//					+ jobworkReceiptItemDTO.getReturnedQuantity() + totalDamagedQuantity;
//			totalReceivedQuantity += totalQuantityForItem;
//			totalQuantityForItem += jobworkReceiptRepository.findReturnedUnits(jobwork.getId());
//			
//			System.out.println(totalQuantityForItem + " " + jobworkItem.getQuantity());
//			if (jobworkItem.getQuantity() == totalQuantityForItem) {
////				batch.setAvailableQuantity(itemCount);
//				jobworkItem.setJobworkItemStatus(JobworkItemStatus.COMPLETED);
////				batch.setBatchStatus(BatchStatus.COMPLETED);
////				jobwork.setJobworkStatus(JobworkStatus.COMPLETED);
////				batchRepository.save(batch);
////				jobworkRepository.save(jobwork);
//			}
////
//			jobworkReceiptItem.setDamagedQuantity(totalDamagedQuantity);
//			receiptItemRepository.save(jobworkReceiptItem);
//
//		}
//		
//		System.out.println("hairs" + totalQuantityForJobwork + totalReceivedQuantity);
//		batch.setAvailableQuantity(itemCount);
//		if (totalQuantityForJobwork == totalReceivedQuantity) {
//			batch.setBatchStatus(BatchStatus.COMPLETED);
//			jobwork.setJobworkStatus(JobworkStatus.COMPLETED);
//			batchRepository.save(batch);
//			jobworkRepository.save(jobwork);
//		}
//
//
//		// increase quantity for batch
//
//		// TODO to make the batch CLOSED if all jobworks quantity are done for this
//		// batch
//
//	}

}
