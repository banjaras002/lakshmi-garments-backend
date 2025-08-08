package com.lakshmigarments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateSupplierDTO {

	@NotBlank(message = "Supplier name is mandatory")
	@Size(max = 200)
	private String name;
	
	@NotBlank(message = "Supplier location is mandatory")
	@Size(max = 400)
	private String location;
}
