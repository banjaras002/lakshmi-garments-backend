package com.lakshmigarments.dto;

import com.lakshmigarments.model.JobworkType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobworkRequestDTO {
	
	private Long employeeId;
	private String batchSerialCode;
	private Long itemId;
	private Long quantity;
	private JobworkType jobworkType;
	private String jobworkNumber;
	private Long assignedBy;

}
