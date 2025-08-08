package com.lakshmigarments.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lakshmigarments.dto.CreateStockDTO;
import com.lakshmigarments.dto.StockDTO;
import com.lakshmigarments.service.StockService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/shipments")
@CrossOrigin(origins = "*")
public class StockController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(StockController.class);
	private StockService stockService;
	
	public StockController(StockService stockService) {
		this.stockService = stockService;
	}

	@PostMapping
	public ResponseEntity<StockDTO> createStock(@RequestBody @Validated CreateStockDTO createStockDTO) {
		LOGGER.info("Create a new stock");
		StockDTO stockDTO = stockService.createStock(createStockDTO);
		return new  ResponseEntity<>(stockDTO, HttpStatus.CREATED);
	}
	
//	@GetMapping
//	public CsrfToken getMethodName(HttpServletRequest httpServletRequest) {
//		return (CsrfToken) httpServletRequest.getAttribute("_csrf");
//	}
	
	
}
