package com.lakshmigarments.dto;

import java.sql.Timestamp;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchDTO {
	
	private Long id;

	private String category;
	
	private String serialCode;
	
	private Timestamp createdAt;
	
	private String batchStatus;
	
	private Boolean isUrgent;
	
	private String remarks;
	
	private List<BatchSubCategoryDTO> subCategories;
}
