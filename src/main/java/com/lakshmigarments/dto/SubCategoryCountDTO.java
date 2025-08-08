package com.lakshmigarments.dto;

import com.lakshmigarments.model.Category;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubCategoryCountDTO {

	private String subCategoryName;
	private Long count;
}
