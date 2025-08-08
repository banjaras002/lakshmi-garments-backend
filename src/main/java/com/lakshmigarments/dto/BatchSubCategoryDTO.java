package com.lakshmigarments.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchSubCategoryDTO {
	
	private Long id;

	private String subCategory;
	
	private Integer quantity;
}
