package com.lakshmigarments.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobworkReceiptDTO {
	
	private String batchSerialCode;
	private String jobworkNumber;
	private Long receivedById;
	private List<JobworkReceiptItemDTO> jobworkReceiptItems;

}
