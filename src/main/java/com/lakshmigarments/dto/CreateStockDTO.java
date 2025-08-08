package com.lakshmigarments.dto;

import java.sql.Date;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateStockDTO {
	
	@NotBlank(message = "Invoice number is mandatory")
	@Size(max = 100)
	private String invoiceNumber;
	
	@PastOrPresent
	private Date invoiceDate;
	
	@PastOrPresent
	private Date shipmentReceivedDate;
	
	@NotNull(message = "Supplier ID is mandatory")
	@Positive(message = "Supplier ID should be positive")
	private Long supplierID;
	
	@NotNull(message = "Transport ID is mandatory")
	@Positive(message = "Transport ID should be positive")
	private Long transportID;
	
	private Double transportCost;
	
	private Boolean isTransportPaid;
	
	private Long createdById;
	
	@NotNull
	private List<CreateLorryReceiptDTO> lorryReceipts;
}
