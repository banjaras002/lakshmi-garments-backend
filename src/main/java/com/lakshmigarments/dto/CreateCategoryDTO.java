package com.lakshmigarments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateCategoryDTO {

	@NotBlank(message = "Category name is mandatory")
	@Size(max = 200)
	private String name;
	
	@Size(max = 10)
	private String code;
}
