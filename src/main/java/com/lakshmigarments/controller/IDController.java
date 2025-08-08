package com.lakshmigarments.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lakshmigarments.dto.CompleteInvoiceDTO;
import com.lakshmigarments.service.IDService;
import com.lakshmigarments.service.InvoiceService;

@RestController
@RequestMapping("/id")
@CrossOrigin(origins = "*")
public class IDController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(IDController.class);
	private final IDService idService;
	
	public IDController(IDService idService) {
		this.idService = idService;
	}

	@GetMapping("/lr")
	public Long getNextLRID() {
		LOGGER.info("Retrieve next LR number");
		return idService.getNextLRID();
	}
	
	@GetMapping("/batch")
	public String getBatchSerialCode(@RequestParam(required = true) String categoryName) {
		LOGGER.info("Retrieve batch serial code");
		return idService.getSerialCode(categoryName);
	}
	
}
