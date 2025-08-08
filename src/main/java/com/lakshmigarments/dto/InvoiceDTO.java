package com.lakshmigarments.dto;

import java.sql.Date;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class InvoiceDTO {

	private Long id;
	
	private String invoiceNumber;
	
	private Date invoiceDate;
	
	private Date receivedDate;
	
	private String supplierName;
	
	private String transportName;
	
	private Boolean isTransportPaid;
	
	private Double transportCost;

}
