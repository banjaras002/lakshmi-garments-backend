package com.lakshmigarments.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lakshmigarments.dto.BatchDTO;
import com.lakshmigarments.model.Batch;
import com.lakshmigarments.service.BatchService;

@RestController
@RequestMapping("/batches")
@CrossOrigin(origins = "*")
public class BatchController {
	
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BatchController.class);
	private BatchService batchService;
	
	public BatchController(BatchService batchService) {
		this.batchService = batchService;
	}

	@PostMapping
	public ResponseEntity<BatchDTO> createBatch(@RequestBody BatchDTO batchDTO) {
		LOGGER.info("Create a new batch");
		BatchDTO createdBatchDTO = batchService.createBatch(batchDTO);
		return new  ResponseEntity<>(createdBatchDTO, HttpStatus.CREATED);
	}
	
	@GetMapping
	public ResponseEntity<List<BatchDTO>> getBatches(@RequestParam(required = false) String search) {
		return new ResponseEntity<>(batchService.getBatches(search), HttpStatus.OK);
	}
}
