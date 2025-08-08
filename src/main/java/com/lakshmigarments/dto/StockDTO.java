package com.lakshmigarments.dto;

import java.sql.Date;
import java.util.List;

import com.lakshmigarments.model.LorryReceipt;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StockDTO {

	private Long invoiceId;
	private String invoiceNumber;
	private Date invoiceDate;
	private Date shipmentReceiveDate;
	private String supplierName;
	private List<LorryReceipt> lorryReceipts;
}
